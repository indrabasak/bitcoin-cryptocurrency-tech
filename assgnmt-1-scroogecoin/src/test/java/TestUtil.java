import java.security.*;

/**
 * {@code TestUtil} is helper class for Java crypto.
 * <p/>
 *
 * @author Indra Basak
 * @since 05/07/2018
 */
public class TestUtil {

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair pair = generator.generateKeyPair();

        return pair;
    }

    public static byte[] createSignature(byte[] message,
            PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(message);

        return privateSignature.sign();
    }

    public static byte[] getSha256Hash(String message) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(message.getBytes());
        return digest.digest();
    }
}
