import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the
     * UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
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

        ArrayList<Transaction.Input> inputs = tx.getInputs();

        double inputSum = 0.0;
        Set<UTXO> utxos = new HashSet<>();

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
     * Handles each epoch by receiving an unordered array of proposed
     * transactions, checking each transaction for correctness, returning a
     * mutually valid array of accepted transactions, and updating the current
     * UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> acceptedTxs = new ArrayList<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
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

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }
}
