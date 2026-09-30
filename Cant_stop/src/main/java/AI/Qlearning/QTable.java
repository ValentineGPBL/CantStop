package AI.Qlearning;

import AI.GameAction;
import AI.GameState;
import java.io.*;

import java.util.*;

public class QTable implements Serializable{

    private static final int MAX_STATES = 500000;

    private Map<String, Map<String, Double>> table;
    
    // Bucketing strategy: NONE, COLUMN_BUCKETING, GROUPED_BUCKETING, AGGRESSIVE_BUCKETING
    private BucketingStrategy bucketingStrategy;
    
    public enum BucketingStrategy {
        NONE,                    // Original implementation, no bucketing
        COLUMN_BUCKETING,        // Bucket each column by progress level
        GROUPED_BUCKETING,       // Group similar columns by height, then bucket progress
        AGGRESSIVE_BUCKETING     // Count columns in each progress bucket regardless of which column
    }

    public QTable() {
        this(BucketingStrategy.COLUMN_BUCKETING);
    }
    
    public QTable(BucketingStrategy strategy) {
        this.bucketingStrategy = strategy;
        this.table = new LinkedHashMap<String, Map<String, Double>>(MAX_STATES + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Map<String, Double>> eldest) {
                return size() > MAX_STATES;
            }
        };
    }

    public String gameStateToStr(GameState state){
        switch(bucketingStrategy) {
            case COLUMN_BUCKETING:
                return StateBucketer.gameStateToBucketedStr(state);
            case GROUPED_BUCKETING:
                return StateBucketer.gameStateToGroupedBucketedStr(state);
            case AGGRESSIVE_BUCKETING:
                return StateBucketer.gameStateToAggressiveBucketedStr(state);
            case NONE:
            default:
                return gameStateToStrOriginal(state);
        }
    }
    
    /**
     * Original implementation without bucketing
     */
    private String gameStateToStrOriginal(GameState state){
        List<String> strState = new ArrayList<>();

        for (int active : state.getActiveColumnIndices()) {
            int reward = state.getColumnRemainingSteps().getOrDefault(active, 0);
            strState.add(active + ":" + reward);
        }

        Collections.sort(strState);
        return strState.toString();
    }
    
    /**
     * Set the bucketing strategy
     */
    public void setBucketingStrategy(BucketingStrategy strategy) {
        this.bucketingStrategy = strategy;
    }
    
    /**
     * Get the current bucketing strategy
     */
    public BucketingStrategy getBucketingStrategy() {
        return bucketingStrategy;
    }

    public double getQValue(GameState state, GameAction action) {
        String stateStr = gameStateToStr(state);
        String actionStr = action.toString();
        return table.getOrDefault(stateStr, new HashMap<>()).getOrDefault(actionStr, 0.0);
    }

    public void setQValue(GameState state, GameAction action, double value) {
        String stateStr = gameStateToStr(state);
        table.computeIfAbsent(stateStr, k -> new HashMap<>()).put(action.toString(), value);
    }

    public GameAction getBestAction(GameState state, List<GameAction> availableActions) {
        GameAction bestAction = availableActions.get(0);
        double bestScore = -Double.MAX_VALUE;

        for (GameAction action : availableActions) {
            double score = getQValue(state, action);
            if (score > bestScore) {
                bestAction = action;
                bestScore = score;
            }
        }
        return bestAction;
    }

    // to keep data from one run to another
    public void saveToFile(String fileName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(this.table);
            System.out.println("Q-Table successfully saved to " + fileName);
        } catch (IOException e) {
            System.err.println("Error saving Q-Table: " + e.getMessage());
        }
    }

    // to load it back
    @SuppressWarnings("unchecked")
    public void loadFromFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("No save file found. Starting with a fresh Q-Table.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            this.table = (Map<String, Map<String, Double>>) ois.readObject();
            System.out.println("Q-Table successfully loaded. States known: " + table.size());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading Q-Table: " + e.getMessage());
        }
    }
}