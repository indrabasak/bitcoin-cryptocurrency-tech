import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1220"})
public class MaxFeeTxHandler3 {
    private UTXOPool utxoPool;


    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the
     * UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler3(UTXOPool utxoPool) {
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

        //System.out.println("**** inputSum: " + inSum + " outputSum: " + outSum);
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

        //System.out.println("inputSum: " + inSum + " outputSum: " + outSum);

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

        List<TransactionWithFeesCollection> possibleCollections
                = new CopyOnWriteArrayList<>();

        for (int i = 0; i < possibleTxs.length; i++) {
            TransactionWithFees txfi = txPosToFeesMap.get(i);

            if (txfi != null) {
                double currentTotalFees = txfi.fees;
                List<TransactionWithFees> currentPossible =
                        new CopyOnWriteArrayList<>();
                currentPossible.add(txfi);
                for (int j = 0; j < possibleTxs.length; j++) {
                    if (i != j && txPosToFeesMap.containsKey(j)) {
                        TransactionWithFees txfj = txPosToFeesMap.get(j);
                        boolean add = true;
                        for (UTXO ut : txfi.inputUtxos) {
                            if (txfj.inputUtxos.contains(ut)) {
                                add = false;
                                break;
                            }
                        }

                        if (add) {
                            currentTotalFees += txfj.fees;
                            currentPossible.add(txfj);
                        }
                    }
                }

                //if (currentTotalFees > 0) {
                possibleCollections.add(
                        new TransactionWithFeesCollection(currentPossible,
                                currentTotalFees));
                //}
            }
        }

        for (TransactionWithFeesCollection coll : possibleCollections) {
            System.out.print("[");
            for (TransactionWithFees txf : coll.txs) {
                System.out.print(txf.index + " ");
            }
            System.out.print("]\n");
        }

        SortedSet<TransactionWithFeesCollection> sortedCollectionByFees =
                new TreeSet<>(
                        (tx1, tx2) -> Double.valueOf(tx2.totalFees).compareTo(
                                tx1.totalFees));

        System.out.println(
                "possibleCollections.size: " + possibleCollections.size());
        for (TransactionWithFeesCollection coll : possibleCollections) {
            List<TransactionWithFees> accepted = new CopyOnWriteArrayList<>();
            List<TransactionWithFees> rejected = new CopyOnWriteArrayList<>();

            UTXOPool tempPool = new UTXOPool(utxoPool);
            for (TransactionWithFees txf : coll.txs) {
                if (isValidTx(txf.tx, tempPool)) {
                    updatePool(txf, tempPool);
                    accepted.add(txf);
                    System.out.println("### accepted: " + txf.index);
                } else {
                    rejected.add(txf);
                    System.out.println("### rejected: " + txf.index);
                }
            }

            boolean loop = true;
            while (loop) {
                loop = processRejectedTxs(accepted, rejected, tempPool);
            }

            if (accepted.size() > 0) {
                double totalFees =
                        accepted.stream().mapToDouble(t -> t.fees).sum();
                sortedCollectionByFees.add(
                        new TransactionWithFeesCollection(accepted, totalFees,
                                tempPool));
            }
        }

        List<TransactionWithFees> accepted = new ArrayList<>();
        if (sortedCollectionByFees.size() > 0) {
            for (TransactionWithFeesCollection coll : sortedCollectionByFees) {
                System.out.print("*** [");
                for (TransactionWithFees txf : coll.txs) {
                    System.out.print(txf.index + " ");
                }
                System.out.print("]\n");
            }

            TransactionWithFeesCollection txfc = sortedCollectionByFees.first();
            accepted = txfc.txs;
            double acceptedTotalFees = txfc.totalFees;
            utxoPool = new UTXOPool(txfc.pool);

            System.out.println("totalFees: " + acceptedTotalFees);
            System.out.println("accepted size: " + accepted.size());
        }


        return accepted.stream().map(
                txf -> txf.tx).toArray((Transaction[]::new));
    }

    private boolean processRejectedTxs(List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected, UTXOPool pool) {
        boolean success = false;


        for (TransactionWithFees tx : rejected) {
            System.out.println("processing rejected: " + tx.index);
            success = success || checkAcceptedTxs(tx, accepted, rejected, pool);
        }

        return success;
    }

    private void updatePool(TransactionWithFees transaction, UTXOPool pool) {
        Map<UTXO, Transaction.Output> removedUTXOs = new HashMap<>();
        transaction.tx.getInputs().forEach(in -> {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = pool.getTxOutput(ut);
            removedUTXOs.put(ut, out);
            pool.removeUTXO(ut);
        });

        for (int i = 0; i < transaction.tx.getOutputs().size(); i++) {
            UTXO ut = new UTXO(transaction.tx.getHash(), i);
            pool.addUTXO(ut, transaction.tx.getOutput(i));
        }

        transaction.removedUTXOs = removedUTXOs;
    }

    private boolean checkAcceptedTxs(TransactionWithFees transaction,
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            UTXOPool pool) {
        System.out.println("1----------- checkAcceptedTxs()");
        Set<TransactionWithFees> shared = new HashSet<>();

        int count = 0;
        for (Transaction.Input in : transaction.tx.getInputs()) {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            if (!pool.contains(ut)) {
                for (TransactionWithFees txf : accepted) {
                    if (txf.removedUTXOs.containsKey(ut)) {
                        count++;
                        shared.add(txf);
                    }
                }
            } else {
                count++;
            }
        }

        System.out.print("shared: [");
        for (TransactionWithFees x : shared) {
            System.out.print(x.index + " ");
        }
        System.out.print("]\n");

        if (count == transaction.tx.numInputs() &&
                !checkTransitiveDependency(accepted, shared, pool)) {
            System.out.println("2----------- checkAcceptedTxs()");
            return proceesSharedTxs(transaction, accepted, rejected, pool,
                    shared);
        }

        System.out.println("3----------- checkAcceptedTxs()");
        return false;
    }

    private boolean checkTransitiveDependency(
            List<TransactionWithFees> accepted,
            Set<TransactionWithFees> shared,
            UTXOPool pool) {
        System.out.println("1 ---- checkForDependencyTree");
        int initialSharedTxsSize = shared.size();

        Set<TransactionWithFees> tempSet = new HashSet<>();
        for (TransactionWithFees at : shared) {
            int foundCount = 0;
            for (int i = 0; i < at.tx.numOutputs(); i++) {
                UTXO ut = new UTXO(at.tx.getHash(), i);

                if (pool.contains(ut)) {
                    foundCount++;
                } else {
                    for (TransactionWithFees entry : accepted) {
                        if (entry.removedUTXOs.containsKey(ut)) {
                            foundCount++;
                            tempSet.add(entry);
                            break;
                        }
                    }
                }
            }

            if (foundCount != at.tx.numOutputs()) {
                System.out.println("2 ---- checkForDependencyTree");
                return true;
            }
        }

        shared.addAll(tempSet);
        if (shared.size() > initialSharedTxsSize) {
            System.out.println("3 ---- checkForDependencyTree");
            return checkTransitiveDependency(accepted, shared, pool);
        }

        System.out.println("4 ---- checkForDependencyTree");
        return false;
    }

    private boolean proceesSharedTxs(TransactionWithFees transaction,
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            UTXOPool pool,
            Set<TransactionWithFees> shared) {
        System.out.println("1 ---- processedSharedTxs");
        UTXOPool tempPool = new UTXOPool(pool);
        double oldFees = 0;
        for (TransactionWithFees at : shared) {

            at.removedUTXOs.entrySet().forEach(e -> {
                tempPool.addUTXO(e.getKey(), e.getValue());
            });

            for (int i = 0; i < at.tx.getOutputs().size(); i++) {
                tempPool.removeUTXO(new UTXO(at.tx.getHash(), i));
            }

            oldFees += at.fees;
        }

        double fees = transaction.fees;
        System.out.println("fees: " + fees + " old fees " + oldFees);
        if (isValidTx(transaction.tx, tempPool) && (fees > oldFees)) {
            shared.forEach(
                    t -> {
                        System.out.println("$$$$$ rejected: " + t.index);
                        accepted.remove(t);
                        rejected.add(t);
                    });
            pool.getAllUTXO().clear();
            for (UTXO ut : tempPool.getAllUTXO()) {
                pool.addUTXO(ut, tempPool.getTxOutput(ut));
            }

            updatePool(transaction, pool);

            System.out.println("$$$$$ accepted: " + transaction.index);
            accepted.add(transaction);

            return true;
        }

        return false;
    }

    private class TransactionWithFees {
        public int index;
        public Transaction tx;

        public double fees;

        public Set<UTXO> inputUtxos;

        public Map<UTXO, Transaction.Output> removedUTXOs;

        public TransactionWithFees(int index, Transaction tx, double fees,
                Set<UTXO> inputUtxos) {
            this.index = index;
            this.tx = tx;
            this.fees = fees;
            this.inputUtxos = inputUtxos;
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
    }
}
