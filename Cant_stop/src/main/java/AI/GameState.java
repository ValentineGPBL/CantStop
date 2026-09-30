package AI;

import java.io.Serializable;
import java.util.*;

public final class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MIN_SUM = 2, MAX_SUM = 12, COLS = 11, MAX_RUNNERS = 3;
    private static final int[] HEIGHTS = {3,5,7,9,11,13,11,9,7,5,3};

    //permanent data
    private final int playerCount; //number of players
    private final int currentPlayer; //whose turn it is
    private final int winTarget;//columns to win
    private final int[][] perm; //permanent steps in a column (player)(column)
    private final int[] claimedBy; //who owns column (-1 if unclaimed)

    //temporary data
    private final int[] temp;//temporary steps this turn
    private final boolean[] active;//which col has pawns
    private final List<int[]> pairings;//possible pairings from last roll

    private final int[] lastDiceRoll;

    private GameState(int playerCount, int currentPlayer, int winTarget,
                      int[][] perm, int[] claimedBy,
                      int[] temp, boolean[] active, List<int[]> pairings, int[] lastDiceRoll) {
        this.playerCount = playerCount;
        this.currentPlayer = currentPlayer;
        this.winTarget = winTarget;
        this.perm = deepCopy(perm);
        this.claimedBy = Arrays.copyOf(claimedBy, claimedBy.length);
        this.temp = Arrays.copyOf(temp, temp.length);
        this.active = Arrays.copyOf(active, active.length);
        this.pairings = copyPairs(pairings);
        this.lastDiceRoll = (lastDiceRoll != null) ? Arrays.copyOf(lastDiceRoll, lastDiceRoll.length) : null;
    }

    //sets state to new board
    public static GameState newGame(int players, int winTarget) {
        int[][] perm = new int[players][COLS];
        int[] claimedBy = new int[COLS];
        Arrays.fill(claimedBy, -1);
        return new GameState(players, 0, winTarget, perm, claimedBy,
                new int[COLS], new boolean[COLS], List.of(), null);
    }

    //check current player
    public int currentPlayer() { return currentPlayer; }

    //checks if player claimed enough columns to win (to stop sim)
    public boolean isTerminal() {
        int[] counts = new int[playerCount];
        for (int c : claimedBy) if (c >= 0) counts[c]++;
        for (int p = 0; p < playerCount; p++) if (counts[p] >= winTarget) return true;
        return false;
    }

    //rewards for player wins (+1 win -1 lose else 0)
    public double[] terminalRewards() {

        double[] rewards = new double[playerCount];
        int[] counts = new int[playerCount];
        for (int c : claimedBy) {
            if (c >= 0) counts[c]++;
        }

        boolean winnerFound = false;
        for (int p = 0; p < playerCount; p++) {
            if (counts[p] >= winTarget) {
                rewards[p] = 1.0; // Winner gets +1
                winnerFound = true;
            }
        }

        if (winnerFound) {
            for (int p = 0; p < playerCount; p++) {
                if (rewards[p] != 1.0) {
                    rewards[p] = -1.0; // Everyone else gets -1
                }
            }
        }
        return rewards;
    }

    //creates same state but with new dice pairings
    public GameState withPairings(List<int[]> newPairings) {
        return new GameState(playerCount, currentPlayer, winTarget,
                perm, claimedBy, temp, active, newPairings, lastDiceRoll);
    }
    public GameState withPairingsAndDice(List<int[]> newPairings, int[] diceRoll) {
    return new GameState(
            playerCount,
            currentPlayer,
            winTarget,
            perm,
            claimedBy,
            temp,
            active,
            newPairings,
            (diceRoll != null ? Arrays.copyOf(diceRoll, diceRoll.length) : null)
    );
}
	

    //shows all possible actions
    public List<GameAction> legalActions() {
        if (isTerminal()) return List.of();
        ArrayList<GameAction> out = new ArrayList<>();


        // pairings are available from roll
        boolean hasTempProgress = false;
        for (int s : temp) if (s > 0) hasTempProgress = true;

        if (hasTempProgress) {
            out.add(new GameAction.Stop());
        }
        if (pairings != null && !pairings.isEmpty()) {
            Set<Integer> locked = lockedSums();
            boolean anyLegal = false;
            for (int[] p : pairings) {
                if (canApplyPairing(p[0], p[1], locked)) {
                    out.add(new GameAction.Pairing(p[0], p[1]));
                    anyLegal = true;
                }
            }
            if (!anyLegal) {
                return List.of(new GameAction.Stop());
            }
        } else {
            // roll the dice.
            out.add(new GameAction.Roll());
        }
        return out;
    }

    //applies an action
    public GameState apply(GameAction action) {
        if (action instanceof GameAction.Stop) {
            return stopAndCommit();
        }
        if (action instanceof GameAction.Pairing pa) {
            return applyPairing(pa.sumA(), pa.sumB());
        }
        if (action instanceof GameAction.Roll) {
            //need to pass by the chance node
            return this.withPairings(List.of());
        }
        throw new IllegalArgumentException("Unknown action type");
    }

    //simulates move with pairings A and B
    private GameState applyPairing(int sumA, int sumB) {
        Set<Integer> locked = lockedSums();
        int[] t = Arrays.copyOf(temp, COLS);
        boolean[] a = Arrays.copyOf(active, COLS);

        String r1 = tryAdvanceOnce(sumA, t, a, locked, perm[currentPlayer]);
        if (r1 != null) throw new IllegalStateException(r1);
        String r2 = tryAdvanceOnce(sumB, t, a, locked, perm[currentPlayer]);
        if (r2 != null) throw new IllegalStateException(r2);

        return new GameState(playerCount, currentPlayer, winTarget,
                perm, claimedBy, t, a, List.of(),this.lastDiceRoll);
    }

    //simulates player choosing stop
    private GameState stopAndCommit() {
        int[][] newPerm = deepCopy(perm);
        int[] newClaimed = Arrays.copyOf(claimedBy, COLS);

        for (int i = 0; i < COLS; i++) {
            int before = newPerm[currentPlayer][i];
            int after = before + temp[i];
            int cap = HEIGHTS[i];
            if (after >= cap) {
                after = cap;
                if (newClaimed[i] == -1) newClaimed[i] = currentPlayer;
            }
            newPerm[currentPlayer][i] = after;
        }
        int nextPlayer = (currentPlayer + 1) % playerCount;
        return new GameState(playerCount, nextPlayer, winTarget,
                newPerm, newClaimed, new int[COLS], new boolean[COLS], List.of(), null);
    }

    //check if pairing is legal
    private boolean canApplyPairing(int a, int b, Set<Integer> locked) {
        int[] t = Arrays.copyOf(temp, COLS);
        boolean[] ac = Arrays.copyOf(active, COLS);

        int[] currentPerm = perm[currentPlayer];

        String r1 = tryAdvanceOnce(a, t, ac, locked, currentPerm);
        if (r1 != null) return false;
        String r2 = tryAdvanceOnce(b, t, ac, locked, currentPerm);
        return r2 == null;
    }

    //checks if possible to advance pawn in col
    private static String tryAdvanceOnce(int sum, int[] t, boolean[] a, Set<Integer> locked, int[] currentPerm) {
        int i = sum - MIN_SUM;
        if (i < 0 || i >= COLS) return "sum out of range";

        if (locked.contains(sum)) return "column locked";

        int runners = 0; for (boolean b : a) if (b) runners++;
        if (!a[i]) {
            if (runners >= MAX_RUNNERS) return "cannot add a 4th runner";
            a[i] = true;
        }

        int cap = HEIGHTS[i];
        if (currentPerm[i] + t[i] >= cap) return "column already at top";

        t[i] = t[i] + 1;
        return null;
    }

    //checks all already locked cols
    private Set<Integer> lockedSums() {
        HashSet<Integer> s = new HashSet<>();
        for (int i = 0; i < COLS; i++) if (claimedBy[i] != -1) s.add(i + MIN_SUM);
        return s;
    }

    //copies to simulate with
    private static boolean inRange(int sum) { return sum >= MIN_SUM && sum <= MAX_SUM; }
    private static int[][] deepCopy(int[][] m) { int[][] r = new int[m.length][]; for (int i=0;i<m.length;i++) r[i] = Arrays.copyOf(m[i], m[i].length); return r; }
    private static List<int[]> copyPairs(List<int[]> ps) { if (ps == null) return List.of(); ArrayList<int[]> r = new ArrayList<>(ps.size()); for (int[] p: ps) r.add(Arrays.copyOf(p,2)); return r; }

    //creates a snapsot of current game state
    public static GameState fromLive(List<? extends PlayerView> players, int currentPlayerIndex, int winTarget,int[] temp, boolean[] active) {

        int P = players.size();
        int[][] perm = new int[P][COLS];
        int[] claimed = new int[COLS]; Arrays.fill(claimed, -1);

        for (int p = 0; p < P; p++) {
            List<Integer> steps = players.get(p).permanentSteps();
            for (int i = 0; i < COLS; i++) perm[p][i] = steps.get(i);
            for (int sum : players.get(p).claimedSums()) {
                int idx = sum - MIN_SUM; if (idx >= 0 && idx < COLS) claimed[idx] = p;
            }
        }
        GameState gameState = new GameState(P, currentPlayerIndex, winTarget, perm, claimed,
                Arrays.copyOf(temp, COLS), Arrays.copyOf(active, COLS), List.of(), null);
        return gameState;
    }

    //used to take relevant info from player.java
    public interface PlayerView {

        Map<Integer, Integer> currentTempMarkers();

        List<Integer> permanentSteps();
        List<Integer> claimedSums();
    }

    @Override
    public GameState clone()  {
        return new GameState(playerCount, currentPlayer, winTarget, perm, claimedBy, temp, active, pairings,lastDiceRoll);
    }

    public int getPlayerCount() { return playerCount; }
    public int getWinTarget() { return winTarget; }
    public int[][] getPerm() { return perm; }
    public int[] getTemp() { return temp; }
    public boolean[] getActiveColumns() { return active; }
    public int[] getClaimedBy() {return claimedBy;}
    public int[] getLastDiceRoll() { return lastDiceRoll; }

    public int[] getActiveColumnIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < COLS; i++) {
            if (active[i]) {
                indices.add(i);
            }
        }
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    public Map<Integer, Integer> getColumnRemainingSteps() {
        Map<Integer, Integer> remaining = new HashMap<>();
        for (int i = 0; i < COLS; i++) {
            int columnSum = i + MIN_SUM;
            int remainingSteps = HEIGHTS[i] - (perm[currentPlayer][i] + temp[i]);
            remaining.put(columnSum, remainingSteps);
        }
        return remaining;
    }

    public Set<Integer> getClaimedColumns() {
        Set<Integer> claimed = new HashSet<>();
        for (int i = 0; i < COLS; i++) {
            if (claimedBy[i] != -1) {
                claimed.add(i + MIN_SUM);
            }
        }
        return claimed;
    }

    public Map<Integer, Integer> getTempMarkers() {
        Map<Integer, Integer> markers = new HashMap<>();
        for (int i = 0; i < COLS; i++) {
            if (active[i]) {
                markers.put(i + MIN_SUM, temp[i]);
            }
        }
        return markers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameState gameState = (GameState) o;
        return currentPlayer == gameState.currentPlayer &&
                Arrays.deepEquals(perm, gameState.perm) &&
                Arrays.equals(claimedBy, gameState.claimedBy) &&
                Arrays.equals(temp, gameState.temp) &&
                Arrays.equals(active, gameState.active);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(currentPlayer);
        result = 31 * result + Arrays.deepHashCode(perm);
        result = 31 * result + Arrays.hashCode(claimedBy);
        result = 31 * result + Arrays.hashCode(temp);
        result = 31 * result + Arrays.hashCode(active);
        return result;
    }

}

