import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

        MaxFeeTxHandler handler = new MaxFeeTxHandler(pool);

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

    @Test
    public void testHandleTxsComplex() throws Exception {
        // Generating two key pairs, one for Scrooge and one for Alice
        KeyPair pair = TestUtil.generateKeyPair();
        PrivateKey private_key_scrooge = pair.getPrivate();
        PublicKey public_key_scrooge = pair.getPublic();

        pair = TestUtil.generateKeyPair();
        PrivateKey private_key_alice = pair.getPrivate();
        PublicKey public_key_alice = pair.getPublic();

        // START - ROOT TRANSACTION
        // Generating a root transaction tx out of thin air, so that Scrooge owns a coin of value 10
        // By thin air I mean that this tx will not be validated, I just need it to get a proper Transaction.Output
        // which I then can put in the UTXOPool, which will be passed to the TXHandler
        Transaction tx = new Transaction();
        tx.addOutput(10, public_key_scrooge);

        // that value has no meaning, but tx.getRawDataToSign(0) will access in.prevTxHash;
        byte[] initialHash = BigInteger.valueOf(1695609641).toByteArray();
        tx.addInput(initialHash, 0);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(private_key_scrooge);
        signature.update(tx.getRawDataToSign(0));
        byte[] sig = signature.sign();

        tx.addSignature(sig, 0);
        tx.finalize();
        // END - ROOT TRANSACTION

        // The transaction output of the root transaction is unspent output
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(), 0);
        utxoPool.addUTXO(utxo, tx.getOutput(0));

        // START - PROPER TRANSACTION
        Transaction tx2 = new Transaction();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx2.addInput(tx.getHash(), 0);

        // I split the coin of value 10 into 3 coins and send all of them for simplicity to the same address (Alice)
        tx2.addOutput(5, public_key_alice);
        tx2.addOutput(3, public_key_alice);
        tx2.addOutput(2, public_key_alice);

        // There is only one (at position 0) Transaction.Input in tx2
        // and it contains the coin from Scrooge, therefore I have to sign with the private key from Scrooge
        signature.initSign(private_key_scrooge);
        signature.update(tx2.getRawDataToSign(0));
        sig = signature.sign();
        tx2.addSignature(sig, 0);
        tx2.finalize();

        // remember that the utxoPool contains a single unspent Transaction.Output which is the coin from Scrooge
        TxHandler txHandler = new TxHandler(utxoPool);
        //System.out.print(txHandler.isValidTx(tx2));
        //System.out.print(txHandler.handleTxs(new Transaction[]{tx2}).length);
    }

    @Test
    public void testHandleTxsComplex2() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        // Tx 1
        // output: 1. $10
        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());

        tx1.finalize();
        UTXO utxo1 = new UTXO(tx1.getHash(), 0);
        pool.addUTXO(utxo1, tx1.getOutput(0));

        // Tx 2
        // output: 1. $5
        Transaction tx2 = new Transaction();
        tx2.addOutput(5.0, pair.getPublic());
        tx2.finalize();
        UTXO utxo2 = new UTXO(tx2.getHash(), 0);
        pool.addUTXO(utxo2, tx2.getOutput(0));

        MaxFeeTxHandler handler = new MaxFeeTxHandler(pool);

        // Transaction 0
        // input: 1. Tx1's - $10
        // output: $9
        // fees: 10 - 9 = $1
        Transaction transaction0 = new Transaction();
        transaction0.addInput(tx1.getHash(), 0);
        transaction0.addOutput(9, pair.getPublic());

        transaction0.getInput(0).addSignature(TestUtil.createSignature(
                transaction0.getRawDataToSign(0),
                pair.getPrivate()));
        transaction0.finalize();

        // Transaction 1
        // input: 1. Tx2's - $5
        // output: $3
        // fees: 5 - 3 = $2
        Transaction transaction1 = new Transaction();
        transaction1.addInput(tx2.getHash(), 0);
        transaction1.addOutput(3, pair.getPublic());

        transaction1.getInput(0).addSignature(TestUtil.createSignature(
                transaction1.getRawDataToSign(0),
                pair.getPrivate()));
        transaction1.finalize();

        // Transaction 2
        // input: 1. Tx1's - $10
        // input: 2. transaction1's - $3
        // output: $5
        // fees: 10 + 3 - 5 = $8
        Transaction transaction2 = new Transaction();
        transaction2.addInput(tx1.getHash(), 0);
        transaction2.addInput(transaction1.getHash(), 0);
        transaction2.addOutput(5, pair.getPublic());

        transaction2.getInput(0).addSignature(TestUtil.createSignature(
                transaction2.getRawDataToSign(0),
                pair.getPrivate()));
        transaction2.getInput(1).addSignature(TestUtil.createSignature(
                transaction2.getRawDataToSign(1),
                pair.getPrivate()));

        transaction2.finalize();

        Transaction[] possibleTxs = new Transaction[3];
        possibleTxs[0] = transaction0;
        possibleTxs[1] = transaction1;
        possibleTxs[2] = transaction2;

        // Transaction0 and Transaction2 has UTXO conflict
        // Transaction2 is dependent on Transaction1
        // successful combination Transaction1 and Transaction2

        Transaction[] validTxs = handler.handleTxs(possibleTxs);
        assertEquals(2, validTxs.length);
    }

    @Test
    public void testHandleTxsComplex3() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        UTXOPool pool = new UTXOPool();

        // Tx 1
        // output: 1. $10
        Transaction tx1 = new Transaction();
        tx1.addOutput(10.0, pair.getPublic());
        tx1.finalize();
        UTXO utxo1 = new UTXO(tx1.getHash(), 0);
        pool.addUTXO(utxo1, tx1.getOutput(0));

        // Tx 2
        // output: 1. $5
        Transaction tx2 = new Transaction();
        tx2.addOutput(5.0, pair.getPublic());
        tx2.finalize();
        UTXO utxo2 = new UTXO(tx2.getHash(), 0);
        pool.addUTXO(utxo2, tx2.getOutput(0));

        MaxFeeTxHandler handler = new MaxFeeTxHandler(pool);

        // Transaction 1
        // input: 1. Tx1's - $10
        // output: $7, $2
        // fees: 10 - 9 = $1
        Transaction transaction0 = new Transaction();
        transaction0.addInput(tx1.getHash(), 0);
        transaction0.addOutput(7, pair.getPublic());
        transaction0.addOutput(2, pair.getPublic());

        transaction0.getInput(0).addSignature(TestUtil.createSignature(
                transaction0.getRawDataToSign(0),
                pair.getPrivate()));
        transaction0.finalize();

        // Transaction 1
        // input: 1. Tx2's - $5
        // output: $3
        // fees: 5 - 3 = $2
        Transaction transaction1 = new Transaction();
        transaction1.addInput(tx2.getHash(), 0);
        transaction1.addOutput(3, pair.getPublic());

        transaction1.getInput(0).addSignature(TestUtil.createSignature(
                transaction1.getRawDataToSign(0),
                pair.getPrivate()));
        transaction1.finalize();

        // Transaction 2
        // input: 1. Transaction0's - $2
        // output: $1
        // fees: 2 - 1 = $1
        Transaction transaction2 = new Transaction();
        transaction2.addInput(transaction0.getHash(), 1);
        transaction2.addOutput(1, pair.getPublic());

        transaction2.getInput(0).addSignature(TestUtil.createSignature(
                transaction2.getRawDataToSign(0),
                pair.getPrivate()));
        transaction2.finalize();

        // Transaction 3
        // input: 1. Tx1's - $10
        // input: 2. transaction1's - $3
        // output: $5
        // fees: 10 + 3 - 5 = $8
        Transaction transaction3 = new Transaction();

        transaction3.addInput(tx1.getHash(), 0);
        transaction3.addInput(transaction1.getHash(), 0);
        transaction3.addOutput(5, pair.getPublic());

        transaction3.getInput(0).addSignature(TestUtil.createSignature(
                transaction3.getRawDataToSign(0),
                pair.getPrivate()));
        transaction3.getInput(1).addSignature(TestUtil.createSignature(
                transaction3.getRawDataToSign(1),
                pair.getPrivate()));

        transaction3.finalize();

        Transaction[] possibleTxs = new Transaction[4];
        possibleTxs[0] = transaction0;
        possibleTxs[1] = transaction1;
        possibleTxs[2] = transaction2;
        possibleTxs[3] = transaction3;

        System.out.println(
                "transaction0.hash: " + Base64.getUrlEncoder().encodeToString(
                        transaction0.getHash()));
        System.out.println(
                "transaction1.hash: " + Base64.getUrlEncoder().encodeToString(
                        transaction1.getHash()));
        System.out.println(
                "transaction2.hash: " + Base64.getUrlEncoder().encodeToString(
                        transaction2.getHash()));
        System.out.println(
                "transaction3.hash: " + Base64.getUrlEncoder().encodeToString(
                        transaction3.getHash()));

        // Transaction0 and Transaction3 has UTXO conflict
        // Transaction2 is dependent on Transaction0's output
        // Transaction3 is dependent on Transaction1's output
        // Expected: Transaction1 and Transaction3


        Transaction[] validTxs = handler.handleTxs(possibleTxs);
        assertEquals(2, validTxs.length);
    }
}
