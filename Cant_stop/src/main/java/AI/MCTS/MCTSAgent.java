package AI.MCTS;

import AI.GameAction;
import AI.GameState;

public class MCTSAgent {

    private final MCTS mcts;
    private final int iterations;
    private final double explorationFactor;

    public MCTSAgent(int iterations, double explorationFactor, int rolloutMaxDepth) {
        this.iterations = iterations;
        this.explorationFactor = explorationFactor;

        Heuristics heuristics = new Heuristics();
        Rollout rollout = new Rollout(rolloutMaxDepth, heuristics);
        this.mcts = new MCTS(rollout);
    }

    public MCTSAgent(int iterations, double explorationFactor) {
        this(iterations, explorationFactor, Integer.MAX_VALUE);
    }

    public GameAction chooseAction(GameState rootState) {
        if (rootState == null) {
            throw new IllegalArgumentException("rootState cannot be null");
        }
        if (rootState.isTerminal()) {
            return null;
        }
        return mcts.findBestAction(rootState, iterations, explorationFactor);
    }
}
