import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Simulation2 {

    /**
     * @param p_graph          parameter for random graph: prob. that an edge will exist
     * @param p_malicious      prob. that a node will be set to be malicious
     * @param p_txDistribution probability of assigning an initial transaction to each node
     * @param numRounds        number of simulation rounds your nodes will run for
     */
    public static void simulate(double p_graph, double p_malicious,
                                double p_txDistribution, int numRounds) {

        // There are four required command line arguments: p_graph (.1, .2, .3),
        // p_malicious (.15, .30, .45), p_txDistribution (.01, .05, .10),
        // and numRounds (10, 20). You should try to test your CompliantNode
        // code for all 3x3x3x2 = 54 combinations.

        int numNodes = 100;
        System.out.println("Running test with parameters: numNodes = " + numNodes
                + ", p_graph = " + p_graph
                + ", p_malicious = " + p_malicious
                + ", p_txDistribution = " + p_txDistribution
                + ", numRounds = " + numRounds);

        // pick which nodes are malicious and which are compliant
        Node[] nodes = new Node[numNodes];
        for (int i = 0; i < numNodes; i++) {
            if (Math.random() < p_malicious)
                // When you are ready to try testing with malicious nodes, replace the
                // instantiation below with an instantiation of a MaliciousNode
                nodes[i] =
                        new MaliciousNode(p_graph, p_malicious, p_txDistribution,
                                numRounds);
            else
                nodes[i] = new CompliantNode(p_graph, p_malicious,
                        p_txDistribution, numRounds);
        }


        // initialize random follow graph
        boolean[][] followees =
                new boolean[numNodes][numNodes]; // followees[i][j] is true iff i follows j
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                if (i == j) continue;
                if (Math.random() < p_graph) { // p_graph is .1, .2, or .3
                    followees[i][j] = true;
                }
            }
        }

        // notify all nodes of their followees
        for (int i = 0; i < numNodes; i++)
            nodes[i].setFollowees(followees[i]);

        // initialize a set of 500 valid Transactions with random ids
        int numTx = 500;
        HashSet<Integer> validTxIds = new HashSet<Integer>();
        Random random = new Random();
        for (int i = 0; i < numTx; i++) {
            int r = random.nextInt();
            validTxIds.add(r);
        }


        // distribute the 500 Transactions throughout the nodes, to initialize
        // the starting state of Transactions each node has heard. The distribution
        // is random with probability p_txDistribution for each Transaction-Node pair.
        for (int i = 0; i < numNodes; i++) {
            HashSet<Transaction> pendingTransactions =
                    new HashSet<Transaction>();
            for (Integer txID : validTxIds) {
                if (Math.random() < p_txDistribution) // p_txDistribution is .01, .05, or .10.
                    pendingTransactions.add(new Transaction(txID));
            }
            nodes[i].setPendingTransaction(pendingTransactions);
        }


        // Simulate for numRounds times
        for (int round =
             0; round < numRounds; round++) { // numRounds is either 10 or 20

            // gather all the proposals into a map. The key is the index of the node receiving
            // proposals. The value is an ArrayList containing 1x2 Integer arrays. The first
            // element of each array is the id of the transaction being proposed and the second
            // element is the index # of the node proposing the transaction.
            HashMap<Integer, Set<Candidate>> allProposals = new HashMap<>();

            for (int i = 0; i < numNodes; i++) {
                Set<Transaction> proposals = nodes[i].sendToFollowers();
                for (Transaction tx : proposals) {
                    if (!validTxIds.contains(tx.id))
                        continue; // ensure that each tx is actually valid

                    for (int j = 0; j < numNodes; j++) {
                        if (!followees[j][i])
                            continue; // tx only matters if j follows i

                        if (!allProposals.containsKey(j)) {
                            Set<Candidate> candidates = new HashSet<>();
                            allProposals.put(j, candidates);
                        }

                        Candidate candidate = new Candidate(tx, i);
                        allProposals.get(j).add(candidate);
                    }

                }
            }

            // Distribute the Proposals to their intended recipients as Candidates
            for (int i = 0; i < numNodes; i++) {
                if (allProposals.containsKey(i))
                    nodes[i].receiveFromFollowees(allProposals.get(i));
            }
        }

        // print results

        int consensus = 0;
        for (int i = 0; i < numNodes; i++) {
            Set<Transaction> transactions = nodes[i].sendToFollowers();
            if (transactions.size() == validTxIds.size()) {
                consensus++;
            }
            System.out.println(
                    "Node " + i + " believes consensus on " + transactions.size() + " transactions");

//            System.out.println(
//                    "Transaction ids that Node " + i + " believes consensus on:");
//            for (Transaction tx : transactions) {
//                System.out.println(tx.id);
//            }
//            System.out.println();
//            System.out.println();
        }

        int average = 0;
        System.out.println("On average " + consensus + " out of " +  "  of nodes reach consensus");

    }

    /*
    Tests for this assignment involve your submitted miner competing with a number of different types of malicious miners



Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
On average 57 out of 72 of nodes reach consensus

Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
On average 32 out of 58 of nodes reach consensus

Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
On average 42 out of 58 of nodes reach consensus

Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
On average 67 out of 76 of nodes reach consensus

Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
On average 74 out of 76 of nodes reach consensus

Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
On average 41 out of 54 of nodes reach consensus

Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
On average 49 out of 54 of nodes reach consensus

     */
    public static void main(String[] args) {
        simulate(0.1, 0.3, 0.01, 10);
    }
}
