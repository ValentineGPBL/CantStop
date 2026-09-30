package AI.MCTS;

import AI.GameState;
import Conditions.BustCondition;
import Conditions.BustCondition.BustResult;

import java.util.Map;
import java.util.Set;

public class Heuristics {

    private final BustCondition bustCondition = new BustCondition();

    //weights
    private static final double W_PROGRESS  = 1.0;
    private static final double W_BUST      = 1.3;
    private static final double W_OPPONENT  = 0.9;
    private static final double W_GREED     = 0.6;
    private static final double W_WINPRESS  = 1.2;

    public double evaluateGameState(GameState state) {
        double score = 0.0;

        score += W_PROGRESS * calculateColumnProgress(state);
        score -= W_BUST * calculateBustRisk(state);
        score -= W_OPPONENT * calculateOpponentProgress(state);
        score -= W_GREED * calculateGreedFactor(state);
        score += W_WINPRESS * calculateWinPressure(state);

        return score;
    }

    private double calculateColumnProgress(GameState state) {
        double focusScore = 0;
        int[] temp = state.getTemp();
        int[][] perm = state.getPerm();
        int currentPlayer = state.currentPlayer();
        int[] heights = {3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3};

        for (int i = 0; i < temp.length; i++) {
            int totalSteps = temp[i] + perm[currentPlayer][i];
            if (totalSteps > 0) {
                double completionRatio = (double) totalSteps / heights[i];

                //prioritize finishing columns
                focusScore += Math.pow(completionRatio, 2) * 10;
                //claiming the column
                if (totalSteps >= heights[i]) {
                    focusScore += 25;
                }
            }
        }
        return focusScore;
    }

    private double calculateBustRisk(GameState state) {
        int[] dice = state.getLastDiceRoll();
        if (dice == null) {
            return 0;
        }

        Map<Integer, Integer> columnRemaining = state.getColumnRemainingSteps();
        Set<Integer> claimedColumns = state.getClaimedColumns();
        Map<Integer, Integer> tempMarkers = state.getTempMarkers();
        boolean[] activeCols = state.getActiveColumns();

        BustResult bustResult = bustCondition.evaluate(dice, columnRemaining, claimedColumns, tempMarkers, activeCols);

        double rushFactor = calculateRushFactor(state);

        return bustResult.busted ? (10.0 / rushFactor) : 0;
    }

    private double calculateOpponentProgress(GameState state) {
        double threatPenalty = 0.0;
        int currentPlayer = state.currentPlayer();
        int[][] perm = state.getPerm();
        int[] temp = state.getTemp();
        int[] heights = {3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3};

        for (int i = 0; i < heights.length; i++) {
            int colHeight = heights[i];

            int maxOpponentSteps = 0;
            for (int p = 0; p < state.getPlayerCount(); p++) {
                if (p == currentPlayer) continue;
                maxOpponentSteps = Math.max(maxOpponentSteps, perm[p][i]);
            }

            double opponentRatio = (double) maxOpponentSteps / colHeight;

            int myTotalSteps = perm[currentPlayer][i] + temp[i];
            double myRatio = (double) myTotalSteps / colHeight;

            if (myTotalSteps > 0 && (colHeight - maxOpponentSteps) <= 2) {
                threatPenalty += (opponentRatio * myRatio) * 10;
            }
        }
        return threatPenalty;
    }

    private double calculateGreedFactor(GameState state) {
        int runnersUsed = 0;
        boolean[] active = state.getActiveColumns();
        for (boolean b : active) if (b) runnersUsed++;

        int totalStepsThisTurn = 0;
        for (int s : state.getTemp()) totalStepsThisTurn += s;

        int deadColumns = 0;
        int[] claimedBy = state.getClaimedBy();
        for (int owner : claimedBy) if (owner != -1) deadColumns++;

        //metric for how late into game
        double lateGameFactor = (deadColumns >= 6) ? 0.4 : (deadColumns >= 4 ? 0.7 : 1.0);

        //greed is scaled down
        if (runnersUsed < 3 && totalStepsThisTurn < 3) {
            double base = 30.0;
            return base * lateGameFactor;
        }

        //if progression this turn is high, stop
        if (totalStepsThisTurn >= 6) {
            return 0;
        }

        return 10.0 * lateGameFactor;
    }

    //if close to winning value rises, if opponent close value drops
    private double calculateWinPressure(GameState state) {
        int me = state.currentPlayer();
        int[] claimedBy = state.getClaimedBy();

        int myClaimed = 0;
        int bestOpponentClaimed = 0;

        int[] counts = new int[state.getPlayerCount()];
        for (int owner : claimedBy) {
            if (owner >= 0) counts[owner]++;
        }

        myClaimed = counts[me];
        for (int p = 0; p < counts.length; p++) {
            if (p == me) continue;
            bestOpponentClaimed = Math.max(bestOpponentClaimed, counts[p]);
        }

        return (myClaimed - bestOpponentClaimed) * 25.0;
    }

    public double calculateRushFactor(GameState state) {
        int rushPenalty = 0;
        int[][] perm = state.getPerm();
        int currentPlayer = state.currentPlayer();
        int[] heights = {3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3};
        int[] colClaimedPerPlayer = new int[state.getPlayerCount()];

        for (int i = 0; i < heights.length; i++) {
            int colHeight = heights[i];

            for (int p = 0; p < state.getPlayerCount(); p++) {
                if (p == currentPlayer) continue;
                if (colHeight == perm[p][i]) {
                    colClaimedPerPlayer[p]++;
                }
            }
        }

        for (int j : colClaimedPerPlayer) {
            if (j >= 1) rushPenalty += 3;
        }
        return rushPenalty == 0 ? 1 : rushPenalty;
    }
}
