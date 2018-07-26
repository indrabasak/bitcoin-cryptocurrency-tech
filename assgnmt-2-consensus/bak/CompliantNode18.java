import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 0/100
 * <p/>
 * We encountered the following warnings when grading this part:
 * <p>
 * The grader ran out of memory while grading your submission. Please try
 * submitting an optimized solution. If you think your solution is correct,
 * please visit the Discussion forum to see if your peers are experiencing
 * similar errors. If the issue isn't resolved in 24 hours, please reach out
 * to Coursera through our Help Center.
 * <p/>
 *
 * @since 05/28/18
 */
public class CompliantNode18 implements Node {


    private double pGraph;

    private double pMalicious;

    private double pTxDistribution;

    private int numRounds;

    private boolean[] followees;

    private boolean[] blackListed;

    private int[] potentialMalicious;

    private Set<Transaction> pendingTransactions;

    private Set<Transaction> initialPendingTransactions;

    private int threshold;

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
        this.pGraph = p_graph;
        this.pMalicious = p_malicious;
        this.pTxDistribution = p_txDistribution;
        this.numRounds = numRounds;

        threshold = (int) p_malicious * numRounds;

        pendingTransactions = new HashSet<>();
        initialPendingTransactions = new HashSet<>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        blackListed = new boolean[followees.length];
        potentialMalicious = new int[followees.length];
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        if (!pendingTransactions.isEmpty()) {
            this.pendingTransactions.addAll(pendingTransactions);
            this.initialPendingTransactions.addAll(pendingTransactions);
        }
    }

    public Set<Transaction> sendToFollowers() {
        System.out.println("----- sendToFollowers()");
        Set<Transaction> sendTransactions;
        if (round < 2) {
            sendTransactions = new HashSet<>(initialPendingTransactions);
        } else {
            sendTransactions = new HashSet<>(pendingTransactions);
            pendingTransactions.clear();
        }

        return sendTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        round++;
        System.out.println("----- receiveFromFollowees()");

        if (round == 1) {
            Map<Integer, Set<Transaction>> followeeToTransactionMap
                    = new HashMap<>();

            for (Candidate c : candidates) {
                if (!followeeToTransactionMap.containsKey(c)) {
                    followeeToTransactionMap.put(c.sender, new HashSet<>());
                }

                followeeToTransactionMap.get(c.sender).add(c.tx);
            }

            for (int i = 0; i < followees.length; i++) {
                if (followees[i]) {
                    if (!followeeToTransactionMap.containsKey(i)) {
                        System.out.println(" 1-------- blacklisted");
                        blackListed[i] = true;
                    } else {
                        if (!followeeToTransactionMap.get(i).containsAll(initialPendingTransactions)) {
                            System.out.println(" 2-------- blacklisted");
                            blackListed[i] = true;
                        }
                    }
                }
            }
        } else {
            Set<Integer> senders =
                    candidates.stream().map(c -> c.sender).collect(toSet());
            for (int i = 0; i < followees.length; i++) {
                if (followees[i] && !senders.contains(i)) {
                    blackListed[i] = true;
                }
            }

            Map<Transaction, Set<Integer>> txToFolloweeMap = new HashMap<>();
            for (Candidate c : candidates) {
                if (!blackListed[c.sender]) {
                    pendingTransactions.add(c.tx);
//                    if (!txToFolloweeMap.containsKey(c.tx)) {
//                        txToFolloweeMap.put(c.tx, new HashSet<>());
//                    }
//
//                    if (txToFolloweeMap.get(c.tx).contains(c.sender)) {
//                        potentialMalicious[c.sender]++;
//                        if (potentialMalicious[c.sender] > threshold) {
//                            blackListed[c.sender] = true;
//                        }
//                    } else {
//                        txToFolloweeMap.get(c.tx).add(c.sender);
//                        pendingTransactions.add(c.tx);
//                    }
                }
            }
        }
    }
}
