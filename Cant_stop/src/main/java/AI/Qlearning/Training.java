package AI.Qlearning;

import AI.GameAction;
import AI.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Training {


    private static final Random random = new Random();


    public static void main(String[] args) {

        String rewardConfig = Rewards.getRewardConfigString();
        String qTableFile = "cant_stop.dat";
        
        QTable table = new QTable();

        table.loadFromFile(qTableFile);

        QLearningAgent agent = new QLearningAgent(table);

        GameState newState;
        double reward;
        boolean busted;

        for (int episode = 0; episode < 1000000; episode++) {
            GameState startState = GameState.newGame(4, 3);


            while(!startState.isTerminal()) {

                GameAction action = agent.chooseAction(startState);

                if (action instanceof GameAction.Roll) {
                    int[] dice = new int[4];
                    for (int i = 0; i < 4; i++) dice[i] = random.nextInt(6) + 1;

                    List<int[]> pairings = new ArrayList<>();
                    pairings.add(new int[]{dice[0] + dice[1], dice[2] + dice[3]});
                    pairings.add(new int[]{dice[0] + dice[2], dice[1] + dice[3]});
                    pairings.add(new int[]{dice[0] + dice[3], dice[1] + dice[2]});

                    newState = startState.withPairingsAndDice(pairings, dice);
                } else {
                    newState = startState.apply(action);
                }
                busted = EdgeCaseHandler.didBust(startState, newState);

                reward = Rewards.calculateReward(startState, newState, busted);
                agent.learn(startState, action, reward, newState);
                startState = newState;
                }

            System.out.println("Episode" + episode + "complete");
            }




        table.saveToFile(qTableFile);

            }

        }