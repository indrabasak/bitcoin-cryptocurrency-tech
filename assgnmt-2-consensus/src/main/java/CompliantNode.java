import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 100/100
 * <p/>
 * Tests for this assignment involve your submitted miner competing with a number of different types of malicious miners
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 72 out of 72 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 72 out of 72 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 58 out of 58 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 58 out of 58 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 76 out of 76 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 76 out of 76 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 54 out of 54 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 54 out of 54 of nodes reach consensus
 *
 * @since 07/25/18
 */
public class CompliantNode implements Node {

    private static final int NUM_OF_TRUST_ROUNDS = 2;

    private boolean[] followees;

    private int[] followeesScore;

    private Set<Transaction> pendingTransactions;

    private Transaction markerTxn;

    private int round;


    /**
     * @param p_graph          the pairwise connectivity probability of the
     *                         random graph: e.g. {.1, .2, .3}
     * @param p_malicious      the probability that a node will be set to be
     *                         malicious: e.g {.15, .30, .45}
     * @param p_txDistribution the probability that each of the initial valid
     *                         transactions will be communicated: e.g. {.01,
     *                         .05, .10}
     * @param numRounds        the number of rounds in the simulation e.g. {10,
     *                         20}
     */
    public CompliantNode(double p_graph, double p_malicious,
                         double p_txDistribution, int numRounds) {
        pendingTransactions = new HashSet<>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        followeesScore = new int[followees.length];
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        if (!pendingTransactions.isEmpty()) {
            this.pendingTransactions.clear();
            this.pendingTransactions.addAll(pendingTransactions);
            Transaction[] txns = pendingTransactions.toArray(new Transaction[0]);
            if (txns.length > 0) {
                markerTxn = txns[0];
            }
        }
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> sendTransactions = new HashSet<>();
        if (round > NUM_OF_TRUST_ROUNDS) {
            sendTransactions.addAll(pendingTransactions);
            pendingTransactions.clear();
        } else {
            // create trust by sending only 1 transaction as long
            // as the round is less than equal to the number of trusted rounds
            if (markerTxn != null) {
                sendTransactions.add(markerTxn);
            }
        }

        return sendTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        round++;

        Map<Integer, Set<Transaction>> senderToTxMap = new HashMap<>();
        for (Candidate c : candidates) {
            if (followees[c.sender]) {
                if (!senderToTxMap.containsKey(c.sender)) {
                    senderToTxMap.put(c.sender, new HashSet<>());
                }

                senderToTxMap.get(c.sender).add(c.tx);
            }
        }

        // if the round is less than equal to the number of trusted rounds
        // and the followee sends ony one transaction, increment
        // the followees' score count by one.
        // it's secret handshake
        if (round <= NUM_OF_TRUST_ROUNDS) {
            for (int i = 0; i < followees.length; i++) {
                if (followees[i]) {
                    if (senderToTxMap.containsKey(i)) {
                        Set<Transaction> txns = senderToTxMap.get(i);
                        if (txns.size() == 1) {
                            followeesScore[i]++;
                        }
                    }
                }
            }
        }

        // if the present round is greater than the number of trusted rounds,
        // consider the transactions from followees who have a followee
        // score count equal to the number of trusted rounds
        if (round > NUM_OF_TRUST_ROUNDS) {
            for (int i = 0; i < followees.length; i++) {
                if (followees[i] && followeesScore[i] == NUM_OF_TRUST_ROUNDS) {
                    if (senderToTxMap.containsKey(i)) {
                        pendingTransactions.addAll(senderToTxMap.get(i));
                    }
                }
            }
        }
    }
}