package AI.MCTS;

import AI.GameAction;
import AI.GameState;

import java.util.*;

public class MCTS {
    Node root=null;
    private Random rand = new Random();
    private final Rollout rolloutPolicy;

    public MCTS(Rollout rolloutPolicy) {
        this.rolloutPolicy = rolloutPolicy;
    }

    /**
     * Finds the best action to take from game state.
     * @param rootState starting state of the game.
     * @param iterations number of MCTS iterations to perform.
     * @param explorationFactor exploration hyperparameter.
     * @return best GameAction found by the algorithm.
     */
    public GameAction findBestAction(GameState rootState, int iterations, double explorationFactor) {
        Node root = new Node(rootState, null, null);
        for(int i=0;i<iterations;i++) {
            Node bestNode=SelectNode(root,explorationFactor);

            if(!bestNode.getState().isTerminal()){
                bestNode=expandNode(bestNode);
            }
            int playerForReward = root.getState().currentPlayer();
            double[] rewards = rolloutPolicy.simulate(bestNode.getState(), playerForReward);

            backpropagate(bestNode, rewards);
        }


        return getBestFinalAction(root);


    }

    /**
     * gives back a node with the best calculated uct or the current node
     * @param explorationFactor exploration hyperparameter.
     * @return bestNode node most fitted
     */
    public Node SelectNode(Node currentNode, double explorationFactor) {
        Node node = currentNode;
        while (node.isFullyExpanded() && !node.getState().isTerminal() && !node.getChildren().isEmpty()) {
            Node nextNode = node.selectBestChild(explorationFactor);
            if (nextNode == null) {
                break;
            }
            node = nextNode;
        }
        return node;
    }
    /**
     * @return a new child if it possible or the current node
     */
    public Node expandNode(Node node) {
        List<GameAction> legalActions = node.getState().legalActions();
        Collections.shuffle(legalActions);
        for (GameAction action : legalActions) {
            boolean alreadyChild = node.getChildren().stream()
                    .anyMatch(child -> child != null && child.getAction() != null && child.getAction().equals(action));

            if (!alreadyChild) {
                if (isProbabilisticAction(action)) {
                    ChanceNode chance = new ChanceNode(node.getState());
                    GameState randomOutcome = chance.sampleOutcome(); // Pick ONE
                    Node outcomeChild = new Node(randomOutcome, node, action);
                    node.addChild(outcomeChild);
                    return outcomeChild;
                } else {
                    GameState nextState = node.getState().apply(action);
                    Node newChild = new Node(nextState, node, action);
                    node.addChild(newChild);
                    return newChild;
                }
            }

        }
        return node;
    }

    /**
     * backpropagate rewards to the root
     */
    private void backpropagate(Node node, double[] rewards) {
        Node tempNode = node;
        while (tempNode != null) {
            tempNode.updateStats(rewards);
            tempNode = tempNode.getParent();
        }
    }

    /**
     * Decide on the action it is going to take
     */
    private GameAction getBestFinalAction(Node root) {
        if (root.getChildren().isEmpty()) {
            List<GameAction> legalActions = root.getState().legalActions();
            if (legalActions.isEmpty()) {
                return null; // No possible moves
            }
            return legalActions.get(0);
        }

        Map<GameAction, Integer> visitsPerAction = new HashMap<>();
        for (Node child : root.getChildren()) {
            if (child != null && child.getAction() != null) {
                visitsPerAction.merge(child.getAction(), child.getVisitCount(), Integer::sum);
            }
        }

        GameAction bestAction = null;
        int maxVisits = -1;

        for (Map.Entry<GameAction, Integer> entry : visitsPerAction.entrySet()) {
            if (entry.getValue() > maxVisits) {
                maxVisits = entry.getValue();
                bestAction = entry.getKey();
            }
        }

        return bestAction;
    }



    /**
     * helpers method for propabilistic action
     */
    private boolean isProbabilisticAction(GameAction action) {
        return action instanceof GameAction.Roll;
    }
}

