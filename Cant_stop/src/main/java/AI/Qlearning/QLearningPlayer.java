package AI.Qlearning;

import AI.AIPlayer;
import AI.GameAction;
import AI.GameState;
import AI.MCTS.PlayerViewAdapter;
import Player.Player;

import java.util.List;

public class QLearningPlayer implements AIPlayer {
    private final int playerIndex;
    private final int winTarget;
    private final QLearningAgent agent;

    public QLearningPlayer(int playerIndex, int winTarget) {
        this.playerIndex = playerIndex;
        this.winTarget = winTarget;
        QTable qTable = new QTable();
        // Try to load pre-trained data if available
        qTable.loadFromFile("cant_stop.dat");
        
        this.agent = new QLearningAgent(qTable);
        this.agent.setEpsilon(0.0); // Greedy
    }

    @Override
    public GameAction chooseAction(GameState state) {
                return agent.chooseAction(state);
    }

    @Override
    public int getPlayerIndex() {
        return playerIndex;
    }
}
