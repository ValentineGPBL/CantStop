package AI.Testing;

import AI.Qlearning.MetricsTracker;

import java.io.*;
import java.util.*;


public class CSVLogger {
    private final String filePath;
    private final int logInterval; 
    private final FileWriter fileWriter;
    private boolean headerWritten = false;

    public CSVLogger(String filePath, int logInterval) throws IOException {
        this.filePath = filePath;
        this.logInterval = logInterval;
        
    
        File file = new File(filePath);
        boolean fileExists = file.exists();
        
        this.fileWriter = new FileWriter(filePath, true);
        
        if (!fileExists) {
            writeHeader();
        }
        headerWritten = true;
    }

    private void writeHeader() throws IOException {
        String header = "Episode,Win_Rate,Avg_Reward,Avg_Q_Delta,Total_Wins,Avg_Turns_To_Win,Epsilon\n";
        fileWriter.append(header);
        fileWriter.flush();
    }

    /**
     * Log metrics at specified intervals
     * @param episode 
     * @param metricsTracker 
     * @param totalWins 
     * @param epsilon 
     */
    public void logMetrics(int episode, MetricsTracker metricsTracker, int totalWins, double epsilon) throws IOException {
        if ((episode + 1) % logInterval != 0) {
            return;
        }

        List<Double> winRates = metricsTracker.getWinRateHistory();
        List<Double> avgRewards = metricsTracker.getAvgRewardHistory();
        List<Integer> episodeLengths = metricsTracker.getEpisodeLengthHistory();
        List<Double> qDeltas = metricsTracker.getQDeltaHistory();
        List<Integer> winSteps = metricsTracker.getWinStepsHistory();

        double avgWinRate = calculateAverage(winRates, logInterval);
        double avgReward = calculateAverage(avgRewards, logInterval);
        double avgQDelta = calculateAverage(qDeltas, logInterval);
        double avgTurnsToWin = calculateAverageTurnsToWin(winSteps, logInterval);

        String logLine = String.format("%d,%.4f,%.4f,%.6f,%d,%.2f,%.6f\n",
                episode + 1,
                avgWinRate,
                avgReward,
                avgQDelta,
                totalWins,
                avgTurnsToWin,
                epsilon
        );

        fileWriter.append(logLine);
        fileWriter.flush();

        System.out.printf("Episode %d - Win Rate: %.4f | Avg Reward: %.4f | Q-Delta: %.6f | Epsilon: %.6f\n",
                episode + 1, avgWinRate, avgReward, avgQDelta, epsilon);
    }

  
    private double calculateAverage(List<Double> values, int count) {
        if (values.isEmpty()) return 0.0;
        
        int start = Math.max(0, values.size() - count);
        double sum = 0;
        for (int i = start; i < values.size(); i++) {
            sum += values.get(i);
        }
        return sum / (values.size() - start);
    }



    private double calculateAverageTurnsToWin(List<Integer> winSteps, int count) {
        if (winSteps.isEmpty()) return 0.0;
        
        int start = Math.max(0, winSteps.size() - count);
        double sum = 0;
        int numWins = winSteps.size() - start;
        
        for (int i = start; i < winSteps.size(); i++) {
            sum += winSteps.get(i);
        }
        return sum / numWins;
    }

    public void close() throws IOException {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}
