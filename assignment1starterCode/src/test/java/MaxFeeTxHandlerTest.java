import java.security.KeyPair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@code MaxFeeTxHandlerTest} represents an unit test for {@code
 * MaxFeeTxHandler}.
 * <p/>
 *
 * @author Indra Basak
 * @since 05/07/2018
 */
@SuppressWarnings({"squid:S00112", "squid:ObjectFinalizeCheck", "squid:S1220"})
public class MaxFeeTxHandlerTest {

    @Test
    public void testHandleTxs() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());
        tx1.finalize();
        UTXO utxo1 = new UTXO(tx1.getHash(), 0);
        pool.addUTXO(utxo1, tx1.getOutput(0));

        Transaction tx2 = new Transaction();
        tx2.addOutput(25.0, pair.getPublic());
        tx2.finalize();
        UTXO utxo2 = new UTXO(tx2.getHash(), 0);
        pool.addUTXO(utxo2, tx2.getOutput(0));

        MaxFeeTxHandler handler = new  MaxFeeTxHandler(pool);

        Transaction transaction1 = new Transaction();
        transaction1.addInput(tx1.getHash(), 0);
        transaction1.addOutput(2, pair.getPublic());

        Transaction.Input input = transaction1.getInput(0);
        byte[] signature = TestUtil.createSignature(
                transaction1.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);
        transaction1.finalize();

        Transaction transaction2 = new Transaction();
        transaction2.addInput(tx2.getHash(), 0);
        transaction2.addOutput(5, pair.getPublic());

        input = transaction2.getInput(0);
        signature = TestUtil.createSignature(
                transaction2.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);
        transaction2.finalize();

        Transaction[] possibleTxs = new Transaction[2];
        possibleTxs[0] = transaction1;
        possibleTxs[1] = transaction2;

        Transaction[] validTxs = handler.handleTxs(possibleTxs);
        assertEquals(2, validTxs.length);
    }
}
