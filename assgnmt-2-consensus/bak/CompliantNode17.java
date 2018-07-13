import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 90/100
 * <p/>
 *
 * @since 05/28/18
 */
public class CompliantNode17 implements Node {


    private double pGraph;

    private double pMalicious;

    private double pTxDistribution;

    private int numRounds;

    private boolean[] followees;

    private boolean[] blackListed;

    private Set<Transaction> pendingTransactions;


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
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        blackListed = new boolean[followees.length];
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        if (!pendingTransactions.isEmpty()) {
            this.pendingTransactions.addAll(pendingTransactions);
        }
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> sendTransactions = new HashSet<>(pendingTransactions);
        pendingTransactions.clear();

        return sendTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        Set<Integer> senders =
                candidates.stream().map(c -> c.sender).collect(toSet());
        for (int i = 0; i < followees.length; i++) {
            if (followees[i] && !senders.contains(i))
                blackListed[i] = true;
        }

        Map<Transaction, Set<Integer>> txToFolloweeMap = new HashMap<>();
        for (Candidate c : candidates) {
            if (!blackListed[c.sender]) {
                if (!txToFolloweeMap.containsKey(c.tx)) {
                    txToFolloweeMap.put(c.tx, new HashSet<>());
                }

                if (txToFolloweeMap.get(c.tx).contains(c.sender)) {
                    blackListed[c.sender] = true;
                } else {
                    txToFolloweeMap.get(c.tx).add(c.sender);
                    pendingTransactions.add(c.tx);
                }
            }
        }
    }
}