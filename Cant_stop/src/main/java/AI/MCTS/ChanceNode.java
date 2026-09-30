package AI.MCTS;

import AI.GameState;

import java.util.*;

import static java.lang.Math.pow;

public class ChanceNode {
    private final GameState parentState;
    private Map<GameState, Double> outcomes;
    //compute prob
    private static final Map<List<Integer>, Double> probs =Probabilities();

    public ChanceNode(GameState parentState) {
        this.parentState = parentState;
        this.outcomes = new HashMap<>();
    }

    /**
     * runs only one time to calculate proba of dices pairings
     * @return dice sums to proba map
     */
    private static Map<List<Integer>, Double> Probabilities() {
        Map<List<Integer>, Integer> pairingCounts = new HashMap<>();
        int numDices = 4;
        double totalPossibleRolls = pow(6, numDices);

        // Dice rolls are 1-6. This loop calculates the sums correctly.
        for (int d1 = 1; d1 <= 6; d1++) {
            for (int d2 = 1; d2 <= 6; d2++) {
                for (int d3 = 1; d3 <= 6; d3++) {
                    for (int d4 = 1; d4 <= 6; d4++) {
                        int sumA = d1 + d2;
                        int sumB = d3 + d4;
                        addToPair(pairingCounts, sumA, sumB);

                        int sumC = d1 + d3;
                        int sumD = d2 + d4;
                        addToPair(pairingCounts, sumC, sumD);

                        int sumE = d1 + d4;
                        int sumF = d2 + d3;
                        addToPair(pairingCounts, sumE, sumF);
                    }
                }
            }
        }

        Map<List<Integer>, Double> probabilities = new HashMap<>();
        // Each of the 1296 rolls (6^4) produces 3 pairings
        double totalPairings = totalPossibleRolls * 3;

        for (Map.Entry<List<Integer>, Integer> entry : pairingCounts.entrySet()) {
            double prob = entry.getValue() / totalPairings;
            probabilities.put(entry.getKey(), prob);
        }
        return Collections.unmodifiableMap(probabilities);
    }

    /**
     * Simulate the selection of a random GameState
     * @return a GameState
     */
    public GameState sampleOutcome() {
        Random r = new Random();
        int[] dice = {r.nextInt(6)+1, r.nextInt(6)+1, r.nextInt(6)+1, r.nextInt(6)+1};
        List<int[]> p = List.of(
                new int[]{dice[0]+dice[1], dice[2]+dice[3]},
                new int[]{dice[0]+dice[2], dice[1]+dice[3]},
                new int[]{dice[0]+dice[3], dice[1]+dice[2]}
        );
        return parentState.withPairingsAndDice(p, dice);
    }

    /**
     * generates all possibles pairing of dice adding it to game state with their prob
     */
    private void generateOutcomes() {

        for (Map.Entry<List<Integer>, Double> entry : probs.entrySet()) {
            List<Integer> sums = entry.getKey();
            double prob = entry.getValue();

            List<int[]> newPairings = new ArrayList<>();
            newPairings.add(new int[]{sums.get(0), sums.get(1)});

            GameState outcome = parentState.withPairings(newPairings);

            this.outcomes.put(outcome, prob);
        }
    }


    private static void addToPair(Map<List<Integer>, Integer> Counts, int sumA, int sumB) {
        List<Integer> key = new ArrayList<>(Arrays.asList(sumA, sumB));
        Collections.sort(key);
        Counts.put(key, Counts.getOrDefault(key, 0) + 1);
    }

    public Map<GameState, Double> getOutcomes() {
        if (this.outcomes == null || this.outcomes.isEmpty()) {
            this.outcomes = new HashMap<>();
            for (Map.Entry<List<Integer>, Double> entry : probs.entrySet()) {
                List<Integer> sums = entry.getKey();
                List<int[]> p = List.of(new int[]{sums.get(0), sums.get(1)});
                this.outcomes.put(parentState.withPairings(p), entry.getValue());
            }
        }
        return outcomes;
    }
}