package AI.Qlearning;

import AI.GameAction;
import AI.GameState;
import AI.MCTS.MCTSAgent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MctsTraining {

    private static final Random random = new Random();

    public static void main(String[] args) {

        String rewardConfig = Rewards.getRewardConfigString();
        String qTableFile = "cant_stop_mcts_trained_" + rewardConfig + ".dat";

        QTable table = new QTable();
        table.loadFromFile(qTableFile);
        QLearningAgent qAgent = new QLearningAgent(table);

        MCTSAgent mctsOpponent = new MCTSAgent(100, 1.41,25);




        System.out.println("Starting Training: 1x Q-Learning vs 3x MCTS (100 iterations)");

        for (int episode = 1; episode <= 1000; episode++) {
            GameState state = GameState.newGame(4, 3);

            while (!state.isTerminal()) {
                int currentPlayer = state.currentPlayer();
                GameAction action;


                if (currentPlayer == 0) {
                    action = qAgent.chooseAction(state);
                } else {
                    action = mctsOpponent.chooseAction(state);
                }

                if (action == null) break;

                GameState nextState;
                if (action instanceof GameAction.Roll) {
                    nextState = simulateDiceRoll(state);
                } else {
                    nextState = state.apply(action);
                }


                if (currentPlayer == 0) {
                    boolean busted = EdgeCaseHandler.didBust(state, nextState);
                    double reward = Rewards.calculateReward(state, nextState, busted);


                    if (nextState.isTerminal()) {
                        reward += nextState.terminalRewards()[0] * 10.0; // Boosted win/loss signal
                    }

                    qAgent.learn(state, action, reward, nextState);
                }

                state = nextState;
            }

            qAgent.epsilonDecay();


            System.out.printf("Episode %d/%d", episode, 1000);
            table.saveToFile(qTableFile);

        }

        table.saveToFile(qTableFile);
        System.out.println("Training Complete.");
    }

    private static GameState simulateDiceRoll(GameState state) {
        int[] dice = new int[4];
        for (int i = 0; i < 4; i++) dice[i] = random.nextInt(6) + 1;

        List<int[]> pairings = new ArrayList<>();
        pairings.add(new int[]{dice[0] + dice[1], dice[2] + dice[3]});
        pairings.add(new int[]{dice[0] + dice[2], dice[1] + dice[3]});
        pairings.add(new int[]{dice[0] + dice[3], dice[1] + dice[2]});

        return state.withPairingsAndDice(pairings, dice);
    }
}