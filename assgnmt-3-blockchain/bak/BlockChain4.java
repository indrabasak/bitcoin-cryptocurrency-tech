// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Score: 93/100
 *
 * Running 27 tests.
 *
 * ######################
 * processBlock() tests:
 * ######################
 * Test 1: Process a block with no transactions
 * ==> passed
 *
 * Test 2: Process a block with a single valid transaction
 * ==> passed
 *
 * Test 3: Process a block with many valid transactions
 * ==> passed
 *
 * Test 4: Process a block with some double spends
 * ==> passed
 *
 * Test 5: Process a new genesis block
 * ==> passed
 *
 * Test 6: Process a block with an invalid prevBlockHash
 * ==> passed
 *
 * Test 7: Process blocks with different sorts of invalid transactions
 * ==> passed
 *
 * Test 8: Process multiple blocks directly on top of the genesis block
 * ==> FAILED
 *
 * Test 9: Process a block containing a transaction that claims a UTXO already claimed by a transaction in its parent
 * ==> passed
 *
 * Test 10: Process a block containing a transaction that claims a UTXO not on its branch
 * ==> passed
 *
 * Test 11: Process a block containing a transaction that claims a UTXO from earlier in its branch that has not yet been claimed
 * ==> passed
 *
 * Test 12: Process a linear chain of blocks
 * ==> passed
 *
 * Test 13: Process a linear chain of blocks of length CUT_OFF_AGE and then a block on top of the genesis block
 * ==> passed
 *
 * Test 14: Process a linear chain of blocks of length CUT_OFF_AGE + 1 and then a block on top of the genesis block
 * ==> passed
 *
 * ######################
 * createBlock() tests:
 * ######################
 * Test 15: Create a block when no transactions have been processed
 * ==> passed
 *
 * Test 16: Create a block after a single valid transaction has been processed
 * ==> passed
 *
 * Test 17: Create a block after a valid transaction has been processed, then create a second block
 * ==> passed
 *
 * Test 18: Create a block after a valid transaction has been processed that is already in a block in the longest valid branch
 * ==> passed
 *
 * Test 19: Create a block after a valid transaction has been processed that uses a UTXO already claimed by a transaction in the longest valid branch
 * ==> passed
 *
 * Test 20: Create a block after a valid transaction has been processed that is not a double spend on the longest valid branch and has not yet been included in any other block
 * ==> passed
 *
 * Test 21: Create a block after only invalid transactions have been processed
 * ==> passed
 *
 * ######################
 * Combination tests:
 * ######################
 * Test 22: Process a transaction, create a block, process a transaction, create a block, ...
 * ==> passed
 *
 * Test 23: Process a transaction, create a block, then process a block on top of that block with a transaction claiming a UTXO from that transaction
 * ==> passed
 *
 * Test 24: Process a transaction, create a block, then process a block on top of the genesis block with a transaction claiming a UTXO from that transaction
 * ==> passed
 *
 * Test 25: Process multiple blocks directly on top of the genesis block, then create a block
 * ==> passed
 *
 * Test 26: Construct two branches of approximately equal size, ensuring that blocks are always created on the proper branch
 * ==> passed
 *
 * Test 27: Similar to previous test, but then try to process blocks whose parents are at height < maxHeight - CUT_OFF_AGE
 * ==> FAILED
 *
 *
 * Total:25/27 tests passed!
 */
public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private Map<ByteArrayWrapper, BlockNode> nodeMap;

    private TransactionPool txPool;

    private BlockNode currentMaxHeightNode;

    /**
     * create an empty block chain with just a genesis block.
     * Assume {@code genesisBlock} is a valid block
     */
    public BlockChain(Block genesisBlock) {
        nodeMap = new HashMap<>();
        txPool = new TransactionPool();

        UTXOPool utxoPool = new UTXOPool();
        addCoinbaseTransaction(genesisBlock, utxoPool);
        BlockNode genesisNode =
                new BlockNode(genesisBlock, null, utxoPool);
        nodeMap.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
        currentMaxHeightNode = genesisNode;
    }

    /**
     * Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        return currentMaxHeightNode.block;
    }

    /**
     * Get the UTXOPool for mining a new block on top of max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return currentMaxHeightNode.getUtxoPool();
    }

    /**
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if (block == null) {
            return false;
        }

        // return false if it is a genesis block
        if (block.getPrevBlockHash() == null) {
            return false;
        }

        //check if parent exists
        ByteArrayWrapper parentBlockId =
                new ByteArrayWrapper(block.getPrevBlockHash());
        BlockNode parentNode = nodeMap.get(parentBlockId);
        if (parentNode == null) {
            return false;
        }

        // check if the new height will exceed max height
        int height = parentNode.height + 1;

        if (height <= currentMaxHeightNode.height - CUT_OFF_AGE) {
            return false;
        }

        // check if the block already exists
        ByteArrayWrapper blockId = new ByteArrayWrapper(block.getHash());
        if (nodeMap.containsKey(blockId)) {
            return false;
        }

        // check if the block is valid
        UTXOPool utxoPool = parentNode.getUtxoPool();
        TxHandler handler = new TxHandler(utxoPool);
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);

        Transaction[] validTxs = handler.handleTxs(txs);
        if (validTxs.length != txs.length) {
            return false;
        }

        utxoPool = handler.getUTXOPool();
        addCoinbaseTransaction(block, utxoPool);

        BlockNode node = new BlockNode(block, parentNode, utxoPool);
        nodeMap.put(blockId, node);

        if (node.height > currentMaxHeightNode.height) {
            currentMaxHeightNode = node;
        }

        return true;
    }

    /**
     * Add a transaction to the transaction pool
     */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    private void addCoinbaseTransaction(Block block, UTXOPool utxoPool) {
        Transaction coinbase = block.getCoinbase();
        //txPool.addTransaction(coinbase);
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }
    }

    private class BlockNode {
        private Block block;

        private BlockNode parent;

        private UTXOPool utxoPool;

        private List<BlockNode> children;

        private int height = 1;

        public BlockNode(Block block, BlockNode parent, UTXOPool utxoPool) {
            this.block = block;
            this.parent = parent;
            this.utxoPool = utxoPool;
            this.children = new ArrayList<>();

            if (parent != null) {
                height = parent.height + 1;
                parent.addChild(this);
            }
        }

        public UTXOPool getUtxoPool() {
            return new UTXOPool(utxoPool);
        }

        public void addChild(BlockNode child) {
            children.add(child);
        }
    }
}