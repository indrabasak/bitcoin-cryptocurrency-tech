import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@code MaxFeeTxHandler} finds a set of transactions with the maximum total
 * transaction fees, i.e., maximize the sum over all transactions in the set.
 * <p/>
 *
 * @author Indra Basak
 * @since 05/14/18
 */
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1220",
        "squid:UnusedPrivateMethod", "squid:S3776"})
public class MaxFeeTxHandler {

    private static final boolean LOG_ON = false;

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the
     * UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
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
     * Calculates transaction fees of a transaction.
     *
     * @param tx   the transaction whose fees being calculated
     * @param pool a pool of unspent transaction outputs
     * @return transaction fees
     */
    private double calculateFees(Transaction tx, UTXOPool pool) {
        double inSum = tx.getInputs().stream()
                .mapToDouble(i -> {
                    UTXO ut = new UTXO(i.prevTxHash, i.outputIndex);
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
            log("Transactions processed: ");
            if (isValidTx(tx, pool)) {
                double fees = calculateFees(tx, pool);
                TransactionWithFees txf =
                        new TransactionWithFees(i, tx, fees);
                log(txf.toString());
                txPosToFeesMap.put(i, txf);
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
            loop = processRejects(accepted, rejected, tempPool);
        }

        if (!accepted.isEmpty()) {
            double totalFees =
                    accepted.stream().mapToDouble(t -> t.fees).sum();
            log("totalFees: " + totalFees, true);
            log("accepted: " + txIndexToString(accepted), true);
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
            log("removed: " + utxoToString(ut));
            spentUTXOs.put(ut, out);
            pool.removeUTXO(ut);
        });

        for (int i = 0; i < transaction.tx.getOutputs().size(); i++) {
            UTXO ut = new UTXO(transaction.tx.getHash(), i);
            pool.addUTXO(ut, transaction.tx.getOutput(i));
            log("added: " + utxoToString(ut));
        }

        transaction.spentUTXOs = spentUTXOs;
    }

    /**
     * Process all previously rejected transactions.
     *
     * @param accepted a list of already accepted transactions
     * @param rejected a list of already rejected transactions
     * @param pool     a pool of unspent transaction outputs
     * @return true one or more rejected transaction is successfully added to
     * the accepted list, false otherwise
     */
    private boolean processRejects(
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected, UTXOPool pool) {
        boolean success = false;

        for (TransactionWithFees tx : rejected) {
            success = success || processReject(tx, accepted, rejected, pool);
        }

        return success;
    }

    /**
     * Process a previously rejected transaction.
     *
     * @param transaction the rejected transaction currenly being processed
     * @param accepted    a list of already accepted transactions
     * @param rejected    a list of already rejected transactions
     * @param pool        a pool of unspent transaction outputs
     * @return true if the reject is successfully added to the accepted list,
     * false otherwise
     */
    private boolean processReject(
            TransactionWithFees transaction,
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            UTXOPool pool) {
        List<TransactionWithFees> maybes = new CopyOnWriteArrayList<>();
        List<TransactionWithFees> shared = new CopyOnWriteArrayList<>();
        List<TransactionWithFees> rejectedShared = new CopyOnWriteArrayList<>();

        boolean success =
                findSharedAndRejectedSiblings(transaction, accepted, rejected,
                        pool, maybes, shared, rejectedShared);

        while (success && !rejectedShared.isEmpty()) {
            TransactionWithFees txf = rejectedShared.remove(0);
            success = findSharedAndRejectedSiblings(txf, accepted, rejected,
                    pool, maybes, shared, rejectedShared);
        }

        if (success) {
            return processSharedAndAddIfNeeded(accepted, rejected,
                    pool, maybes, shared);
        }

        return false;
    }

    /**
     * Finds the following:
     * <ol>
     * <li>Already accepted transactions which is in conflict with the rejected
     * transaction currently being processed.</li>
     * <li>Other sibling rejected transactions which need to processed
     * with the rejected transaction currenly being processed.</li>
     * </ol>
     *
     * @param transaction    the rejected transaction currenly being processed
     * @param accepted       a list of already accepted transactions
     * @param rejected       a list of already rejected transactions
     * @param pool           a pool of unspent transaction outputs
     * @param maybes         a list of previously rejected transactions which
     *                       are
     *                       elligible for acceptance
     * @param shared         a list of already accepted transactions which
     *                       shares the
     *                       same input transaction as a rejected transaction
     * @param rejectedShared a list of rejected transactions on which needs to
     *                       be processed together
     * @return true if all the input UTXOs for the current rejected transaction
     * are found, false otherwise
     */
    private boolean findSharedAndRejectedSiblings(
            TransactionWithFees transaction,
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            UTXOPool pool,
            List<TransactionWithFees> maybes,
            List<TransactionWithFees> shared,
            List<TransactionWithFees> rejectedShared) {
        log("Trying to add reject with id: " + transaction.index);
        log("already accepted: " + txIndexToString(accepted));

        int before = shared.size();
        int count = 0;
        for (Transaction.Input in : transaction.tx.getInputs()) {
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            if (!pool.contains(ut)) {
                boolean found = false;
                for (TransactionWithFees txf : accepted) {
                    if (txf.spentUTXOs.containsKey(ut)) {
                        log("in: " + getHash(in.prevTxHash) + " - " +
                                in.outputIndex + " found dependency in " +
                                txf.index);
                        count++;
                        found = true;
                        if (!shared.contains(txf)) {
                            shared.add(txf);
                        }
                        break;
                    }
                }

                if (!found) {
                    for (TransactionWithFees rtxf : rejected) {
                        if (transaction.index != rtxf.index
                                && !maybes.contains(rtxf)
                                && (Arrays.equals(in.prevTxHash,
                                rtxf.tx.getHash())
                                && in.outputIndex < rtxf.tx.numOutputs())) {
                            log("in: " + getHash(in.prevTxHash) +
                                    " - " + in.outputIndex +
                                    " found dependency in " + rtxf.index);
                            count++;
                            found = true;
                            if (!rejectedShared.contains(rtxf)) {
                                rejectedShared.add(rtxf);
                            }
                        }
                    }
                }

                if (!found) {
                    log("in: " + getHash(in.prevTxHash) +
                            " - " + in.outputIndex + " not found");
                }
            } else {
                log("in: " + getHash(in.prevTxHash) +
                        " - " + in.outputIndex + " found dependency in pool");
                count++;
            }
        }

        log("Found dependencies in rejected: " + txIndexToString(accepted));

        int after = shared.size();
        if (count == transaction.tx.numInputs() && (after == before ||
                !checkTransitiveDependencies(accepted, shared, pool))) {
            maybes.add(transaction);
            return true;
        }

        return false;
    }

    /**
     * Does the following:
     * <ol>
     * <li>Checks if the output transactions of all the shared transactions are
     * present in the UTXO pool.</li>
     * <li>If it's not present, checks if any previously accepted transaction
     * has consumed that transaction.</li>
     * <li>If yes, puts the newly found transaction in the shared bucket.</li>
     * <li>This method recursiveley finds all the transitive dependencies.</li>
     * <li>If an output transaction is not found, it returns true as there's a
     * conflict.</li>
     * </ol>
     *
     * @param accepted a list of already accepted transactions
     * @param shared   a list of already accepted transactions which shares the
     *                 same input transaction as a rejected transaction
     * @param pool     a pool of unspent transaction outputs
     * @return true if an output transaction is not found, false otherwise
     */
    private boolean checkTransitiveDependencies(
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> shared,
            UTXOPool pool) {
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

        // recurse if a new shared transaction is found
        if (shared.size() > initialSharedTxsSize) {
            return checkTransitiveDependencies(accepted, shared, pool);
        }

        return false;
    }

    /**
     * Process newly found rejected transactions in 'maybes' bucket
     * which is elligible for acceptance if the previously accepted transactions
     * in 'shared' bucket are removed. Transactions in 'maybes' bucket are
     * accepted if they are valid after the removal of transations in 'shared'
     * bucket and if the total transaction fees of 'maybes' bucket is more
     * than total transaction fees of 'shared' bucket.
     *
     * @param accepted a list of already accepted transactions
     * @param rejected a list of previoulsy rejected transactions
     * @param pool     a pool of unspent transaction outputs
     * @param maybes   a list of previously rejected transactions which are
     *                 elligible for acceptance
     * @param shared   a list of already accepted transactions which shares the
     *                 same input transaction as a rejected transaction
     * @return true if the transactions in the 'maybes' bucket is added to the
     * accepted bucket successfully, false otherwise
     */
    private boolean processSharedAndAddIfNeeded(
            List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            UTXOPool pool,
            List<TransactionWithFees> maybes,
            List<TransactionWithFees> shared) {
        UTXOPool tempPool = new UTXOPool(pool);

        log("Aleady accepted: " + txIndexToString(accepted));
        log("Trying to add: " + txIndexToString(maybes));
        log("Shared: " + txIndexToString(shared));

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

        // dry run for checking all 'maybes' are valid
        double fees = 0;
        for (int t = maybes.size() - 1; t >= 0; t--) {
            TransactionWithFees txf = maybes.get(t);
            if (isValidTx(txf.tx, tempPool)) {
                fees += txf.fees;
                updatePool(txf, tempPool);
            } else {
                return false;
            }
        }

        // real run - add all 'maybes'
        return addMaybes(accepted, rejected, pool, maybes, shared,
                oldFees, fees);
    }

    /**
     * Finally add maybes if neew fees are greater than old fees.
     *
     * @param accepted a list of already accepted transactions
     * @param rejected a list of previoulsy rejected transactions
     * @param pool     a pool of unspent transaction outputs
     * @param maybes   a list of previously rejected transactions which are
     *                 elligible for acceptance
     * @param shared   a list of already accepted transactions which shares the
     *                 same input transaction as a rejected transaction
     * @param oldFees  total sum of old fees
     * @param fees     total sum  of new fees
     * @return true if the transactions in the 'maybes' bucket is added to the
     * accepted bucket successfully, false otherwise
     */
    private boolean addMaybes(List<TransactionWithFees> accepted,
            List<TransactionWithFees> rejected,
            UTXOPool pool,
            List<TransactionWithFees> maybes,
            List<TransactionWithFees> shared,
            double oldFees, double fees) {
        if (shared.isEmpty() || (fees > oldFees)) {
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

            for (int t = maybes.size() - 1; t >= 0; t--) {
                TransactionWithFees txf = maybes.get(t);
                updatePool(txf, pool);
                accepted.add(txf);
                rejected.remove(txf);
            }

            return true;
        }

        return false;
    }

    /**
     * Gets a Base64 encoded string a byte array
     *
     * @param input a byte to be encoded
     * @return an encoded string
     */
    private String getHash(byte[] input) {
        return Base64.getUrlEncoder().encodeToString(input);
    }

    /**
     * Returns a string representation of an UTXO.
     *
     * @param utxo the UTXO to be converted to a string
     * @return UTXO as a string
     */
    private String utxoToString(UTXO utxo) {
        StringBuilder bldr = new StringBuilder("UTXO[");
        bldr.append(utxo.getIndex()).append("]: ").append(
                getHash(utxo.getTxHash()));

        return super.toString();
    }

    /**
     * Returns a string representation of an UTXO pool.
     *
     * @param pool the UTXO pool to be converted to a string
     * @return UTXO pool as a string
     */
    private String poolToString(UTXOPool pool) {
        StringBuilder bldr = new StringBuilder("pool:\n");
        pool.getAllUTXO().forEach(
                u -> bldr.append(utxoToString(u)).append("\n"));

        return bldr.toString();
    }

    /**
     * Returns a string representation of a list of transactions.
     *
     * @param txs a list of transaction
     * @return a list of transactions as a string
     */
    private String txsToString(List<TransactionWithFees> txs) {
        StringBuilder bldr = new StringBuilder();
        txs.forEach(t -> bldr.append(t).append("\n"));

        return bldr.toString();
    }

    /**
     * Returns a list of transactions as an array of indices.
     *
     * @param txs a list of transaction
     * @return a list of transactions as an array of indices
     */
    private String txIndexToString(List<TransactionWithFees> txs) {
        StringBuilder bldr = new StringBuilder("[");
        txs.forEach(t -> bldr.append(t.index).append(" "));
        bldr.append("]");

        return bldr.toString();
    }

    /**
     * Logs a message
     *
     * @param str a string to be logged
     */
    private void log(String str) {
        log(str, false);
    }

    /**
     * Logs a message
     *
     * @param str      a string to be logged
     * @param override logs a message even if the LOG_ON flag is off.
     */
    private void log(String str, boolean override) {
        if (LOG_ON || override) {
            System.out.println(str);
        }
    }

    /**
     * {@code TransactionWithFees} a wrapper cclass whick keeps track of a
     * transaction along its position in the possible transactions,
     * previously calculated transaction fees, input UTXOs, and spent UTXOs.
     * <p/>
     *
     * @author Indra Basak
     * @since 05/14/18
     */
    private class TransactionWithFees {
        private int index;

        private Transaction tx;

        private double fees;

        private Map<UTXO, Transaction.Output> spentUTXOs;

        public TransactionWithFees(int index, Transaction tx, double fees) {
            this.index = index;
            this.tx = tx;
            this.fees = fees;
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
            return (index == other.index) ? true : false;
        }

        @Override
        public String toString() {
            StringBuilder bldr = new StringBuilder("Tx[");
            bldr.append(index).append("]: ").append(getHash(tx.getHash()));

            return super.toString();
        }
    }
}
