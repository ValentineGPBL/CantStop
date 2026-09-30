package AI.Qlearning;

import AI.GameState;

public class Rewards {
    public static final double BUST_PENALTY = -5000.0;           // Reduced bust penalty
    public static final double SECURE_COLUMN_BONUS = 5000.0;      // Increased for completing columns
    public static final double SAVE_PROGRESS_BONUS = 50.0;       // Changed from negative to positive
    public static final double STOP_BONUS = 100.0;               // Reward for making safe stops
    public static final double WIN_BONUS = 5000.0;               // Increased win reward
    public static final double STEP_PENALTY = -100.0;            // Reduced per-step penalty
    public static final double CONSECUTIVE_ROLL_BONUS = 20.0;    // Bonus per consecutive successful roll
    
    /**
     * Generate a filename-safe string representation of current reward configuration
     * Example: "B-5000_SC5000_SP50_SB100_W5000_P-100_CR20"
     */
    public static String getRewardConfigString() {
        return String.format("B%.0f_SC%.0f_SP%.0f_SB%.0f_W%.0f_P%.0f_CR%.0f",
            BUST_PENALTY,
            SECURE_COLUMN_BONUS,
            SAVE_PROGRESS_BONUS,
            STOP_BONUS,
            WIN_BONUS,
            STEP_PENALTY,
            CONSECUTIVE_ROLL_BONUS
        );
    }

    public static double calculateReward(GameState oldState, GameState newState, boolean busted) {
        double reward = 0.0;

        if (busted) {
            reward += BUST_PENALTY;
        } else {
            double oldScore = evaluateStatePower(oldState, oldState.currentPlayer());
            double newScore = evaluateStatePower(newState, oldState.currentPlayer());
            reward += (newScore - oldScore) * 10.0;  // Scale up state power differences

            int player = oldState.currentPlayer();

            // Reward for securing columns
            int[] beforeClaimed = oldState.getClaimedBy();
            int[] afterClaimed = newState.getClaimedBy();
            for (int i = 0; i < beforeClaimed.length; i++) {
                if (beforeClaimed[i] != player && afterClaimed[i] == player) {
                    reward += SECURE_COLUMN_BONUS;
                }
            }

            // Reward for saving progress (Stop action)
            int[] beforeTemp = oldState.getTemp();
            int[] afterTemp = newState.getTemp();
            int savedColumns = 0;
            for (int i = 0; i < beforeTemp.length; i++) {
                int diff = beforeTemp[i] - afterTemp[i];  // Progress lost if busted
                if (diff > 0) {
                    reward += SAVE_PROGRESS_BONUS * diff;  // Now positive
                    savedColumns++;
                }
            }
            
            // Bonus for making a strategic stop (if multiple columns saved)
            if (savedColumns > 0) {
                reward += STOP_BONUS * savedColumns;
            }

            if (newState.isTerminal()) {
                double[] terminalRewards = newState.terminalRewards();
                if (terminalRewards[player] == 1.0) reward += WIN_BONUS;
            }
            else {
                reward += STEP_PENALTY;
            }
        }

        return reward;
    }
    
    /**
     * Calculate reward bonus for consecutive successful rolls without busting.
     * The more consecutive rolls, the higher the bonus (exponential-like growth).
     * @param consecutiveRolls number of successful rolls in a row
     * @return bonus reward for consecutive rolls
     */
    public static double getConsecutiveRollBonus(int consecutiveRolls) {
        if (consecutiveRolls <= 0) return 0.0;
        // Bonus increases with each roll: 20, 40, 60, 80, etc.
        return CONSECUTIVE_ROLL_BONUS * consecutiveRolls;
    }

    private static double evaluateStatePower(GameState state, int playerIndex) {
        double score = 0;
        int[] temp = state.getTemp();
        int[][] perm = state.getPerm();
        int[] heights = {3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3};

        for (int i = 0; i < temp.length; i++) {

            int totalSteps = temp[i] + perm[playerIndex][i];
            if (totalSteps > 0) {
                double completionRatio = (double) totalSteps / heights[i];
                score += Math.pow(completionRatio, 2) * 20; 
            }
        }
        return score;
    }
}