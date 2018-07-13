import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 80/100
 * <p/>
 *
 * @since 05/28/18
 */
public class CompliantNode14 implements Node {

    private double pGraph;

    private double pMalicious;

    private double pTxDistribution;

    private int numRounds;

    private int round;

    private boolean[] followees;

    private Set<Transaction> pendingTransactions;

    private Set<Transaction> allValidTransactions;

    private Map<Transaction, Set<Integer>> txToCandidateMap;

    int numberOfMaliciousFollowees;

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
        txToCandidateMap = new ConcurrentHashMap<>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;

        int size = 0;
        for (int i = 0; i < followees.length; i++) {
            if (followees[i]) {
                size++;
            }
        }

        // number of potential malicious followees
        numberOfMaliciousFollowees = (int) (pMalicious * size);
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        //add transaction if it's not seen before
        pendingTransactions.forEach(t -> {
            if (!allValidTransactions.contains(t)) {
                allValidTransactions.addAll(pendingTransactions);
                this.pendingTransactions.addAll(pendingTransactions);
            }
        });
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> sendTransactions;
        if (round < numRounds) {
            sendTransactions = new HashSet<>(pendingTransactions);
            pendingTransactions.clear();
        } else {
            sendTransactions = new HashSet<>(allValidTransactions);
        }

        return sendTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        round++;

        // shortlist transactions sent from a followee
        for (Candidate c : candidates) {
            if (followees[c.sender]) {
                if (!txToCandidateMap.containsKey(c.tx)) {
                    txToCandidateMap.put(c.tx, new HashSet<>());
                }

                if (!txToCandidateMap.get(c.tx).contains(c.sender)) {
                    pendingTransactions.add(c.tx);
                    allValidTransactions.add(c.tx);
                } else {
                    txToCandidateMap.get(c.tx).add(c.sender);
                }
            }
        }

        // add transactions to pending transactions list which
        // meets the required threshold, i.e., more than the probable
        // malicious followees
        txToCandidateMap.forEach((k, v) -> {
            if (v.size() > numberOfMaliciousFollowees) {
                pendingTransactions.add(k);
                allValidTransactions.add(k);
                txToCandidateMap.remove(k);
            }
        });
    }
}