import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestBlockChain {

    @Test
    public void testBlockChain() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair pair = generator.generateKeyPair();

        Block genesisBlock = new Block(null, pair.getPublic());
        genesisBlock.finalize();
        BlockChain chain = new BlockChain(genesisBlock);
        Block maxHeightBlock = chain.getMaxHeightBlock();
        assertEquals(genesisBlock, maxHeightBlock);

        BlockHandler handler = new BlockHandler(chain);

        generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair pair2 = generator.generateKeyPair();
        Block block = handler.createBlock(pair2.getPublic());

        maxHeightBlock = chain.getMaxHeightBlock();
        assertEquals(block, maxHeightBlock);

        Block block3 = new Block(genesisBlock.getHash(), pair.getPublic());
        block3.finalize();

        assertTrue(chain.addBlock(block3));
    }
}
