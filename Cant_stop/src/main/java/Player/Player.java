package Player;

import java.util.ArrayList;
import java.util.Collections;

public class Player {
    private final int playerID;
    private final String color;
    private ArrayList<Integer> permanentStepPositions;//progress for each column from 2 to 12
    private ArrayList<Integer> columnsClaimed;

    public Player(int playerID, String color) {
        this.playerID = playerID;
        this.color = color;
        this.permanentStepPositions = new ArrayList<>(Collections.nCopies(11, 0));
        this.columnsClaimed = new ArrayList<>();
    }

    public int getPlayerID() {
        return playerID;
    }

    public String getColor() {
        return color;
    }

    public ArrayList<Integer> getPermanentStepPositions() {
        return new ArrayList<>(permanentStepPositions);
    }

        public ArrayList<Integer> getColumnsClaimed() {
        return new ArrayList<>(columnsClaimed);
    }

    public void setPermanentStepPositions(ArrayList<Integer> positions) {
        if (positions == null || positions.size() != 11)
            throw new IllegalArgumentException("positions must have size = 11");
        this.permanentStepPositions = positions;
    }   

    public void setColumnsClaimed(ArrayList<Integer> columns) {
        if (columns == null)
            throw new IllegalArgumentException("columns cannot be null");
        this.columnsClaimed = columns;
    }

}
