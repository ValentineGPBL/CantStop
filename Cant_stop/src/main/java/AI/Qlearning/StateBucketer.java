package AI.Qlearning;

import AI.GameState;
import java.util.*;


public class StateBucketer {
    
    private static final int PROGRESS_BUCKETS = 5;
    
    private static final int[] HEIGHTS = {3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3};
    

    public static String gameStateToBucketedStr(GameState state) {
        StringBuilder sb = new StringBuilder();
        
        // Get state information
        int[] temp = state.getTemp();
        int[][] perm = state.getPerm();
        int[] claimedBy = state.getClaimedBy();
        int currentPlayer = state.currentPlayer();
        boolean[] active = state.getActiveColumns();
        
        // Create buckets for each column
        List<String> columnBuckets = new ArrayList<>();
        
        for (int col = 0; col < HEIGHTS.length; col++) {
            if (active[col]) {
                // Column is active - bucket by progress level
                int totalProgress = temp[col] + perm[currentPlayer][col];
                int progressBucket = getProgressBucket(totalProgress, HEIGHTS[col]);
                
                String columnInfo = String.format("A%d:%d:%d", 
                    col, 
                    progressBucket,
                    claimedBy[col]
                );
                columnBuckets.add(columnInfo);
            }
        }
        

        Collections.sort(columnBuckets);
        
        return columnBuckets.toString();
    }
    

    public static String gameStateToGroupedBucketedStr(GameState state) {
        StringBuilder sb = new StringBuilder();
        
        int[] temp = state.getTemp();
        int[][] perm = state.getPerm();
        int[] claimedBy = state.getClaimedBy();
        int currentPlayer = state.currentPlayer();
        boolean[] active = state.getActiveColumns();
        
        Map<Integer, List<Integer>> heightGroups = new TreeMap<>();
        
        for (int col = 0; col < HEIGHTS.length; col++) {
            if (active[col]) {
                heightGroups.computeIfAbsent(HEIGHTS[col], k -> new ArrayList<>()).add(col);
            }
        }
        
        List<String> groupBuckets = new ArrayList<>();
        
        for (Map.Entry<Integer, List<Integer>> entry : heightGroups.entrySet()) {
            int height = entry.getKey();
            List<Integer> cols = entry.getValue();
            
            List<Integer> progressBuckets = new ArrayList<>();
            for (int col : cols) {
                int totalProgress = temp[col] + perm[currentPlayer][col];
                int bucket = getProgressBucket(totalProgress, height);
                progressBuckets.add(bucket);
            }
            

            Collections.sort(progressBuckets);
            
            String groupInfo = String.format("H%d:%s", height, progressBuckets);
            groupBuckets.add(groupInfo);
        }
        
        return groupBuckets.toString();
    }
    

    public static String gameStateToAggressiveBucketedStr(GameState state) {
        int[] temp = state.getTemp();
        int[][] perm = state.getPerm();
        int currentPlayer = state.currentPlayer();
        boolean[] active = state.getActiveColumns();
        
        int[] bucketCounts = new int[PROGRESS_BUCKETS + 1];
        
        for (int col = 0; col < HEIGHTS.length; col++) {
            if (active[col]) {
                int totalProgress = temp[col] + perm[currentPlayer][col];
                int bucket = getProgressBucket(totalProgress, HEIGHTS[col]);
                bucketCounts[bucket]++;
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i <= PROGRESS_BUCKETS; i++) {
            if (bucketCounts[i] > 0) {
                sb.append(i).append(":").append(bucketCounts[i]).append(",");
            }
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        
        return sb.toString();
    }
    

    private static int getProgressBucket(int progress, int columnHeight) {
        if (progress <= 0) return 0;
        if (progress >= columnHeight) return PROGRESS_BUCKETS;
        
        double ratio = (double) progress / columnHeight;
        return (int) Math.ceil(ratio * PROGRESS_BUCKETS);
    }
    

    public static int getProgressBucketCount() {
        return PROGRESS_BUCKETS;
    }
}
