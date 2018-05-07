import java.security.KeyPair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@code UTXOPoolTest} represents an unit test for {@code UTXOPool}.
 * <p/>
 *
 * @author Indra Basak
 * @since 05/07/2018
 */
@SuppressWarnings({"squid:S00112", "squid:ObjectFinalizeCheck", "squid:S1220", "squid:S1854"})
public class UTXOPoolTest {

    @Test
    public void testAddUTXO() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();

        Transaction tx = new Transaction();
        tx.addOutput(2.5, pair.getPublic());
        tx.finalize();

        int index = 0;
        Transaction.Output output = tx.getOutput(index);
        assertNotNull(output);

        UTXOPool pool = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(), index);
        pool.addUTXO(utxo, output);
        assertEquals(output, pool.getTxOutput(utxo));
        assertTrue(pool.contains(utxo));
    }

    @Test
    public void testRemoveUTXO() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();

        Transaction tx = new Transaction();
        tx.addOutput(4.5, pair.getPublic());
        tx.finalize();

        int index = 0;
        Transaction.Output output = tx.getOutput(index);
        assertNotNull(output);

        UTXOPool pool = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(), index);
        pool.addUTXO(utxo, output);
        assertTrue(pool.contains(utxo));

        pool.removeUTXO(utxo);
        assertFalse(pool.contains(utxo));
    }

    @Test
    public void testGetAllUTXO() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();

        Transaction tx = new Transaction();
        tx.addOutput(4.2, pair.getPublic());
        tx.addOutput(3.7, pair.getPublic());
        tx.finalize();

        UTXOPool pool = new UTXOPool();
        Transaction.Output output1 = tx.getOutput(0);
        assertNotNull(output1);
        UTXO utxo1 = new UTXO(tx.getHash(), 0);
        pool.addUTXO(utxo1, output1);

        Transaction.Output output2 = tx.getOutput(1);
        assertNotNull(output2);
        UTXO utxo2 = new UTXO(tx.getHash(), 1);
        pool.addUTXO(utxo2, output2);

        assertTrue(pool.getAllUTXO().size() == 2);
        assertTrue(pool.contains(utxo1));
        assertTrue(pool.contains(utxo2));
    }

    @Test
    public void testCopyConstructor() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();

        Transaction tx = new Transaction();
        tx.addOutput(221.5, pair.getPublic());
        tx.addOutput(4.6, pair.getPublic());
        tx.finalize();

        UTXOPool pool = new UTXOPool();
        Transaction.Output output1 = tx.getOutput(0);
        assertNotNull(output1);
        UTXO utxo1 = new UTXO(tx.getHash(), 0);
        pool.addUTXO(utxo1, output1);

        Transaction.Output output2 = tx.getOutput(1);
        assertNotNull(output2);
        UTXO utxo2 = new UTXO(tx.getHash(), 1);
        pool.addUTXO(utxo2, output2);
        assertTrue(pool.getAllUTXO().size() == 2);

        UTXOPool pool2 = new UTXOPool(pool);
        assertTrue(pool2.getAllUTXO().size() == 2);

    }
}
