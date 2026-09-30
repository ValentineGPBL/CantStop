package AI.Testing;

import AI.GameState;
import AI.MCTS.MCTSAgent;

public class TimeForParam {


    public static void measureMoveTimes(int[] iterationsOptions, double[] explorationFactorOptions, int runsPerSetting) {
        System.out.println("Starting Measurement");
        System.out.println("===================================================");
        System.out.printf("Measuring average time.\n\n", runsPerSetting);

        GameState testState = GameState.newGame(2, 3);

        System.out.printf("%-12s | %-20s | %-20s\n", "Iterations", "Exploration Factor", "Avg.Time / Move (ms)");
        System.out.println("------------------------------------------------------------------------------");

        for (int iterations : iterationsOptions) {
            for (double explorationFactor : explorationFactorOptions) {
                MCTSAgent agent = new MCTSAgent(iterations, explorationFactor);
                long totalDurationNanos = 0;
                // Measurement
                for (int i = 0; i < runsPerSetting; i++) {
                    GameState currentState = testState.clone();

                    long startTime = System.nanoTime();
                    agent.chooseAction(currentState);
                    long endTime = System.nanoTime();

                    totalDurationNanos += (endTime - startTime);
                }

                double averageTimeMillis = (double) totalDurationNanos / runsPerSetting / 1_000_000.0;
                System.out.printf("%-12d | %-20.2f | %-20.2f\n",
                        iterations, explorationFactor, averageTimeMillis);
            }
        }
        System.out.println("======================================================");
    }

    public static void main(String[] args) {
        int[] iterationsToTest = linspace(1,1000, 20);
        double[] explorationFactorsToTest = {1.41};
        int numberOfRuns = 10;

        measureMoveTimes(iterationsToTest, explorationFactorsToTest, numberOfRuns);
    }

    private static int[] linspace(int start, int end, int num) {
        int ratio= (start+ end)/num;
        int[] line=new int[num+1];
        line[0]=start;

        for(int i=1;i<num+1;i++){
            line[i]=line[i-1]+ratio;
        }

        return line;
    }
}