package AI.Qlearning;

import AI.GameAction;
import AI.GameState;
import java.util.*;
import static java.lang.Math.max;


public class QLearningAgent {
    private QTable qTable;
    private Random random = new Random();

    private double alpha = 0.1;          
    private double gamma = 0.95;          
    private double epsilon = 0.3;        
    private double minEpsilon = 0.001;     
    private double epsilonDecayRate = 0.99995;  


    public QLearningAgent(QTable qTable){
        this.qTable = qTable;
    }
    

    public QLearningAgent(QTable.BucketingStrategy strategy){
        this(new QTable(strategy));
    }

    public GameAction chooseAction(GameState state){
        List<GameAction> availableActions = state.legalActions();
        if (availableActions.isEmpty()) return null;

        if (random.nextDouble() < epsilon) {
            return availableActions.get(random.nextInt(availableActions.size()));
        } else {
            return qTable.getBestAction(state, availableActions);
        }
    }
    public void epsilonDecay(){
        epsilon = max(minEpsilon, epsilon * epsilonDecayRate);
    }

    public double learn(GameState state, GameAction action, double reward, GameState nextState){
        double oldQ = qTable.getQValue(state, action);

        // Max Q for next state
        List<GameAction> nextPossible = nextState.legalActions();
        double maxNextQ = 0.0;

        if (!nextPossible.isEmpty()) {
            GameAction bestNext = qTable.getBestAction(nextState, nextPossible);
            maxNextQ = qTable.getQValue(nextState, bestNext);
        }

        // Bellman Equation
        double newQ = oldQ + alpha * (reward + (gamma * maxNextQ) - oldQ);
        qTable.setQValue(state, action, newQ);
        
        // Return Q-delta (magnitude of Q-value update)
        return Math.abs(newQ - oldQ);
    }

    public void setEpsilon(double e) { this.epsilon = e; }
    public void setGamma(double g) { this.gamma = g; }
    public void setAlpha(double a) { this.alpha = a; }
    public double getEpsilon() { return epsilon; }
    

    public void setBucketingStrategy(QTable.BucketingStrategy strategy) {
        qTable.setBucketingStrategy(strategy);
    }
    

    public QTable.BucketingStrategy getBucketingStrategy() {
        return qTable.getBucketingStrategy();
    }
}