import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1220"})
public class MaxFeeTxHandler6 {
    private UTXOPool utxoPool;


    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the
     * UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler6(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public boolean isValidTx(Transaction tx) {
        return isValidTx(tx, utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the
     * sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx, UTXOPool pool) {
        UTXOPool uniqueUtxos = new UTXOPool();
        double inSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = pool.getTxOutput(utxo);

            // CASE 1: all outputs claimed by {@code tx} are in the
            // current UTXO pool
            if (!pool.contains(utxo)) {
                return false;
            }

            // CASE 2: the signatures on each input of {@code tx} are valid
            if (!Crypto.verifySignature(out.address, tx.getRawDataToSign(i),
                    in.signature)) {
                return false;
            }

            // CASE 3: no UTXO is claimed multiple times by {@code tx}
            if (uniqueUtxos.contains(utxo)) {
                return false;
            }

            uniqueUtxos.addUTXO(utxo, out);
            inSum += out.value;
        }

        double outSum = 0;
        for (Transaction.Output out : tx.getOutputs()) {
            // CASE 4: all of {@code tx}s output values are non-negative
            if (out.value < 0) {
                return false;
            }

            outSum += out.value;
        }

        // CASE 5: the sum of {@code tx}s input values is greater than or
        // equal to the sum of its output values
        return inSum >= outSum;
    }

    /**
     * Calculates transaction fees.
     *
     * @param tx
     * @param pool
     * @return
     */
    private double calculateFees(Transaction tx, UTXOPool pool) {
        double inSum = tx.getInputs().stream()
                .mapToDouble(i -> {
                    UTXO ut = new UTXO(i.prevTxHash, i.outputIndex);
                    //if (pool.getTxOutput(ut) != null && isValidTx(tx)) {
                    if (pool.getTxOutput(ut) != null) {
                        return pool.getTxOutput(
                                new UTXO(i.prevTxHash, i.outputIndex)).value;
                    }

                    return 0;
                }).sum();

        double outSum = tx.getOutputs().stream().filter(
                Objects::nonNull).mapToDouble(o -> o.value).sum();

        return inSum - outSum;
    }

    /**
     * Finds a set of transactions with maximum total transaction fees -- i.e.
     * maximize the sum over all transactions in the set of (sum of input values
     * - sum of output values)).
     *
     * @param possibleTxs
     * @return an array of valid transactions sorted in descending order
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        UTXOPool pool = new UTXOPool(utxoPool);

        for (Transaction tx : possibleTxs) {
            for (int i = 0; i < tx.numOutputs(); i++) {
                UTXO ut = new UTXO(tx.getHash(), i);
                pool.addUTXO(ut, tx.getOutput(i));
            }
        }

        Map<Integer, TransactionWithFees> txPosToFeesMap = new HashMap<>();
        for (int i = 0; i < possibleTxs.length; i++) {
            Transaction tx = possibleTxs[i];
            if (isValidTx(tx, pool)) {
                double fees = calculateFees(tx, pool);
                Set<UTXO> inputUtxos = new HashSet<>();
                for (Transaction.Input in : tx.getInputs()) {
                    inputUtxos.add(new UTXO(in.prevTxHash, in.outputIndex));
                }
                txPosToFeesMap.put(i,
                        new TransactionWithFees(i, tx, fees, inputUtxos));
            }
        }

        List<TransactionWithFees> accepted = new CopyOnWriteArrayList<>();
        List<TransactionWithFees> rejected = new CopyOnWriteArrayList<>();
        UTXOPool tempPool = new UTXOPool(utxoPool);
        txPosToFeesMap.entrySet().forEach(e -> {
            TransactionWithFees txf = e.getValue();
            if (isValidTx(txf.tx, tempPool)) {
                updatePool(txf, tempPool);
                accepted.add(txf);
            } else {
                rejected.add(txf);
            }
        });

        boolean loop = true;
        while (loop) {
            loop = processRejected(accepted, rejected, tempPool);
        }

        if (accepted.size() > 0) {
            double totalFees =
                    accepted.stream().mapToDouble(t -> t.fees).sum();
            System.out.println("totalFees: " + totalFees);
            System.out.print("accepted: [");
            accepted.forEach(t -> System.out.print(t.index + " "));
            System.out.print("]\n");
        }

        utxoPool = new UTXOPool(tempPool);

        return accepted.stream().map(
                txf -> txf.tx).toArray((Transaction[]::new));
    }

    private void updatePool(TransactionWithFees transaction, UTXOPool pool) {
        Map<UTXO, Transaction.Output> spentUTXOs = new HashMap<>();

        transaction.tx.getInputs().forEach(in -> {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = pool.getTxOutput(ut);
            spentUTXOs.put(ut, out);
            pool.removeUTXO(ut);
        });

        for (int i = 0; i < transaction.tx.getOutputs().size(); i++) {
            UTXO ut = new UTXO(transaction.tx.getHash(), i);
            pool.addUTXO(ut, transaction.tx.getOutput(i));
        }

        transaction.spentUTXOs = spentUTXOs;
    }

    private boolean processRejected(List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected, UTXOPool pool) {
        boolean success = false;

        for (TransactionWithFees tx : rejected) {
            success = success || removeAcceptedIfNeeded(tx, accepted,
                    rejected, pool);
        }

        return success;
    }

    private boolean removeAcceptedIfNeeded(
            TransactionWithFees transaction,
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            UTXOPool pool) {
        Set<TransactionWithFees> sharedSet = new HashSet<>();
        List<TransactionWithFees> shared = new CopyOnWriteArrayList<>();

        System.out.println("1 --- check: accepted");
        for (TransactionWithFees t : accepted) {
            System.out.print(t.index + " ");
        }
        System.out.println("Trying to add " + transaction.index);

        printPool(pool);

        int count = 0;
        for (Transaction.Input in : transaction.tx.getInputs()) {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            if (!pool.contains(ut)) {
                for (TransactionWithFees txf : accepted) {
                    if (txf.spentUTXOs.containsKey(ut)) {
                        System.out.println("found: 1");
                        count++;
                        if (!sharedSet.contains(txf)) {
                            sharedSet.add(txf);
                            shared.add(txf);
                        }
                    }
                }
            } else {
                Transaction.Output out = pool.getTxOutput(ut);
                count++;
                System.out.println("found: 2 - value: " + out.value);
                printUtxo(ut);
            }
        }

        System.out.println("shared [");
        for (TransactionWithFees t : sharedSet) {
            System.out.print(t.index + " ");
        }
        System.out.print("]\n");

        System.out.println(
                "count: " + count + " numInputs: " + transaction.tx.numInputs());
        if (count == transaction.tx.numInputs()) {
            if (sharedSet.isEmpty()) {
                return proceesSharedAndAddIfNeeded(transaction, accepted,
                        rejected, pool,
                        shared);
            } else if (!checkTransitiveDependency(accepted, shared, pool)) {
                return proceesSharedAndAddIfNeeded(transaction, accepted,
                        rejected, pool,
                        shared);
            }
        }

        return false;
    }

    private boolean checkTransitiveDependency(
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> shared,
            UTXOPool pool) {
        System.out.println("1 --------- checkTransitiveDependency");
        int initialSharedTxsSize = shared.size();

        Set<TransactionWithFees> tempShared = new HashSet<>(shared);

        for (TransactionWithFees at : shared) {
            int foundCount = 0;
            for (int i = 0; i < at.tx.numOutputs(); i++) {
                UTXO ut = new UTXO(at.tx.getHash(), i);

                if (pool.contains(ut)) {
                    foundCount++;
                } else {
                    for (TransactionWithFees entry : accepted) {
                        if (entry.spentUTXOs.containsKey(ut)) {
                            foundCount++;
                            if (!tempShared.contains(entry)) {
                                shared.add(entry);
                            }

                            break;
                        }
                    }
                }
            }

            if (foundCount != at.tx.numOutputs()) {
                return true;
            }
        }

        if (shared.size() > initialSharedTxsSize) {
            System.out.println("3 --------- checkTransitiveDependency");
            return checkTransitiveDependency(accepted, shared, pool);
        }

        return false;
    }

    private boolean proceesSharedAndAddIfNeeded(
            TransactionWithFees transaction,
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            UTXOPool pool,
            List<TransactionWithFees> shared) {
        UTXOPool tempPool = new UTXOPool(pool);

        int before = pool.getAllUTXO().size();
        System.out.println("1 --- proceesSharedAndAddIfNeeded");
        for (TransactionWithFees t : accepted) {
            System.out.print(t.index + " ");
        }
        System.out.println("Trying to add " + transaction.index);

        System.out.print("shared [");
        for (TransactionWithFees t : shared) {
            System.out.print(t.index + " ");
        }
        System.out.print("]\n");

        double oldFees = 0;
        for (int t = shared.size() - 1; t >= 0; t--) {
            TransactionWithFees at = shared.get(t);

            at.spentUTXOs.entrySet().forEach(e ->
                    tempPool.addUTXO(e.getKey(), e.getValue()));

            for (int i = 0; i < at.tx.getOutputs().size(); i++) {
                tempPool.removeUTXO(new UTXO(at.tx.getHash(), i));
            }

            oldFees += at.fees;
        }

        int after = pool.getAllUTXO().size();
        System.out.println("before: " + before + " after: " + after);
        System.out.println("2 --- proceesSharedAndAddIfNeeded");
        System.out.println(
                "old fees: " + oldFees + " new fees: " + transaction.fees);
        double fees = calculateFees(transaction.tx, tempPool);
        if (isValidTx(transaction.tx, tempPool) && (shared.isEmpty() ||
                (fees > oldFees))) {
            for (int t = shared.size() - 1; t >= 0; t--) {
                TransactionWithFees at = shared.get(t);

                at.spentUTXOs.entrySet().forEach(e ->
                        pool.addUTXO(e.getKey(), e.getValue()));

                for (int i = 0; i < at.tx.getOutputs().size(); i++) {
                    pool.removeUTXO(new UTXO(at.tx.getHash(), i));
                }

                accepted.remove(at);
                rejected.add(at);
            }

            //printPool(pool);

            updatePool(transaction, pool);
            accepted.add(transaction);
            rejected.remove(transaction);

            return true;
        }

        return false;
    }

    private String getHash(byte[] input) {
        return Base64.getUrlEncoder().encodeToString(input);
    }

    private void printUtxo(UTXO utxo) {
        System.out.println("Tx: " + getHash(
                utxo.getTxHash()) + " index: " + utxo.getIndex());
    }

    private void printPool(UTXOPool pool) {
        for (UTXO utxo : pool.getAllUTXO()) {
            printUtxo(utxo);
        }
    }

    private class TransactionWithFees {
        public int index;
        public Transaction tx;

        public double fees;

        public Set<UTXO> inputUtxos;

        public Map<UTXO, Transaction.Output> spentUTXOs;

        public TransactionWithFees(int index, Transaction tx, double fees,
                Set<UTXO> inputUtxos) {
            this.index = index;
            this.tx = tx;
            this.fees = fees;
            this.inputUtxos = inputUtxos;
        }


        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 17 + index;
            hash = hash * 31 + Arrays.hashCode(tx.getHash());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            TransactionWithFees other = (TransactionWithFees) obj;
            if (index == other.index) {
                return true;
            }

            return false;
        }
    }

    private class TransactionWithFeesCollection {
        public List<TransactionWithFees> txs;

        public double totalFees;

        public UTXOPool pool;

        public TransactionWithFeesCollection(List<TransactionWithFees> txs,
                double totalFees) {
            this(txs, totalFees, null);
        }

        public TransactionWithFeesCollection(List<TransactionWithFees> txs,
                double totalFees, UTXOPool pool) {
            this.txs = txs;
            this.totalFees = totalFees;
            this.pool = pool;
        }

        public String print() {
            StringBuilder bldr = new StringBuilder("[ ");
            for (TransactionWithFees txf : txs) {
                bldr.append(txf.index + " ");
            }
            bldr.append("]");

            return bldr.toString();
        }
    }
}

