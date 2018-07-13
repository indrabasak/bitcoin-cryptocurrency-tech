import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1220"})
public class MaxFeeTxHandler2 {
    private UTXOPool utxoPool;


    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the
     * UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler2(UTXOPool utxoPool) {
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

        Map<Integer, Set<Integer>> conflictMap = new HashMap<>();
        for (int i = 0; i < possibleTxs.length; i++) {
            TransactionWithFees txf = txPosToFeesMap.get(i);

            if (txf != null) {
                Set<Integer> conflictSet = new HashSet<>();

                for (int j = 0; j < possibleTxs.length; j++) {
                    if (i != j) {
                        TransactionWithFees txfj = txPosToFeesMap.get(j);
                        if (txfj != null) {
                            for (UTXO ut : txf.inputUtxos) {
                                if (txfj.inputUtxos.contains(ut)) {
                                    conflictSet.add(j);
                                    break;
                                }
                            }
                        }
                    }
                }
                conflictMap.put(i, conflictSet);
            }
        }

        conflictMap.entrySet().forEach(e -> {
            System.out.print(e.getKey() + ": [");
            for (Integer i : e.getValue()) {
                System.out.print(i + " ");
            }
            System.out.print("]\n");
        });
        System.out.println("tx map size: " + txPosToFeesMap.size());
        List<TransactionWithFees> possible = new CopyOnWriteArrayList<>();
        double totalFees = 0;
        for (int i = 0; i < possibleTxs.length; i++) {
            TransactionWithFees txfi = txPosToFeesMap.get(i);

            if (txfi != null) {
                double currentTotalFees = txfi.fees;
                List<TransactionWithFees> currentPossible =
                        new CopyOnWriteArrayList<>();
                currentPossible.add(txfi);
                Set<Integer> iConflictSet = conflictMap.get(i);
                UTXOPool tempPool = new UTXOPool(utxoPool);
                for (int j = 0; j < possibleTxs.length; j++) {
                    if (i != j && txPosToFeesMap.containsKey(j)) {
                        TransactionWithFees txfj = txPosToFeesMap.get(j);

                        if (!iConflictSet.contains(j)) {
                            boolean add = true;
                            Set<Integer> jConflictSet = conflictMap.get(j);
                            for (Integer ic : iConflictSet) {
                                if (jConflictSet.contains(ic)) {
                                    add = false;
                                    break;
                                }
                            }

                            if (add) {
                                for (Integer jc : jConflictSet) {
                                    if (!iConflictSet.contains(jc)) {
                                        TransactionWithFees txfk =
                                                txPosToFeesMap.get(jc);
                                        if (txfk.fees > txfj.fees) {
                                            add = false;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (add) {
                                currentTotalFees += txfj.fees;
                                currentPossible.add(txfj);
                                for (int x = 0; x < txfj.tx.numOutputs(); x++) {
                                    UTXO ut = new UTXO(txfj.tx.getHash(), x);
                                    tempPool.addUTXO(ut, txfj.tx.getOutput(x));
                                }
                                ;
                            }
                        }
                    }
                }

                for (TransactionWithFees txfp : currentPossible) {
                    if (!isValidTx(txfp.tx, tempPool)) {
                        currentTotalFees -= txfp.fees;
                        currentPossible.remove(txfp);
                    }
                }

                if (currentTotalFees > totalFees) {
                    System.out.println(
                            "currentPossible size: " + currentPossible.size());

                    for (TransactionWithFees f : currentPossible) {
                        System.out.print(f.index + " ");
                    }

                    System.out.println();
                    totalFees = currentTotalFees;
                    possible = currentPossible;
                }
            }
        }

        System.out.println("possible tx size: " + possible.size());
        List<TransactionWithFees> accepted = new CopyOnWriteArrayList<>();
        List<TransactionWithFees> rejected = new CopyOnWriteArrayList<>();
        for (TransactionWithFees txf : possible) {
            if (isValidTx(txf.tx)) {
                updatePool(txf);
                accepted.add(txf);
                System.out.println("&&&&&&&&&&& valid " + txf.index);
            } else {
                System.out.println("&&&&&&&&&&& not valid " + txf.index);
                rejected.add(txf);
            }
        }

        boolean loop = true;
        while (loop) {
            loop = processRejectedTxs(accepted, rejected);
        }

        double acceptedTotalFees = 0;
        for (TransactionWithFees txf : accepted) {
            acceptedTotalFees += txf.fees;
        }

        System.out.println("totalFees: " + acceptedTotalFees);
        System.out.println("accepted size: " + accepted.size());

        return accepted.stream().map(
                txf -> txf.tx).toArray((Transaction[]::new));
    }

    private boolean processRejectedTxs(List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected) {
        boolean success = false;

        for (TransactionWithFees tx : rejected) {
            success = success || checkAcceptedTxs(tx, accepted, rejected);
        }

        return success;
    }

    private void updatePool(TransactionWithFees transaction) {
        Map<UTXO, Transaction.Output> removedUTXOs = new HashMap<>();
        transaction.tx.getInputs().forEach(in -> {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = utxoPool.getTxOutput(ut);
            removedUTXOs.put(ut, out);
            utxoPool.removeUTXO(ut);
        });

        for (int i = 0; i < transaction.tx.getOutputs().size(); i++) {
            UTXO ut = new UTXO(transaction.tx.getHash(), i);
            utxoPool.addUTXO(ut, transaction.tx.getOutput(i));
        }

        transaction.removedUTXOs = removedUTXOs;
    }

    private boolean checkAcceptedTxs(TransactionWithFees transaction,
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected) {
        System.out.println("1----------- checkAcceptedTxs()");
        Set<TransactionWithFees> sharedTxs = new HashSet<>();

        int count = 0;
        for (Transaction.Input in : transaction.tx.getInputs()) {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            if (!utxoPool.contains(ut)) {
                for (TransactionWithFees txf : accepted) {
                    if (txf.removedUTXOs.containsKey(ut)) {
                        count++;
                        sharedTxs.add(txf);
                    }
                }
            } else {
                count++;
            }
        }

        if (count == transaction.tx.numInputs()) {
            System.out.println("2----------- checkAcceptedTxs()");
            return proceesSharedTxs(transaction, accepted, rejected, sharedTxs);
        }

        System.out.println("3----------- checkAcceptedTxs()");
        return false;
    }

    private boolean proceesSharedTxs(TransactionWithFees transaction,
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            Set<TransactionWithFees> sharedTxs) {
        System.out.println("1 ---- processedSharedTxs");
        UTXOPool pool = new UTXOPool(utxoPool);
        double oldFees = 0;
        for (TransactionWithFees at : sharedTxs) {

            at.removedUTXOs.entrySet().forEach(e -> {
                pool.addUTXO(e.getKey(), e.getValue());
            });

            for (int i = 0; i < at.tx.getOutputs().size(); i++) {
                pool.removeUTXO(new UTXO(at.tx.getHash(), i));
            }

            oldFees += at.fees;
        }

        double fees = transaction.fees;
        System.out.println("fees: " + fees + " old fees " + oldFees);
        if (isValidTx(transaction.tx, pool) && (fees > oldFees)) {
            sharedTxs.forEach(
                    t -> {
                        accepted.remove(t);
                        rejected.add(t);
                    });
            utxoPool = new UTXOPool(pool);
            updatePool(transaction);
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
}
