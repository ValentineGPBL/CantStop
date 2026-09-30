package AI.MCTS;

import AI.GameAction;
import AI.GameState;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private GameState state;
    private Node parent;
    private List<Node> children;
    private GameAction action; // The action that led to this node

    private int visitCount;
    private double[] winScores;
    private final int playerCount;


    public Node(GameState state, Node parent, GameAction action) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.children = new ArrayList<>();
        this.playerCount = state.getPlayerCount();
        this.winScores = new double[this.playerCount];
    }

    /**
     * @return True if all actions have child nodes.
     */
    public boolean isFullyExpanded() {
        return children.size() == state.legalActions().size();
    }

    /**
     * @param explorationFactor The exploration constant.
     * @return The child node with the highest score.
     */
    public Node selectBestChild(double explorationFactor) {
        Node bestChild=null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int currentPlayer = this.state.currentPlayer();

        for (Node child:children){
            if (child.getVisitCount() == 0) {
                return child;
            }
            double exploitation = child.winScores[currentPlayer] / child.getVisitCount();
            double exploration = explorationFactor * Math.sqrt(Math.log(this.getVisitCount()) / child.getVisitCount());
            double childScore = exploitation + exploration;

            if (childScore > bestScore){
                bestScore = childScore;
                bestChild = child;
            }
        }
        return bestChild ;
    }

    /**
     * @param rewards used in backpropagation
     */
    public void updateStats(double[] rewards) {
        visitCount++;
        for (int i = 0; i < playerCount; i++) {
            winScores[i] += rewards[i];
        }
    }

    /**
     * @param child The child node to add.
     */
    public void addChild(Node child) {
        this.children.add(child);
    }


    /**
     * Getters
     */
    public GameState getState() {return state;}
    public Node getParent() {return parent;}
    public List<Node> getChildren() {return children;}
    public GameAction getAction() {return action;}
    public int getVisitCount() {return visitCount;}
    public double[] getWinScores() { return winScores; }
}