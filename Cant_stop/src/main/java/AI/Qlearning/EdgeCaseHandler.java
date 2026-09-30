package AI.Qlearning;

import AI.GameState;

import java.util.Arrays;

public class EdgeCaseHandler {
    public static boolean didBust(GameState beforeRoll, GameState afterRoll) {
        // A bust occurs when the turn passes to the next player, the previous player's permanent
        // board did not change (no commit), and the new state's temporary markers are all zero.
        if (beforeRoll.currentPlayer() == afterRoll.currentPlayer()) return false;
        int beforePlayer = beforeRoll.currentPlayer();
        int[] beforePerm = beforeRoll.getPerm()[beforePlayer];
        int[] afterPerm = afterRoll.getPerm()[beforePlayer];
        boolean tempEmpty = isTempEmpty(afterRoll);
        return Arrays.equals(beforePerm, afterPerm) && tempEmpty;
    }

    private static boolean isTempEmpty(GameState state) {
        int[] temp = state.getTemp();
        for (int t : temp) if (t > 0) return false;
        return true;
    }




}
