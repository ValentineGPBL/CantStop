package AI.Testing;

import AI.GameAction;
import AI.GameState;
import AI.Qlearning.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Optimized training for Q-Learning to minimize game length
 * Key optimizations:
 * - Uses GROUPED_BUCKETING for better state space generalization
 * - Improved reward structure that encourages quick wins
 * - Better hyperparameters (alpha, gamma, epsilon)
 * - Tracks step count to monitor game length improvement
 */
public class OptimizedSingleAgentTraining {

    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {

        // Use GROUPED_BUCKETING for better generalization and faster convergence
        String rewardConfig = Rewards.getRewardConfigString();
        String qTableFile = "cant_stop_optimized_data_" + rewardConfig + ".dat";
        String csvFile = "optimized_agent_metrics_" + rewardConfig + ".csv";
        
        QTable table = new QTable(QTable.BucketingStrategy.GROUPED_BUCKETING);

        File qtableFile = new File(qTableFile);
        if (qtableFile.exists()) {
            table.loadFromFile(qTableFile);
        }

        QLearningAgent agent = new QLearningAgent(table);
        agent.setAlpha(0.1);      // Faster learning
        agent.setGamma(0.95);     // Value future rewards more
        agent.setEpsilon(0.15);   // Less random exploration
        
        MetricsTracker metricsTracker = new MetricsTracker();
        CSVLogger csvLogger = new CSVLogger(csvFile, 10000);

        GameState newState;
        double reward;
        boolean busted;
        int totalWins = 0;
        double lastQDelta = 0.0;
        
        // Track game lengths
        int totalSteps = 0;
        int minSteps = Integer.MAX_VALUE;
        int maxSteps = 0;

        System.out.println("Starting Optimized Q-Learning Training...");
        System.out.println("Strategy: GROUPED_BUCKETING");
        System.out.println("Alpha: 0.1, Gamma: 0.95, Epsilon: 0.15");
        System.out.println();

        for (int episode = 0; episode < 500000; episode++) {
            GameState startState = GameState.newGame(1, 3);
            int steps = 0;
            int consecutiveRolls = 0;
            double episodeReward = 0.0;
            boolean episodeWon = false;

            while(!startState.isTerminal()) {

                GameAction action = agent.chooseAction(startState);

                if (action instanceof GameAction.Roll) {
                    consecutiveRolls++;
                    int[] dice = new int[4];
                    for (int j = 0; j < 4; j++) dice[j] = random.nextInt(6) + 1;

                    List<int[]> pairings = new ArrayList<>();
                    pairings.add(new int[]{dice[0] + dice[1], dice[2] + dice[3]});
                    pairings.add(new int[]{dice[0] + dice[2], dice[1] + dice[3]});
                    pairings.add(new int[]{dice[0] + dice[3], dice[1] + dice[2]});

                    newState = startState.withPairingsAndDice(pairings, dice);
                } else {
                    consecutiveRolls = 0;
                    newState = startState.apply(action);
                }
                busted = EdgeCaseHandler.didBust(startState, newState);

                reward = Rewards.calculateReward(startState, newState, busted);
                
                // Add consecutive roll bonus if this was a successful roll
                if (!busted && action instanceof GameAction.Roll) {
                    reward += Rewards.getConsecutiveRollBonus(consecutiveRolls);
                } else if (busted) {
                    consecutiveRolls = 0;
                }
                
                lastQDelta = agent.learn(startState, action, reward, newState);
                episodeReward += reward;
                startState = newState;
                steps++;
            }

            // Track if episode was won
            episodeWon = startState.isTerminal() && startState.terminalRewards()[0] == 1.0;
            if (episodeWon) totalWins++;
            
            // Track step statistics
            totalSteps += steps;
            minSteps = Math.min(minSteps, steps);
            maxSteps = Math.max(maxSteps, steps);
            
            // Record metrics for this episode
            double avgReward = steps > 0 ? episodeReward / steps : 0.0;
            metricsTracker.recordEpisode(episode + 1, episodeWon, avgReward, lastQDelta, steps);
            
            // Decay epsilon
            agent.epsilonDecay();
            
            // Log metrics and progress every 1000 episodes
            if ((episode + 1) % 1000 == 0) {
                double avgSteps = totalSteps / 1000.0;
                double winRate = (totalWins * 100.0) / (episode + 1);
                System.out.printf("Episode %d | Avg Steps: %.1f | Min: %d | Max: %d | Win Rate: %.1f%% | Epsilon: %.4f%n",
                    episode + 1, avgSteps, minSteps, maxSteps, winRate, agent.getEpsilon());
                
                csvLogger.logMetrics(episode, metricsTracker, totalWins, agent.getEpsilon());
                totalSteps = 0;
                minSteps = Integer.MAX_VALUE;
                maxSteps = 0;
            }
        }

        csvLogger.close();
        table.saveToFile(qTableFile);
        System.out.println("\nTraining complete! Metrics saved to " + csvFile);
        System.out.println("Q-Table saved to " + qTableFile);
    }
}
