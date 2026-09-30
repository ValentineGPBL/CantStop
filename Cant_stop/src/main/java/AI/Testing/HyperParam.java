package AI.Testing;

import AI.MCTS.MCTSAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class HyperParam {

    public static void findBestParameters(int[] iterationsOptions, double[] explorationFactorOptions, int numGamesPerSetting, int numPlayers, int winTarget) {
        System.out.println("Starting Grid Search");
        System.out.println("========================================");

        // Multi threading initialized
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numCores);
        System.out.println("Running search on " + numCores + " cores.");

        // Store asynchronus results
        List<Future<SearchResult>> futureResults = new ArrayList<>();

        for (int iterations : iterationsOptions) {
            for (double explorationFactor : explorationFactorOptions) {
                Callable<SearchResult> task = () -> {
                    MCTSAgent weakAgent = new MCTSAgent(500, 1.41);
                    System.out.printf("Starting:Iterations: %d, Exploration: %.2f\n", iterations, explorationFactor);

                    MCTSAgent challengerAgent = new MCTSAgent(iterations, explorationFactor);
                    MCTSvsMCTS.TestResult result = MCTSvsMCTS.runTests(numGamesPerSetting, numPlayers, winTarget, challengerAgent, weakAgent);
                    double winRate = (double) result.winCounts[0] / result.gamesPlayed;
                    System.out.printf("Finished:Iterations: %-5d | Exploration Factor: %-5.2f | Win Rate: %.2f%%\n",
                            iterations, explorationFactor, winRate * 100);
                    return new SearchResult(iterations, explorationFactor, winRate);
                };
                futureResults.add(executor.submit(task));
            }
        }


        double bestWinRate = -1.0;
        SearchResult bestResult = null;

        for (Future<SearchResult> future : futureResults) {
            try {
                SearchResult result = future.get();//wait for results
                if (result.winRate > bestWinRate) {
                    bestWinRate = result.winRate;
                    bestResult = result;
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        System.out.println("\n=====================================================================");
        System.out.printf("Best Win Rate: %.2f%%\n", bestResult.winRate * 100);
        System.out.printf("Best Iterations: %d\n", bestResult.iterations);
        System.out.printf("Best Exploration Factor: %.2f\n", bestResult.explorationFactor);
        System.out.println("=====================================================================");
    }

    //stores results in objects to make it usable in multi thread
    private static class SearchResult {
        final int iterations;
        final double explorationFactor;
        final double winRate;

        SearchResult(int iterations, double explorationFactor, double winRate) {
            this.iterations = iterations;
            this.explorationFactor = explorationFactor;
            this.winRate = winRate;
        }
    }

    public static void main(String[] args) {
        int[] iterationsToTest ={1000};
        double[] explorationFactorsToTest = linspace(0,10,30);
        int numGames = 20;
        int playerCount = 2;
        int columnsToWin = 3;

        findBestParameters(iterationsToTest, explorationFactorsToTest, numGames, playerCount, columnsToWin);
    }
    private static double[] linspace(double start, double end, int num) {
        double ratio= (start+ end)/num;
        double[] line=new double[num+1];
        line[0]=start;
        for(int i=1;i<num+1;i++){
            line[i]=line[i-1]+ratio;
        }
        return line;
    }
}
