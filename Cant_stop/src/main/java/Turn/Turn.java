package Turn;

import Player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public class Turn {

    public static final int MIN_SUM = 2;
    public static final int MAX_SUM = 12;
    public static final int COLS = (MAX_SUM - MIN_SUM + 1);
    public static final int MAX_RUNNERS = 3;
    private static final int[] HEIGHTS = { 3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3 };

    private final Player player;
    private boolean active = true;
    private final int[] tempSteps = new int[COLS];
    private final boolean[] activeCols = new boolean[COLS];

    public Turn(Player player) {
        this.player = Objects.requireNonNull(player, "player");
        Arrays.fill(activeCols, false);// Start each turn with no active columns

    }
    public Player getPlayer() { return player; }
    public boolean[] getActiveColsArray() { return Arrays.copyOf(activeCols, COLS); }
    public int[] getTempStepsArray() {
        return Arrays.copyOf(tempSteps, COLS);
    }
    public MoveResult applyPairing(int sumA, int sumB, Set<Integer> lockedColumns) {
        checkActive();
        String vrA = validateSum(sumA); if (vrA != null) return MoveResult.illegal(vrA);
        String vrB = validateSum(sumB); if (vrB != null) return MoveResult.illegal(vrB);

        int[] newTemp = Arrays.copyOf(tempSteps, COLS);
        boolean[] newActive = Arrays.copyOf(activeCols, COLS);
        boolean movedSomething = false;

        // advance on each sum
        if (tryAdvanceOnce(sumA, newTemp, newActive, lockedColumns) == null) {
            movedSomething = true;
        }
        if (tryAdvanceOnce(sumB, newTemp, newActive, lockedColumns) == null) {
            movedSomething = true;
        }

        if (movedSomething) {
            System.arraycopy(newTemp, 0, tempSteps, 0, COLS);
            System.arraycopy(newActive, 0, activeCols, 0, COLS);
            return MoveResult.ok();
        } else {
            return MoveResult.illegal("No valid moves could be made. One or both columns may be full or you may have too many runners.");
        }
    }

    public ArrayList<Integer> stopAndCommit() {
        if (!active) return new ArrayList<>();

        ArrayList<Integer> newlyCompletedColumns = new ArrayList<>();
        ArrayList<Integer> perm = player.getPermanentStepPositions();
        ArrayList<Integer> currentClaims = player.getColumnsClaimed();

        for (int i = 0; i < COLS; i++) {
            if (tempSteps[i] > 0) {
                int after = perm.get(i) + tempSteps[i];
                if (after >= HEIGHTS[i]) {
                    after = HEIGHTS[i];
                    int sum = i + MIN_SUM;
                    if (!currentClaims.contains(sum)) {
                        newlyCompletedColumns.add(sum);
                    }
                }
                perm.set(i, after);
            }
        }
        player.setPermanentStepPositions(perm);
        clearAndEnd();
        return newlyCompletedColumns;
    }

 
    public MoveResult canApplyPairing(int sumA, int sumB, Set<Integer> lockedColumns) {
        if (!active) return MoveResult.illegal("turn is not active");
        String vrA = validateSum(sumA); if (vrA != null) return MoveResult.illegal(vrA);
        String vrB = validateSum(sumB); if (vrB != null) return MoveResult.illegal(vrB);

        int[] tmpSteps = Arrays.copyOf(tempSteps, COLS);
        boolean[] tmpActive = Arrays.copyOf(activeCols, COLS);

        String r1 = tryAdvanceOnce(sumA, tmpSteps, tmpActive, lockedColumns);
        if (r1 != null) return MoveResult.illegal(r1);

        String r2 = tryAdvanceOnce(sumB, tmpSteps, tmpActive, lockedColumns);
        if (r2 != null) return MoveResult.illegal(r2);

        return MoveResult.ok();
    }

    public ArrayList<int[]> legalPairings(ArrayList<int[]> candidates, Set<Integer> lockedColumns) {
        ArrayList<int[]> out = new ArrayList<>();
        if (candidates == null) return out;
        for (int[] p : candidates) {
            if (p == null || p.length != 2) continue;
            if (canApplyPairing(p[0], p[1], lockedColumns).ok) {
                out.add(new int[]{p[0], p[1]});
            }
        }
        return out;
    }

    private String tryAdvanceOnce(int sum, int[] t, boolean[] a, Set<Integer> lockedColumns) {
        int i = idx(sum);

        if (lockedColumns.contains(sum)) {
            return "column " + sum + " has been claimed and is locked";
        }

        int permanentProgress = player.getPermanentStepPositions().get(i);
        int currentTempProgress = t[i];
        if (permanentProgress + currentTempProgress >= HEIGHTS[i]) {
            return "column " + sum + " is already at the top";
        }

        if (!a[i]) {
            if (countActive(a) >= MAX_RUNNERS) {
                return "cannot add a 4th runner";
            }
            a[i] = true;
        }
        t[i]++;
        return null;
    }
    public void bust() {
        if (!active) return;
        clearAndEnd();
    }
    private void clearAndEnd() {
        Arrays.fill(tempSteps, 0);
        Arrays.fill(activeCols, false);
        active = false;
    }
    private static int idx(int sum) { return sum - MIN_SUM; }
    private static int countActive(boolean[] flags) { int c = 0; for (boolean b : flags) if (b) c++; return c; }
    private static String validateSum(int sum) { if (sum < MIN_SUM || sum > MAX_SUM) return "sum out of range (2..12)"; return null; }
    private void checkActive() { if (!active) throw new IllegalStateException("turn is not active"); }

    public static final class MoveResult {
        public final boolean ok; public final String reason;
        private MoveResult(boolean ok, String reason) { this.ok = ok; this.reason = reason; }
        public static MoveResult ok() { return new MoveResult(true, null); }
        public static MoveResult illegal(String reason) { return new MoveResult(false, reason); }
    }
}