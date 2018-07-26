import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 31/100
 * <p/>
 * Tests for this assignment involve your submitted miner competing with a number of different types of malicious miners
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 28 out of 72 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 30 out of 72 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 15 out of 58 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 16 out of 58 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 15 out of 76 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 31 out of 76 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 5 out of 54 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 15 out of 54 of nodes reach consensus
 *
 * @since 07/23/18
 */
public class CompliantNode32 implements Node {

    private int numRounds;

    private boolean[] followees;

    private boolean[] blackListed;

    private Set<Transaction> pendingTransactions;

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
        round++;
        int followeesCount = 0;

        Set<Integer> senders =
                candidates.stream().map(c -> c.sender).collect(toSet());
        for (int i = 0; i < followees.length; i++) {
            // 1. be functionally dead and never actually
            // broadcast any transactions
            if (followees[i]) {
                if (!senders.contains(i)) {
                    blackListed[i] = true;
                } else {
                    followeesCount++;
                }
            }
        }

        Map<Transaction, Integer> txnToSenderMap = new HashMap<>();
        for (Candidate c : candidates) {
            if (followees[c.sender] && !blackListed[c.sender]) {
                if (!txnToSenderMap.containsKey(c.tx)) {
                    txnToSenderMap.put(c.tx, 1);
                } else {
                    int count = txnToSenderMap.get(c.tx);
                    count++;
                    txnToSenderMap.put(c.tx, count);
                }
            }
        }

        double threshold = Math.floor((float) (round * followeesCount)/numRounds);

        txnToSenderMap.forEach((tx, senderCount) -> {
            if (senderCount >= threshold) {
                pendingTransactions.add(tx);
            }
        });

        System.out.println(String.format("round: %d, num. of rounds: %d, " +
                        "followees: %d, threshold: %f", round, numRounds,
                followeesCount, threshold));
    }
}