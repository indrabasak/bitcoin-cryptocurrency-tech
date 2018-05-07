import java.security.KeyPair;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@code TransactionTest} represents an unit test for {@code Transaction}.
 * <p/>
 *
 * @author Indra Basak
 * @since 05/07/2018
 */
@SuppressWarnings({"squid:S00112", "squid:ObjectFinalizeCheck", "squid:S1220"})
public class TransactionTest {

    @Test
    public void testAddOutput() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();

        Transaction tx = new Transaction();
        tx.addOutput(15.0, pair.getPublic());
        assertEquals(1, tx.getOutputs().size());

        tx.addOutput(10.0, pair.getPublic());
        assertEquals(2, tx.numOutputs());
    }

    @Test
    public void testAddAndGetAndRemoveInput() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();

        Transaction tx = new Transaction();

        Transaction tx1 = new Transaction();
        tx1.addOutput(5.0, pair.getPublic());
        tx1.finalize();
        tx.addInput(tx1.getHash(), 0);
        assertEquals(1, tx.getInputs().size());

        Transaction tx2 = new Transaction();
        tx2.addOutput(10.0, pair.getPublic());
        tx2.finalize();
        tx.addInput(tx2.getHash(), 0);
        assertEquals(2, tx.getInputs().size());

        Transaction.Input input = tx.getInput(1);
        assertNotNull(input);
        assertEquals(0, input.outputIndex);
        assertArrayEquals(tx2.getHash(), input.prevTxHash);

        byte[] signature = TestUtil.createSignature(
                tx.getRawDataToSign(1),
                pair.getPrivate());
        input.addSignature(signature);
        assertArrayEquals(signature, input.signature);

        tx.removeInput(1);
        assertEquals(1, tx.numInputs());

        tx.removeInput(new UTXO(tx1.getHash(), 0));
        assertEquals(0, tx.numInputs());
    }

    @Test
    public void testFinalize() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();

        Transaction tx1 = new Transaction();
        tx1.addOutput(5.0, pair.getPublic());
        tx1.finalize();

        Transaction tx = new Transaction();
        tx.addInput(tx1.getHash(), 0);
        tx.addOutput(3.0, pair.getPublic());
        tx.finalize();
        assertTrue(tx.getHash().length > 0);
    }

    @Test
    public void testSignatureAndCloning() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();

        Transaction tx = new Transaction();

        Transaction tx1 = new Transaction();
        tx1.addOutput(5.0, pair.getPublic());
        tx1.finalize();
        tx.addInput(tx1.getHash(), 0);

        Transaction tx2 = new Transaction();
        tx2.addOutput(10.0, pair.getPublic());
        tx2.finalize();
        tx.addInput(tx2.getHash(), 0);

        KeyPair pair2 = TestUtil.generateKeyPair();
        tx.addOutput(12.0, pair2.getPublic());

        Transaction.Input input = tx.getInput(0);
        byte[] signature = TestUtil.createSignature(
                tx.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);

        Transaction.Input input2 = tx.getInput(1);
        byte[] signature2 = TestUtil.createSignature(
                tx.getRawDataToSign(1),
                pair.getPrivate());
        input2.addSignature(signature2);

        tx.finalize();
        assertTrue(tx.getRawTx().length > 0);

        Transaction cloneTx = new Transaction(tx);
        assertEquals(2, cloneTx.numInputs());
        assertEquals(1, cloneTx.numOutputs());
    }
}
