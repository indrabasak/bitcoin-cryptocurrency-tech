import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 74/100
 * <p/>
 * Tests for this assignment involve your submitted miner competing with a
 * number of different types of malicious miners
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 56 out of 72 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 43 out of 72 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 42 out of 58 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 40 out of 58 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 66 out of 76 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 51 out of 76 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 42 out of 54 of nodes reach consensus
 *
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 37 out of 54 of nodes reach consensus
 *
 * @since 07/23/18
 */
public class CompliantNode implements Node {

    private int numRounds;

    private boolean[] followees;

    private boolean[] blackListed;

    private Set<Transaction> pendingTransactions;

    private Map<Integer, Set<Integer>> previousTransactions;

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
        previousTransactions = new HashMap<>();
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

        Set<Integer> senders =
                candidates.stream().map(c -> c.sender).collect(toSet());
        for (int i = 0; i < followees.length; i++) {
            // 1. be functionally dead and never actually
            // broadcast any transactions
            if (followees[i] && !senders.contains(i)) {
                blackListed[i] = true;
            }
        }

        /*
        this.pendingTransactions = candidates.stream()
                .filter(candidate -> !this.blackList.contains(candidate.sender))
                .map(candidate -> candidate.tx)
                .collect(toSet());
         */

        Map<Integer, Set<Integer>> currentTransactions = new HashMap<>();
        for (Candidate c : candidates) {
            if (followees[c.sender] && !blackListed[c.sender]) {
                if (!currentTransactions.containsKey(c.sender)) {
                    currentTransactions.put(c.sender, new HashSet<>());
                }

                if (currentTransactions.get(c.sender).contains(c.tx.id)) {
                    blackListed[c.sender] = true;
                } else {
                    currentTransactions.get(c.sender).add(c.tx.id);
                    pendingTransactions.add(c.tx);
                }
            }
        }

        // check for equilibrium, i.e., reached consensus
        boolean consensus = true;
        int prevCount = 0;
        int count = 0;
        for (Set<Integer> txs : currentTransactions.values()) {
            if (count == 0) {
                prevCount = txs.size();
            } else {
                int currCount = txs.size();
                if (currCount != prevCount) {
                    consensus = false;
                    break;
                }
                prevCount = currCount;

            }
            count++;
        }

        if (!consensus) {
            currentTransactions.forEach((sender, txs) -> {
                //System.out.println(" sender " + sender + " current tx size: " + txs.size());
                if (previousTransactions.containsKey(sender)) {
                    Set<Integer> oldTxs = previousTransactions.get(sender);
                    //System.out.println(" sender " + sender + " prev tx size: " + oldTxs.size());
                    // 2. constantly broadcasts its own set of transactions and
                    // never accept transactions given to it.
                    if (round < numRounds) {
                        if (oldTxs.equals(txs)) {
                            blackListed[sender] = true;
                        }
                    }
                }
                previousTransactions.put(sender, txs);
            });
        }

        //previousTransactions = currentTransactions;
    }
}