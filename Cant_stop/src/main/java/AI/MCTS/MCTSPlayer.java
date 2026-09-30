package AI.MCTS;

import AI.GameAction;
import AI.GameState;
import AI.AIPlayer;
import Player.Player;

import java.util.List;

public class MCTSPlayer implements AIPlayer {

    private final int playerIndex; // index in GameView.players list
    private final int winTarget;
    private final MCTSAgent agent;

    public MCTSPlayer(int playerIndex, int winTarget, MCTSAgent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("agent cannot be null");
        }
        if (playerIndex < 0) {
            throw new IllegalArgumentException("playerIndex must be >= 0");
        }
        this.playerIndex = playerIndex;
        this.winTarget = winTarget;
        this.agent = agent;
    }

    public GameAction chooseAction(GameState state) {
        return agent.chooseAction(state);
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
}
