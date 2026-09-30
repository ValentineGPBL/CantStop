package AI.MCTS;

import AI.GameState;
import Player.Player;
import java.util.List;
import java.util.Map;

public class PlayerViewAdapter implements GameState.PlayerView {

    private final Player player;

    @Override
    public Map<Integer, Integer> currentTempMarkers() {
        // You need to pass the current Turn's markers here
        // Or if the Player object doesn't have them, pass them from GameView
        return Map.of(); // Update this to return the actual temporary progress
    }

    public PlayerViewAdapter(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
        this.player = player;
    }

    @Override
    public List<Integer> permanentSteps() {
        return player.getPermanentStepPositions();
    }

    @Override
    public List<Integer> claimedSums() {
        return player.getColumnsClaimed();
    }
}
