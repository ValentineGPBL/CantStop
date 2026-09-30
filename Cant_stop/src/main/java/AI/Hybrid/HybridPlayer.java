package AI.Hybrid;

import AI.AIPlayer;
import AI.GameAction;
import AI.GameState;
import AI.MCTS.PlayerViewAdapter;
import Player.Player;

import java.util.List;

public class HybridPlayer implements AIPlayer {

    private final int playerIndex;
    private final int winTarget;
    private final HybridMCTSAgent agent;

    public HybridPlayer(int playerIndex, int winTarget, HybridMCTSAgent agent) {
        this.playerIndex = playerIndex;
        this.winTarget = winTarget;
        this.agent = agent;
    }

    public GameAction chooseAction(GameState rootState) {
        return agent.chooseAction(rootState);
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
}

