import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 91/100
 * <p/>
 * Tests for this assignment involve your submitted miner competing with a number of different types of malicious miners
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 65 out of 72 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 72 out of 72 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 44 out of 58 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 56 out of 58 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 65 out of 76 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 73 out of 76 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 43 out of 54 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 49 out of 54 of nodes reach consensus
 *
 * @since 07/25/18
 */
public class CompliantNode35 implements Node {

    private int numRounds;

    private boolean[] followees;

    private boolean[] blackListed;

    private Set<Transaction> originalProposals;

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
        originalProposals = new HashSet<>();
        pendingTransactions = new HashSet<>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        blackListed = new boolean[followees.length];
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        originalProposals.clear();
        if (!pendingTransactions.isEmpty()) {
            originalProposals.addAll(pendingTransactions);
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

        Set<Integer> senders =
                candidates.stream().map(c -> c.sender).collect(toSet());
        for (int i = 0; i < followees.length; i++) {
            // 1. be functionally dead and never actually
            // broadcast any transactions
            if (followees[i] && !senders.contains(i)) {
                blackListed[i] = true;
            }
        }

        Map<Transaction, Integer> txnFrequencyMap = new HashMap<>();
        Set<Integer> followeesSet = new HashSet<>();
        Map<Integer, Set<Transaction>> senderToTxMap = new HashMap<>();
        for (Candidate c : candidates) {
            if (followees[c.sender] && !blackListed[c.sender]) {
                followeesSet.add(c.sender);

                if (!txnFrequencyMap.containsKey(c.tx)) {
                    txnFrequencyMap.put(c.tx, 1);
                } else {
                    int count = txnFrequencyMap.get(c.tx);
                    txnFrequencyMap.put(c.tx, count + 1);
                }

                if (!senderToTxMap.containsKey(c.sender)) {
                    senderToTxMap.put(c.sender, new HashSet<>());
                }

                senderToTxMap.get(c.sender).add(c.tx);
            }
        }

        if (round < numRounds - 2) {
            txnFrequencyMap.forEach((tx, count) -> {
                pendingTransactions.add(tx);

            });
        } else {
//            final int fCount = followeesSet.size();
//            txnFrequencyMap.forEach((tx, count) -> {
//                if (count > 0.2 * fCount) {
//                    pendingTransactions.add(tx);
//                }
//
//            });

            senderToTxMap.forEach((sender, txs) -> {
                if (txs.containsAll(originalProposals)) {
                    pendingTransactions.addAll(txs);
                }
            });
        }
    }
}