// File: Cant_stop/src/main/java/game/WinCondition.java
package Conditions;

import Player.Player;
import java.util.List;


public class WinCondition {
    private final int columnsToWin;

//default win con of 3
    public WinCondition() {
        this.columnsToWin = 3;
    }

 // custom win con if needed
    public WinCondition(int columnsToWin) {
        if (columnsToWin < 1) {
            throw new IllegalArgumentException("columnsToWin must be at least 1");
        }
        this.columnsToWin = columnsToWin;
    }

// win check for a single player
    public boolean hasPlayerWon(Player player) {
        if (player == null) return false;
        List<Integer> claimedColumns = player.getColumnsClaimed();
        return claimedColumns != null && claimedColumns.size() >= columnsToWin;
    }

//checks a list of players and returns the first one that has won, or null if none have won
    public Player getWinningPlayer(List<Player> players) {
        if (players == null) return null;
        for (Player player : players) {
            if (hasPlayerWon(player)) {
                return player;
            }
        }
        return null;
    }


    public int getColumnsToWin() {
        return columnsToWin;
    }
}
