import org.junit.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@code CryptoTest} represents an unit test for {@code Crypto}.
 * <p/>
 *
 * @author Indra Basak
 * @since 05/07/2018
 */
@SuppressWarnings({"squid:S00112", "squid:ObjectFinalizeCheck", "squid:S1220", "squid:S1854"})
public class CryptoTest {

    @Test
    public void testVerifySignature() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        String message = "Hello There!";
        byte[] signature =
                TestUtil.createSignature(message.getBytes(), privateKey);

        boolean valid = Crypto.verifySignature(publicKey, message.getBytes(),
                signature);
        assertTrue(valid);
    }

    @Test
    public void testVerifySignatureInvalid() throws Exception {
        KeyPair pair = TestUtil.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();

        String message = "Hello There!";
        byte[] signature =
                TestUtil.createSignature(message.getBytes(), privateKey);

        KeyPair pair2 = TestUtil.generateKeyPair();

        boolean valid = Crypto.verifySignature(pair2.getPublic(),
                message.getBytes(), signature);
        assertFalse(valid);
    }
}
