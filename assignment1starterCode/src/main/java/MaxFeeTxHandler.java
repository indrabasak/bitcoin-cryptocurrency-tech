import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1220"})
public class MaxFeeTxHandler {
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
    public boolean isValidTx(Transaction tx) {
        double inputSum = 0.0;
        Set<UTXO> utxos = new HashSet<>();

        ArrayList<Transaction.Input> inputs = tx.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Transaction.Input input = inputs.get(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);


            // CASE 1: all outputs claimed by {@code tx} are in the
            // current UTXO pool
            if (!utxoPool.contains(utxo)) {
                return false;
            }

            Transaction.Output inputsOutput = utxoPool.getTxOutput(utxo);

            // CASE 2: the signatures on each input of {@code tx} are valid
            if (!Crypto.verifySignature(inputsOutput.address,
                    tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            // CASE 3: no UTXO is claimed multiple times by {@code tx}
            if (utxos.contains(utxo)) {
                return false;
            }

            utxos.add(utxo);
            inputSum += inputsOutput.value;
        }

        double outputSum = 0.0;
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        for (Transaction.Output output : outputs) {
            // CASE 4: all of {@code tx}s output values are non-negative
            if (output.value < 0) {
                return false;
            }

            outputSum += output.value;
        }

        // CASE 5: the sum of {@code tx}s input values is greater than or
        // equal to the sum of its output values
        return inputSum >= outputSum;
    }

    /**
     * Calculates transaction fee.
     *
     * @param tx
     * @return
     */
    public double calculateFee(Transaction tx) {
        double inputSum = 0.0;
        Set<UTXO> utxos = new HashSet<>();

        ArrayList<Transaction.Input> inputs = tx.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Transaction.Input input = inputs.get(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            if (utxoPool.contains(utxo) && !utxos.contains(utxo)) {
                utxos.add(utxo);
                inputSum += utxoPool.getTxOutput(utxo).value;
            }
        }

        double outputSum = 0.0;
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        for (Transaction.Output output : outputs) {
            // CASE 4: all of {@code tx}s output values are non-negative
            if (output.value >= 0) {
                outputSum += output.value;
            }
        }

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
        Set<Transaction> sortedTxs = new TreeSet<>(
                (tx1, tx2) -> Double.valueOf(calculateFee(tx2)).compareTo(
                        calculateFee(tx1)));

        sortedTxs.addAll(Arrays.asList(possibleTxs));

        List<Transaction> acceptedTxs = new ArrayList<>();
        for (Transaction tx : sortedTxs) {
            if (isValidTx(tx)) {
                for (Transaction.Input input : tx.getInputs()) {
                    utxoPool.removeUTXO(
                            new UTXO(input.prevTxHash, input.outputIndex));
                }

                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output output = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, output);
                }

                acceptedTxs.add(tx);
            }
        }

        return acceptedTxs.toArray(new Transaction[0]);
    }
}
