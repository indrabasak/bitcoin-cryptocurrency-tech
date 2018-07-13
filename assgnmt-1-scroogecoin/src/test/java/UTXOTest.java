import org.junit.Test;

import static org.junit.Assert.*;

/**
 * {@code UTXOTest} represents an unit test for {@code UTXO}.
 * <p/>
 *
 * @author Indra Basak
 * @since 05/07/2018
 */
public class UTXOTest {

    @Test
    public void testGetters() throws Exception {
        String msg = "Txn1";
        byte[] txHash = TestUtil.getSha256Hash(msg);
        int index = 1;

        UTXO utxo = new UTXO(txHash, index);
        assertNotNull(utxo);
        assertEquals(index, utxo.getIndex());
        assertArrayEquals(txHash, utxo.getTxHash());
    }

    @Test
    public void testEquality() throws Exception {
        String msg = "Txn1";
        byte[] txHash = TestUtil.getSha256Hash(msg);
        int index = 1;

        UTXO utxo1 = new UTXO(txHash, index);
        UTXO utxo2 = new UTXO(txHash, index);
        assertEquals(utxo1, utxo2);
        assertEquals(utxo1.hashCode(), utxo2.hashCode());
    }

    @Test
    public void testInequality() throws Exception {
        String msg1 = "Txn1";
        byte[] txHash1 = TestUtil.getSha256Hash(msg1);
        int index1 = 1;
        UTXO utxo1 = new UTXO(txHash1, index1);

        String msg2 = "Txn2";
        byte[] txHash2 = TestUtil.getSha256Hash(msg2);
        int index2 = 2;
        UTXO utxo2 = new UTXO(txHash2, index2);

        assertNotEquals(utxo1, utxo2);
        assertNotEquals(utxo1.hashCode(), utxo2.hashCode());

        utxo2 = new UTXO(txHash2, index1);
        assertNotEquals(utxo1, utxo2);
    }

    @Test
    public void textCompareTo() throws Exception {
        String msg = "Txn1";
        byte[] txHash = TestUtil.getSha256Hash(msg);
        int index = 1;

        UTXO utxo1 = new UTXO(txHash, index);
        UTXO utxo2 = new UTXO(txHash, index);
        assertEquals(0, utxo1.compareTo(utxo2));

        String msg3 = "Txn3";
        byte[] txHash3 = TestUtil.getSha256Hash(msg3);
        int index3 = 2;
        UTXO utxo3 = new UTXO(txHash3, index3);
        assertEquals(-1, utxo1.compareTo(utxo3));
        assertEquals(1, utxo3.compareTo(utxo2));
    }
}
