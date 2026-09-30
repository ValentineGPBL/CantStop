package AI.Hybrid;

import AI.GameAction;
import AI.GameState;
import AI.MCTS.MCTS;
import AI.Qlearning.QTable;

public class HybridMCTSAgent {

    private final MCTS mcts;
    private final int iterations;
    private final double explorationFactor;

    public HybridMCTSAgent(QTable qTable, int iterations, double explorationFactor) {
        this.iterations = iterations;
        this.explorationFactor = explorationFactor;
        qTable.loadFromFile("can't_stop_data.dat");
        QTableRollout rollout = new QTableRollout(qTable);
        this.mcts = new MCTS(rollout);
    }

    public GameAction chooseAction(GameState rootState) {
        if (rootState == null || rootState.isTerminal()) return null;
        return mcts.findBestAction(rootState, iterations, explorationFactor);
    }
}

