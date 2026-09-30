package AI.Hybrid;

import AI.GameAction;
import AI.GameState;
import AI.MCTS.Rollout;
import AI.MCTS.Heuristics;
import AI.Qlearning.QTable;

import java.util.List;

public class QTableRollout extends Rollout {

    private final QTable qTable;
    private final double scale; //used for normalization

    public QTableRollout(QTable qTable) {
        this(qTable, 10.0);
    }

    public QTableRollout(QTable qTable, double scale) {
        super(Integer.MAX_VALUE, new Heuristics());
        this.qTable = qTable;
        this.scale = scale;
    }

    @Override
    public double[] simulate(GameState state, int playerForReward) {

        //if terminal use real terminal rewards
        if (state.isTerminal()) {
            return state.terminalRewards();
        }

        List<GameAction> actions = state.legalActions();

        //no legal actions returns 0
        if (actions == null || actions.isEmpty()) {
            double[] rewards = new double[state.getPlayerCount()];
            rewards[playerForReward] = 0.0;
            return rewards;
        }

        //check qtable for best action + value
        GameAction best = qTable.getBestAction(state, actions);
        double bestQ = qTable.getQValue(state, best);

        //if not root players turn bestQ is flipped (need to change this for 3+ players though)
        double v = (state.currentPlayer() == playerForReward) ? bestQ : -bestQ;

        //normalize q values between -1 and 1 for MCTS
        double normalized = Math.tanh(v / scale);

        double[] rewards = new double[state.getPlayerCount()];
        rewards[playerForReward] = normalized;
        return rewards;
    }
}

