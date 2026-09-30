package AI.Testing;

import AI.GameAction;
import AI.GameState;
import AI.Qlearning.EdgeCaseHandler;
import AI.Qlearning.QLearningAgent;
import AI.Qlearning.QTable;
import AI.Qlearning.Rewards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class HyperparameterTrainingTesting {

    private static final Random globalRandom = new Random();

    public static void runHyperparameterSweep(int episodesPerConfig, int numPlayers, int winTarget, boolean verbose) throws IOException {
        double[] alphas = ascendingRange(0.01, 0.5, 0.07);
        double[] gammas = ascendingRange(0.8, 0.99, 0.02);
        double[] epsilons = descendingRange(1.0, 0.1, 0.05);

        File qtablesDir = new File("qtables_hyperparams");
        if (!qtablesDir.exists()) qtablesDir.mkdirs();

        String resultsCsv = "hyperparameter_tuning_results.csv";
        File outFile = new File(resultsCsv);
        boolean exists = outFile.exists();
        String header = "alpha,gamma,epsilon,episodes,wins,winRate,avgStepsAll,avgStepsWhenWin,avgReward,avgQDelta,qtableFile";
        if (!exists) {
            try (java.io.FileWriter fw = new java.io.FileWriter(outFile, true)) {
                fw.append(header);
                fw.append(System.lineSeparator());
            }
        }

        int configIndex = 1;
        int totalConfigs = alphas.length * gammas.length * epsilons.length;

        for (double alpha : alphas) {
            for (double gamma : gammas) {
                for (double epsilon : epsilons) {
                    if (verbose) System.out.printf("Config %d/%d: alpha=%.3f gamma=%.3f epsilon=%.3f\n", 
                            configIndex, totalConfigs, alpha, gamma, epsilon);

                    QTable table = new QTable();
                    QLearningAgent agent = new QLearningAgent(table);
                    agent.setAlpha(alpha);
                    agent.setGamma(gamma);
                    agent.setEpsilon(epsilon);

                    TrainMetrics m = trainConfig(agent, table, episodesPerConfig, numPlayers, winTarget, verbose);

                    System.out.printf("Agent %d/%d trained: alpha=%.3f gamma=%.3f epsilon=%.3f\n", configIndex, totalConfigs, alpha, gamma, epsilon);

                    String fileName = String.format("qtables_hyperparams/qtable_a%.3f_g%.3f_e%.3f.dat", 
                            alpha, gamma, epsilon);
                    table.saveToFile(fileName);

                    String csvLine = String.format("%.3f,%.3f,%.3f,%d,%d,%.6f,%.3f,%.3f,%.6f,%.6f,%s",
                            alpha, gamma, epsilon, episodesPerConfig, m.wins, m.winRate, 
                            m.avgStepsAll, m.avgStepsWhenWin, m.avgReward, m.avgQDelta, fileName);
                    try (java.io.FileWriter fw = new java.io.FileWriter(outFile, true)) {
                        fw.append(csvLine);
                        fw.append(System.lineSeparator());
                    }

                    if (verbose) System.out.printf("  Completed -> winRate=%.6f avgReward=%.6f avgQDelta=%.6f avgSteps=%.3f\n",
                            m.winRate, m.avgReward, m.avgQDelta, m.avgStepsAll);

                    configIndex++;
                }
            }
        }

        System.out.printf("Hyperparameter sweep complete. Results saved to %s\n", resultsCsv);
    }

    private static TrainMetrics trainConfig(QLearningAgent agent, QTable table, int episodes, 
                                            int numPlayers, int winTarget, boolean verbose) {
        Random rng = new Random(globalRandom.nextLong());

        int trainedPlayer = 0;
        int wins = 0;
        long totalSteps = 0;
        double totalReward = 0.0;
        double totalQDelta = 0.0;
        long totalWinSteps = 0;

        for (int ep = 1; ep <= episodes; ep++) {
            GameState state = GameState.newGame(numPlayers, winTarget);
            int steps = 0;
            double episodeReward = 0.0;

            while (!state.isTerminal()) {
                int playerIdx = state.currentPlayer();
                GameAction action;
                if (playerIdx == trainedPlayer) action = agent.chooseAction(state);
                else {
                    List<GameAction> avail = state.legalActions();
                    action = avail.get(rng.nextInt(avail.size()));
                }

                if (action == null) break;

                GameState newState;
                if (action instanceof GameAction.Roll) {
                    int[] dice = new int[4];
                    for (int i = 0; i < 4; i++) dice[i] = rng.nextInt(6) + 1;
                    List<int[]> pairings = new ArrayList<>();
                    pairings.add(new int[]{dice[0] + dice[1], dice[2] + dice[3]});
                    pairings.add(new int[]{dice[0] + dice[2], dice[1] + dice[3]});
                    pairings.add(new int[]{dice[0] + dice[3], dice[1] + dice[2]});
                    newState = state.withPairingsAndDice(pairings, dice);
                } else {
                    newState = state.apply(action);
                }

                boolean busted = EdgeCaseHandler.didBust(state, newState);

                double reward = Rewards.calculateReward(state, newState, busted);
                if (busted) reward = Rewards.BUST_PENALTY;

                if (playerIdx == trainedPlayer) {
                    double oldQ = table.getQValue(state, action);
                    agent.learn(state, action, reward, newState);
                    double newQ = table.getQValue(state, action);
                    totalQDelta += Math.abs(newQ - oldQ);
                    episodeReward += reward;
                }

                state = newState;
                steps++;

                if (state.isTerminal()) {
                    if (state.terminalRewards()[trainedPlayer] == 1.0) {
                        wins++;
                        totalWinSteps += steps;
                    }
                }
            }

            totalSteps += steps;
            totalReward += episodeReward;
        }

        double winRate = wins / (double) episodes;
        double avgStepsAll = totalSteps / (double) episodes;
        double avgStepsWhenWin = wins > 0 ? (totalWinSteps / (double) wins) : -1.0;
        double avgReward = totalReward / (double) episodes;
        double avgQDelta = totalQDelta / (double) Math.max(1, episodes);

        TrainMetrics m = new TrainMetrics();
        m.wins = wins;
        m.winRate = winRate;
        m.avgStepsAll = avgStepsAll;
        m.avgStepsWhenWin = avgStepsWhenWin;
        m.avgReward = avgReward;
        m.avgQDelta = avgQDelta;
        return m;
    }

    private static class TrainMetrics {
        int wins;
        double winRate;
        double avgStepsAll;
        double avgStepsWhenWin;
        double avgReward;
        double avgQDelta;
    }

    private static double[] ascendingRange(double start, double end, double step) {
        int steps = (int) Math.round((end - start) / step);
        if (steps < 0) return new double[0];
        double[] arr = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            arr[i] = Math.round((start + i * step) * 1000.0) / 1000.0;
        }
        return arr;
    }

    private static double[] descendingRange(double start, double end, double step) {
        int steps = (int) Math.round((start - end) / step);
        if (steps < 0) return new double[0];
        double[] arr = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            arr[i] = Math.round((start - i * step) * 1000.0) / 1000.0;
        }
        return arr;
    }

    public static void main(String[] args) throws IOException {
        int episodes = 10000; 
        int players = 1;
        int winTarget = 3;
        boolean verbose = false;
        if (args.length >= 1) episodes = Integer.parseInt(args[0]);
        if (args.length >= 2) players = Integer.parseInt(args[1]);
        if (args.length >= 3) winTarget = Integer.parseInt(args[2]);
        if (args.length >= 4) verbose = Boolean.parseBoolean(args[3]);

        runHyperparameterSweep(episodes, players, winTarget, verbose);
    }
}