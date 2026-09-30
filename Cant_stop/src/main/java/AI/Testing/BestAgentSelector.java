package AI.Testing;

import AI.Qlearning.QTable;

import java.io.*;
import java.util.*;


public class BestAgentSelector {

    private static class AgentResult {
        double alpha, gamma, epsilon;
        int episodes, wins;
        double winRate, avgStepsAll, avgStepsWhenWin, avgReward, avgQDelta;
        String qtableFile;

        @Override
        public String toString() {
            return String.format("alpha=%.3f gamma=%.3f epsilon=%.3f | winRate=%.6f avgStepsWhenWin=%.3f avgQDelta=%.6f",
                    alpha, gamma, epsilon, winRate, avgStepsWhenWin, avgQDelta);
        }
    }

    public static void main(String[] args) throws IOException {
        String csvFile = "hyperparameter_tuning_results.csv";
        if (args.length >= 1) csvFile = args[0];

        AgentResult best = selectBestAgent(csvFile);

        if (best != null) {
            System.out.println("========================================");
            System.out.println("BEST AGENT SELECTED");
            System.out.println("========================================");
            System.out.printf("Alpha:              %.3f\n", best.alpha);
            System.out.printf("Gamma:              %.3f\n", best.gamma);
            System.out.printf("Epsilon:            %.3f\n", best.epsilon);
            System.out.printf("Episodes:           %d\n", best.episodes);
            System.out.printf("Wins:               %d\n", best.wins);
            System.out.printf("Win Rate:           %.6f\n", best.winRate);
            System.out.printf("Avg Steps (All):    %.3f\n", best.avgStepsAll);
            System.out.printf("Avg Steps (Win):    %.3f\n", best.avgStepsWhenWin);
            System.out.printf("Avg Reward:         %.6f\n", best.avgReward);
            System.out.printf("Avg Q-Delta:        %.6f\n", best.avgQDelta);
            System.out.printf("Q-Table File:       %s\n", best.qtableFile);
            System.out.println("========================================");

            QTable bestTable = new QTable();
            bestTable.loadFromFile(best.qtableFile);
            System.out.println("Best agent Q-table loaded successfully.");
        } else {
            System.out.println("No agents found in CSV or CSV is empty.");
        }
    }

    public static AgentResult selectBestAgent(String csvFile) throws IOException {
        File file = new File(csvFile);
        if (!file.exists()) {
            System.err.println("CSV file not found: " + csvFile);
            return null;
        }

        List<AgentResult> results = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; 
                }

                String[] parts = line.split(",");
                if (parts.length < 11) continue; 

                try {
                    AgentResult r = new AgentResult();
                    r.alpha = Double.parseDouble(parts[0]);
                    r.gamma = Double.parseDouble(parts[1]);
                    r.epsilon = Double.parseDouble(parts[2]);
                    r.episodes = Integer.parseInt(parts[3]);
                    r.wins = Integer.parseInt(parts[4]);
                    r.winRate = Double.parseDouble(parts[5]);
                    r.avgStepsAll = Double.parseDouble(parts[6]);
                    r.avgStepsWhenWin = Double.parseDouble(parts[7]);
                    r.avgReward = Double.parseDouble(parts[8]);
                    r.avgQDelta = Double.parseDouble(parts[9]);
                    r.qtableFile = parts[10];

                    results.add(r);
                } catch (NumberFormatException e) {
                }
            }
        }

        if (results.isEmpty()) return null;

        AgentResult best = results.get(0);
        for (AgentResult r : results) {
            if (isBetter(r, best)) best = r;
        }

        return best;
    }

    private static boolean isBetter(AgentResult r, AgentResult best) {
        final double EPS = 1e-9;

        if (r.winRate > best.winRate + EPS) return true;
        if (Math.abs(r.winRate - best.winRate) > EPS) return false;

        double rSteps = r.avgStepsWhenWin < 0 ? Double.MAX_VALUE : r.avgStepsWhenWin;
        double bestSteps = best.avgStepsWhenWin < 0 ? Double.MAX_VALUE : best.avgStepsWhenWin;

        if (rSteps < bestSteps - EPS) return true;
        if (Math.abs(rSteps - bestSteps) > EPS) return false;

        if (r.avgQDelta < best.avgQDelta - EPS) return true;
        if (Math.abs(r.avgQDelta - best.avgQDelta) > EPS) return false;

        if (r.alpha < best.alpha - EPS) return true;
        if (Math.abs(r.alpha - best.alpha) > EPS) return false;

        if (r.gamma > best.gamma + EPS) return true;

        return false;
    }
}