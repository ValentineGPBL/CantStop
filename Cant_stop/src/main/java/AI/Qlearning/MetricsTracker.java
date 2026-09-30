package AI.Qlearning;

import java.util.*;

public class MetricsTracker {

    private final List<Double> winRateHistory = new ArrayList<>();
    private final List<Double> avgRewardHistory = new ArrayList<>();
    private final List<Integer> episodeLengthHistory = new ArrayList<>();
    private final List<Double> qDeltaHistory = new ArrayList<>();
    private final List<Integer> winStepsHistory = new ArrayList<>();

    private int wins = 0;

    public void recordEpisode(int episode, //episode number
                              boolean won, //whether agent won this ep or not
                              double avgReward, //avgReward in episode
                              double lastQDelta, //magnitude of last q-value update
                              int steps) { //steps taken this ep

        if (won) {
            wins++;
            winStepsHistory.add(steps);
        }

        winRateHistory.add(wins / (double) episode);
        avgRewardHistory.add(avgReward);
        qDeltaHistory.add(lastQDelta);
        episodeLengthHistory.add(steps);
    }

    //getters
    public List<Double> getWinRateHistory() { return winRateHistory; }
    public List<Double> getAvgRewardHistory() { return avgRewardHistory; }
    public List<Integer> getEpisodeLengthHistory() { return episodeLengthHistory; }
    public List<Double> getQDeltaHistory() { return qDeltaHistory; }
    public List<Integer> getWinStepsHistory() { return winStepsHistory; }
}

