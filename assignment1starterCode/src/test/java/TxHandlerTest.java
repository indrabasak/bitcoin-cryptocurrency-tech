import java.security.KeyPair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@code TxHandlerTest} represents an unit test for {@code TxHandler}.
 * <p/>
 *
 * @author Indra Basak
 * @since 05/07/2018
 */
@SuppressWarnings({"squid:S00112", "squid:ObjectFinalizeCheck", "squid:S1220"})
public class TxHandlerTest {

    @Test
    public void testIsValidTxValid() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());
        tx1.finalize();
        UTXO utxo1 = new UTXO(tx1.getHash(), 0);
        pool.addUTXO(utxo1, tx1.getOutput(0));

        Transaction tx2 = new Transaction();
        tx2.addOutput(5.0, pair.getPublic());
        tx2.finalize();
        UTXO utxo2 = new UTXO(tx2.getHash(), 0);
        pool.addUTXO(utxo2, tx2.getOutput(0));

        TxHandler handler = new TxHandler(pool);

        Transaction tx = new Transaction();
        tx.addInput(tx1.getHash(), 0);
        tx.addOutput(7.50, pair.getPublic());

        Transaction.Input input = tx.getInput(0);
        byte[] signature = TestUtil.createSignature(
                tx.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);

        tx.finalize();
        assertTrue(tx.getRawTx().length > 0);

        assertTrue(handler.isValidTx(tx));
    }

    @Test
    public void testIsValidTxMissingUtxo() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());
        tx1.finalize();

        TxHandler handler = new TxHandler(pool);

        Transaction tx = new Transaction();
        tx.addInput(tx1.getHash(), 0);
        tx.addOutput(7.50, pair.getPublic());

        Transaction.Input input = tx.getInput(0);
        byte[] signature = TestUtil.createSignature(
                tx.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);

        tx.finalize();
        assertTrue(tx.getRawTx().length > 0);

        assertFalse(handler.isValidTx(tx));
    }

    @Test
    public void testIsValidTxInvalidSignature() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());
        tx1.finalize();
        UTXO utxo1 = new UTXO(tx1.getHash(), 0);
        pool.addUTXO(utxo1, tx1.getOutput(0));

        TxHandler handler = new TxHandler(pool);

        Transaction tx = new Transaction();
        tx.addInput(tx1.getHash(), 0);
        tx.addOutput(7.50, pair.getPublic());

        KeyPair pair2 = TestUtil.generateKeyPair();
        Transaction.Input input = tx.getInput(0);
        byte[] signature = TestUtil.createSignature(
                tx.getRawDataToSign(0),
                pair2.getPrivate());
        input.addSignature(signature);

        tx.finalize();
        assertTrue(tx.getRawTx().length > 0);

        assertFalse(handler.isValidTx(tx));
    }

    @Test
    public void testIsValidTxMultipleUtxoClaim() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());
        tx1.finalize();
        UTXO utxo1 = new UTXO(tx1.getHash(), 0);
        pool.addUTXO(utxo1, tx1.getOutput(0));

        TxHandler handler = new TxHandler(pool);

        Transaction tx = new Transaction();
        tx.addInput(tx1.getHash(), 0);
        tx.addInput(tx1.getHash(), 0);
        tx.addOutput(7.50, pair.getPublic());

        Transaction.Input input = tx.getInput(0);
        byte[] signature = TestUtil.createSignature(
                tx.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);

        input = tx.getInput(1);
        signature = TestUtil.createSignature(
                tx.getRawDataToSign(1),
                pair.getPrivate());
        input.addSignature(signature);

        tx.finalize();
        assertTrue(tx.getRawTx().length > 0);

        assertFalse(handler.isValidTx(tx));
    }

    @Test
    public void testIsValidTxNegativeOutput() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());
        tx1.finalize();
        UTXO utxo1 = new UTXO(tx1.getHash(), 0);
        pool.addUTXO(utxo1, tx1.getOutput(0));

        TxHandler handler = new TxHandler(pool);

        Transaction tx = new Transaction();
        tx.addInput(tx1.getHash(), 0);
        tx.addOutput(-7.50, pair.getPublic());

        Transaction.Input input = tx.getInput(0);
        byte[] signature = TestUtil.createSignature(
                tx.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);

        tx.finalize();
        assertTrue(tx.getRawTx().length > 0);

        assertFalse(handler.isValidTx(tx));
    }

    @Test
    public void testIsValidTxOutputExceedsInput() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());
        tx1.finalize();
        UTXO utxo1 = new UTXO(tx1.getHash(), 0);
        pool.addUTXO(utxo1, tx1.getOutput(0));

        TxHandler handler = new TxHandler(pool);

        Transaction tx = new Transaction();
        tx.addInput(tx1.getHash(), 0);
        tx.addOutput(17.50, pair.getPublic());
        tx.addOutput(10.00, pair.getPublic());

        Transaction.Input input = tx.getInput(0);
        byte[] signature = TestUtil.createSignature(
                tx.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);

        tx.finalize();
        assertTrue(tx.getRawTx().length > 0);

        assertFalse(handler.isValidTx(tx));
    }

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
        tx2.addOutput(5.0, pair.getPublic());
        tx2.finalize();
        UTXO utxo2 = new UTXO(tx2.getHash(), 0);
        pool.addUTXO(utxo2, tx2.getOutput(0));

        TxHandler handler = new TxHandler(pool);

        Transaction tx3 = new Transaction();
        tx3.addInput(tx1.getHash(), 0);
        tx3.addOutput(7.50, pair.getPublic());

        Transaction.Input input = tx3.getInput(0);
        byte[] signature = TestUtil.createSignature(
                tx3.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);
        tx3.finalize();

        Transaction tx4 = new Transaction();
        tx4.addInput(tx2.getHash(), 0);
        tx4.addOutput(10, pair.getPublic());

        input = tx4.getInput(0);
        signature = TestUtil.createSignature(
                tx4.getRawDataToSign(0),
                pair.getPrivate());
        input.addSignature(signature);
        tx4.finalize();

        Transaction[] possibleTxs = new Transaction[2];
        possibleTxs[0] = tx3;
        possibleTxs[1] = tx4;

        Transaction[] validTxs = handler.handleTxs(possibleTxs);
        assertEquals(1, validTxs.length);
        assertEquals(tx3, validTxs[0]);

        assertFalse(handler.contains(utxo1));
        assertTrue(handler.contains(utxo2));
    }
}
