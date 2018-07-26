import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

/**
 * {@code CompliantNode} refers to a node that follows the rules (not
 * malicious).
 * <p/>
 * Score: 66/100
 * <p/>
 * Tests for this assignment involve your submitted miner competing with a number of different types of malicious miners
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 46 out of 72 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 48 out of 72 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 34 out of 58 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.1, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 38 out of 58 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.01, numRounds = 10
 * On average 58 out of 76 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.3, p_txDistribution = 0.05, numRounds = 10
 * On average 51 out of 76 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.01, numRounds = 10
 * On average 31 out of 54 of nodes reach consensus
 * <p>
 * Running test with parameters: numNodes = 100, p_graph = 0.2, p_malicious = 0.45, p_txDistribution = 0.05, numRounds = 10
 * On average 32 out of 54 of nodes reach consensus
 *
 * @since 07/23/18
 */
public class CompliantNode29 implements Node {

    private boolean[] followees;

    private boolean[] blackListed;

    private Set<Transaction> pendingTransactions;

    private Set<Transaction> currentValidTransactions;

    private Map<Integer, Set<Transaction>> previousTransactions;

    private int round;

    private UUID id;


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
        previousTransactions = new HashMap<>();
        currentValidTransactions = new HashSet<>();
        id = UUID.randomUUID();
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
        sendTransactions.addAll(currentValidTransactions);
        //pendingTransactions.clear();
        //currentValidTransactions.clear();

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

        Map<Integer, Set<Transaction>> currentTransactions = new HashMap<>();
        for (Candidate c : candidates) {
            if (followees[c.sender] && !blackListed[c.sender]) {
                if (!currentTransactions.containsKey(c.sender)) {
                    currentTransactions.put(c.sender, new HashSet<>());
                }

                if (currentTransactions.get(c.sender).contains(c.tx)) {
                    blackListed[c.sender] = true;
                } else {
                    currentTransactions.get(c.sender).add(c.tx);
                    //pendingTransactions.add(c.tx);
                }
            }
        }

        currentTransactions.forEach((sender, txs) -> {
            if (previousTransactions.containsKey(sender)) {
                Set<Transaction> oldTxs = previousTransactions.get(sender);
                // 2. constantly broadcasts its own set of transactions and
                // never accept transactions given to it.
                if (!txs.containsAll(oldTxs)) {
                    System.out.println(id + " - round: " + round + " blacklisted");
                    blackListed[sender] = true;
                    for (Transaction tx : oldTxs) {
                        currentValidTransactions.remove(tx);
                    }
                }
            }

            if (!blackListed[sender]) {
                // only add transaction from followees who are not
                // blacklisted
                System.out.println(id + " - round: " + round + " added");
                for (Transaction tx : txs) {
                    if (!pendingTransactions.contains(tx) &&
                            !currentTransactions.containsKey(tx)) {
                        currentValidTransactions.add(tx);
                    }
                }
            }
            previousTransactions.put(sender, txs);
        });
    }

}