
package Conditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BustCondition {
    private static final int MAX_RUNNERS = 3;
    private static final int MIN_SUM = 2; 
    public static class BustResult {
        public final boolean busted;
        public final List<String> possibleMoves;

        public BustResult(boolean busted, List<String> possibleMoves) {
            this.busted = busted;
            this.possibleMoves = possibleMoves == null ? Collections.emptyList() : possibleMoves;
        }
    }


    public BustResult evaluate(int[] dice,
                               Map<Integer, Integer> columnRemaining,
                               Set<Integer> claimedColumns,
                               Map<Integer, Integer> tempMarkers,
                               boolean[] activeCols) {
        if (dice == null || dice.length != 4) {
            throw new IllegalArgumentException("dice must be an int[4]");
        }
        if (columnRemaining == null) throw new IllegalArgumentException("columnRemaining required");
        if (claimedColumns == null) throw new IllegalArgumentException("claimedColumns required");
        if (tempMarkers == null) tempMarkers = Collections.emptyMap();
        if (activeCols == null) throw new IllegalArgumentException("activeCols required");


        List<int[]> pairings = generatePairings(dice);
        List<String> accumulatedMoves = new ArrayList<>();

        for (int[] pairing : pairings) {
            int sumA = pairing[0];
            int sumB = pairing[1];

            boolean aCan = canAdvance(sumA, columnRemaining, claimedColumns, activeCols);
            boolean bCan = canAdvance(sumB, columnRemaining, claimedColumns, activeCols);

            if (aCan || bCan) {
                if (aCan) addExampleMove(accumulatedMoves, sumA, tempMarkers, columnRemaining);
                if (bCan) addExampleMove(accumulatedMoves, sumB, tempMarkers, columnRemaining);
                return new BustResult(false, accumulatedMoves);
            }
        }

        return new BustResult(true, Collections.emptyList());
    }

    private static List<int[]> generatePairings(int[] d) {
        return Arrays.asList(
                new int[]{d[0] + d[1], d[2] + d[3]},
                new int[]{d[0] + d[2], d[1] + d[3]},
                new int[]{d[0] + d[3], d[1] + d[2]}
        );
    }

    private static boolean canAdvance(int colSum,
                                      Map<Integer, Integer> columnRemaining,
                                      Set<Integer> claimedColumns,
                                      boolean[] activeCols) { // <-- PARAMETERS CHANGED

        if (claimedColumns.contains(colSum)) return false;

        if (columnRemaining.getOrDefault(colSum, 0) <= 0) return false;

        int colIndex = colSum - MIN_SUM;
        if (colIndex < 0 || colIndex >= activeCols.length) return false;

        if (activeCols[colIndex]) {
            return true;
        }

        int activeCount = 0;
        for (boolean b : activeCols) {
            if (b) activeCount++;
        }

        return activeCount < MAX_RUNNERS;
    }

    private static void addExampleMove(List<String> moves,
                                       int colSum,
                                       Map<Integer, Integer> tempMarkers,
                                       Map<Integer, Integer> columnRemaining) {
        if (moves == null) return;
        String move;
        Integer temp = tempMarkers.get(colSum);
        int remaining = columnRemaining.getOrDefault(colSum, 0);
        if (temp == null) {
            move = "Place new temp marker on " + colSum + " (remaining steps " + remaining + ")";
        } else {
            move = "Advance temp marker on " + colSum + " from " + temp + " -> " + (temp + 1) + " (remaining " + remaining + ")";
        }
        if (!moves.contains(move)) moves.add(move);
    }
}