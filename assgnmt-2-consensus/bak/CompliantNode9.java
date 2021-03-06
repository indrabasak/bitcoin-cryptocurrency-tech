import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 81/100
 * <p/>
 *
 * @since 05/28/18
 */
public class CompliantNode9 implements Node {

    private double pGraph;

    private double pMalicious;

    private double pTxDistribution;

    private int numRounds;

    private int round;

    private boolean[] followees;

    private Set<Transaction> pendingTransactions;

    private Set<Transaction> allValidTransactions;

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
        this.pGraph = p_graph;
        this.pMalicious = p_malicious;
        this.pTxDistribution = p_txDistribution;
        this.numRounds = numRounds;

        pendingTransactions = new HashSet<>();
        allValidTransactions = new HashSet<>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        if (!pendingTransactions.isEmpty()) {
            this.pendingTransactions.addAll(pendingTransactions);
            this.allValidTransactions.addAll(pendingTransactions);
        }
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> sendTransactions;
        if (round < numRounds) {
            System.out.println("----- 1");
            sendTransactions = new HashSet<>(pendingTransactions);
            //pendingTransactions.clear();
        } else {
            System.out.println("----- 2");
            sendTransactions = new HashSet<>(allValidTransactions);
        }
        //round++;

        return sendTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        pendingTransactions.clear();
        round++;
        Map<Transaction, Set<Integer>> txToCandidateMap = new HashMap<>();
        // shortlist transactions sent from a followee
        for (Candidate c : candidates) {
            if (followees[c.sender]) {
                pendingTransactions.add(c.tx);
                allValidTransactions.add(c.tx);
            }
        }
    }
}