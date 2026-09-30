package AI.MCTS;

import AI.GameAction;
import AI.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Rollout {

    private final Random random = new Random();
    private final int maxDepth;
    private final Heuristics heuristics;

    public Rollout(int maxDepth, Heuristics heuristics) {
        this.maxDepth = maxDepth;
        this.heuristics = heuristics;
    }

    // constructor for "infinite" depth
    public Rollout(Heuristics heuristics) {
        this(Integer.MAX_VALUE, heuristics);

    }

    public double[] simulate(GameState state, int playerForReward) {
        GameState currentState = state.clone();

        int depth = 0;
        while (!currentState.isTerminal() && depth < this.maxDepth) {
            List<GameAction> legalActions = currentState.legalActions();
            if (legalActions.isEmpty()) {
                break;
            }

            GameAction randomAction = legalActions.get(random.nextInt(legalActions.size()));

            if (randomAction instanceof GameAction.Roll) {
                int[] dice = new int[4];
                for (int i = 0; i < 4; i++) dice[i] = random.nextInt(6) + 1;

                List<int[]> pairings = new ArrayList<>();
                pairings.add(new int[]{dice[0] + dice[1], dice[2] + dice[3]});
                pairings.add(new int[]{dice[0] + dice[2], dice[1] + dice[3]});
                pairings.add(new int[]{dice[0] + dice[3], dice[1] + dice[2]});

                currentState = currentState.withPairingsAndDice(pairings, dice);
            } else {
                currentState = currentState.apply(randomAction);
            }
            depth++;
        }

        if (!currentState.isTerminal()) {
            double rawHeuristicScore = heuristics.evaluateGameState(currentState);
            double normalizedValue = Math.tanh(rawHeuristicScore / 10.0);
            double[] heuristicRewards = new double[currentState.getPlayerCount()];
            heuristicRewards[playerForReward] = normalizedValue;
            return heuristicRewards;
        } else {
            return currentState.terminalRewards();
        }
    }

    /**
     * Estimates score thanks to column claimed
     * @return A score between -1.0 and 1.0.
     */
    private double evaluateHeuristic(GameState state, int playerForReward) {
       // estimate with current claimed columns
        int[] claimedCounts = new int[state.getPlayerCount()];
        for (int owner : state.getClaimedBy()) {
            if (owner != -1) {
                claimedCounts[owner]++;
            }
        }

        double myScore = claimedCounts[playerForReward];
        double opponentScore = 0;
        for(int i = 0; i < state.getPlayerCount(); i++) {
            if (i != playerForReward) {
                opponentScore = Math.max(opponentScore, claimedCounts[i]);
            }
        }

        // normalize score
        return (myScore - opponentScore) / state.getWinTarget();
    }
}