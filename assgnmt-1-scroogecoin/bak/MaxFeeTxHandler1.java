import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1220"})
public class MaxFeeTxHandler1 {
    private UTXOPool utxoPool;


    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the
     * UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler1(UTXOPool utxoPool) {
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
        double inputSum = 0;

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
            inputSum += out.value;
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
        return inputSum >= outSum;
    }

    /**
     * Calculates transaction fees.
     *
     * @param tx
     * @param pool
     * @return
     */
    private double calculateFees(Transaction tx, UTXOPool pool) {
        double inputSum = tx.getInputs().stream()
                .mapToDouble(i -> {
                    UTXO ut = new UTXO(i.prevTxHash, i.outputIndex);
                    if (pool.getTxOutput(ut) != null && isValidTx(tx, pool)) {
                        return pool.getTxOutput(
                                new UTXO(i.prevTxHash, i.outputIndex)).value;
                    }

                    return 0;
                }).sum();

        double outputSum = tx.getOutputs().stream().filter(
                Objects::nonNull).mapToDouble(o -> o.value).sum();

        return inputSum - outputSum;
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
        Set<Transaction> sortedTxsByFee = new TreeSet<>((tx1, tx2) ->
                Double.valueOf(calculateFees(tx2, utxoPool)).compareTo(
                        calculateFees(tx1, utxoPool)));
        Collections.addAll(sortedTxsByFee, possibleTxs);

        List<Transaction> rejected = new CopyOnWriteArrayList<>();
        Map<String, TransactionWithSpentUtxo> accepted =
                new ConcurrentHashMap<>();
        for (Transaction tx : sortedTxsByFee) {
            if (isValidTx(tx)) {
                TransactionWithSpentUtxo txu = updatePool(tx);
                accepted.put(getHashString(tx.getHash()), txu);
            } else {
                rejected.add(tx);
            }
        }

        boolean loop = true;
        while (loop) {
            loop = processRejectedTxs(rejected, accepted);
        }

        double totalFees = 0;
        for (Map.Entry<String, TransactionWithSpentUtxo> e : accepted.entrySet()) {
            totalFees += e.getValue().fees;
        }

        System.out.println("totalFees: " + totalFees);

        return accepted.entrySet().stream().map(
                e -> e.getValue().tx).toArray((Transaction[]::new));
    }

    private boolean processRejectedTxs(List<Transaction> rejected,
            Map<String, TransactionWithSpentUtxo> accepted) {
        boolean success = false;

        for (Transaction tx : rejected) {
            success = success || checkAcceptedTxs(tx, accepted, rejected);
        }

        return success;
    }

    private TransactionWithSpentUtxo updatePool(Transaction transaction) {
        double fees = calculateFees(transaction, utxoPool);
        Map<UTXO, Transaction.Output> removedUTXOs = new HashMap<>();
        transaction.getInputs().forEach(in -> {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = utxoPool.getTxOutput(ut);
            removedUTXOs.put(ut, out);
            utxoPool.removeUTXO(ut);
        });

        for (int i = 0; i < transaction.getOutputs().size(); i++) {
            UTXO ut = new UTXO(transaction.getHash(), i);
            utxoPool.addUTXO(ut, transaction.getOutput(i));
        }

        return new TransactionWithSpentUtxo(transaction, fees, removedUTXOs);
    }

    private boolean checkAcceptedTxs(Transaction transaction,
            Map<String, TransactionWithSpentUtxo> accepted,
            List<Transaction> rejected) {
        Set<TransactionWithSpentUtxo> sharedTxs = new HashSet<>();

        int count = 0;
        for (Transaction.Input in : transaction.getInputs()) {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            if (!utxoPool.contains(ut)) {
                for (Map.Entry<String, TransactionWithSpentUtxo> e : accepted.entrySet()) {
                    if (e.getValue().removedUTXOs.containsKey(ut)) {
                        count++;
                        sharedTxs.add(e.getValue());
                    }
                }
            } else {
                count++;
            }
        }

        if (count == transaction.numInputs() && !sharedTxs.isEmpty() &&
                !checkForDependencyTree(accepted, sharedTxs)) {
            return proceesSharedTxs(transaction, accepted, rejected, sharedTxs);
        }

        return false;
    }

    private boolean checkForDependencyTree(
            Map<String, TransactionWithSpentUtxo> accepted,
            Set<TransactionWithSpentUtxo> sharedTxs) {
        System.out.println("1 ---- checkForDependencyTree");
        int initialSharedTxsSize = sharedTxs.size();

        for (TransactionWithSpentUtxo at : sharedTxs) {
            int foundCount = 0;
            for (int i = 0; i < at.tx.numOutputs(); i++) {
                UTXO ut = new UTXO(at.tx.getHash(), i);

                if (utxoPool.contains(ut)) {
                    foundCount++;
                } else {
                    for (Map.Entry<String, TransactionWithSpentUtxo> entry : accepted.entrySet()) {
                        if (entry.getValue().removedUTXOs.containsKey(ut)) {
                            foundCount++;
                            sharedTxs.add(entry.getValue());
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

        if (sharedTxs.size() > initialSharedTxsSize) {
            System.out.println("3 ---- checkForDependencyTree");
            return checkForDependencyTree(accepted, sharedTxs);
        }

        System.out.println("4 ---- checkForDependencyTree");
        return false;
    }

    private boolean proceesSharedTxs(Transaction transaction,
            Map<String, TransactionWithSpentUtxo> accepted,
            List<Transaction> rejected,
            Set<TransactionWithSpentUtxo> sharedTxs) {
        System.out.println("1 ---- processedSharedTxs");
        UTXOPool pool = new UTXOPool(utxoPool);
        double oldFees = 0;
        for (TransactionWithSpentUtxo at : sharedTxs) {

            at.removedUTXOs.entrySet().forEach(e -> {
                pool.addUTXO(e.getKey(), e.getValue());
            });

            for (int i = 0; i < at.tx.getOutputs().size(); i++) {
                pool.removeUTXO(new UTXO(at.tx.getHash(), i));
            }

            oldFees += at.fees;
        }

        double fees = calculateFees(transaction, pool);
        System.out.println("fees: " + fees + " old fees " + oldFees);
        if (isValidTx(transaction, pool) && (fees > oldFees)) {
            sharedTxs.forEach(
                    t -> {
                        accepted.remove(getHashString(t.tx.getHash()));
                        rejected.add(t.tx);
                    });
            utxoPool = new UTXOPool(pool);
            accepted.put(getHashString(transaction.getHash()), updatePool(transaction));

            return true;
        }

        return false;
    }

    private String getHashString(byte[] hash) {
        return Base64.getUrlEncoder().encodeToString(hash);
    }

    private class TransactionWithSpentUtxo {
        public Transaction tx;

        public double fees;

        public Map<UTXO, Transaction.Output> removedUTXOs;

        public TransactionWithSpentUtxo(Transaction tx, double fees,
                Map<UTXO, Transaction.Output> removedUTXOs) {
            this.tx = tx;
            this.fees = fees;
            this.removedUTXOs = removedUTXOs;
        }
    }
}
