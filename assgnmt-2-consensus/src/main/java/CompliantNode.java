import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 95
 * <p/>
 *
 * @since 05/20/18
 */
public class CompliantNode implements Node {

    private double p_graph;

    private double p_malicious;

    private double p_txDistribution;

    private int numRounds;

    private int round;

    private boolean[] followees;

    private boolean[] blackListed;

    private Map<Integer, Boolean> followeesToBlacklistedMap;

    private Set<Transaction> pendingTransactions;

    private Map<Integer, Set<Integer>> candidateToTransactionMap;

    /**
     * @param p_graph          the pairwise connectivity probability of the
     *                         random graph: e.g. {.1, .2, .3}
     * @param p_malicious      the probability that a node will be set to be
     *                         malicious: e.g {.15, .30, .45}
     * @param p_txDistribution the probability that each of the initial valid
     *                         transactions will be communicated: e.g. {.01,
     *                         .05,
     *                         .10}
     * @param numRounds        the number of rounds in the simulation e.g. {10,
     *                         20}
     */
    public CompliantNode(double p_graph, double p_malicious,
            double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;

        pendingTransactions = new HashSet<>();
        candidateToTransactionMap = new HashMap<>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        blackListed = new boolean[followees.length];
        for (int i = 0; i < followees.length; i++) {
            followeesToBlacklistedMap.put(i, followees[i]);
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions.addAll(pendingTransactions);
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> sendTransactions = new HashSet<>(pendingTransactions);
        pendingTransactions.clear();

        return sendTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        round++;

        Set<Integer> senders =
                candidates.stream().map(c -> c.sender).collect(toSet());
        for (int i = 0; i < followees.length; i++) {
            if (followees[i] && !senders.contains(i))
                blackListed[i] = true;
        }

        for (Candidate c : candidates) {
            if (!blackListed[c.sender]) {
                //pendingTransactions.add(c.tx);
                if (!candidateToTransactionMap.containsKey(c.sender)) {
                    candidateToTransactionMap.put(c.sender, new HashSet<>());
                }

                Set<Integer> transactions =
                        candidateToTransactionMap.get(c.sender);
                if (transactions.contains(c.tx.id)) {
                    blackListed[c.sender] = true;
                    System.out.println("seen before: " + c.sender + " = " + c.tx.id);
                } else {
                    pendingTransactions.add(c.tx);
                    transactions.add(c.tx.id);
                    System.out.println("not seen before: " + c.sender + " = " + c.tx.id);
                }
            }
        }
    }
}
