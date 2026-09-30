package AI.Testing;

import AI.GameAction;
import AI.GameState;
import AI.MCTS.MCTSAgent;
import AI.Qlearning.QLearningAgent;
import AI.Qlearning.QTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MctsVSQL {
    private static final Random rand = new Random();

    public static void main(String[] args){
        int[] iterations= linspace(1,2001,20);
        int games=50;
        int wintarget=3;

        for(int iteration:iterations){
            gameResults(iteration,games,wintarget);
        }
    }

    public static void gameResults(int mctsIterations,int games,int winTarget) {

        QTable trainedTable = new QTable();
        trainedTable.loadFromFile("cant_stop.dat");


        QLearningAgent qAgent = new QLearningAgent(trainedTable);
        qAgent.setEpsilon(0.0);


        MCTSAgent mctsAgent = new MCTSAgent(mctsIterations, 1.41, 25);

        int qWins = 0, mctsWins = 0, totalTurns = 0;

        for (int i = 1; i <= games; i++) {
            GameState state = GameState.newGame(2, winTarget);
            int turns = 0;

            while (!state.isTerminal()) {
                GameAction action;

                if (state.currentPlayer() == 0) {

                    action = qAgent.chooseAction(state);
                } else {
                    action = mctsAgent.chooseAction(state);
                }

                if (action == null) break;

                if (action instanceof GameAction.Roll) {
                    state = rollDice(state);
                } else {
                    state = state.apply(action);
                }
                turns++;
            }

            totalTurns += turns;
            if (state.terminalRewards()[0] == 1.0) {
                qWins++;
            } else {
                mctsWins++;
            }
        }

        // Final Output
        System.out.println("\n" + "=".repeat(30));
        System.out.printf("FINAL RESULTS (Against MCTS: %d)\n",mctsIterations);
        System.out.println("=".repeat(30));
        System.out.printf("Q-Learning Win Rate: %.2f%%\n", (qWins * 100.0 / games));
        System.out.printf("MCTS Win Rate:       %.2f%%\n", (mctsWins * 100.0 / games));
        System.out.printf("Average Turns:       %.2f\n", (totalTurns / (double) games));

    }

    private static GameState rollDice(GameState state) {
        int[] dice = new int[4];
        for (int i = 0; i < 4; i++) dice[i] = rand.nextInt(6) + 1;

        List<int[]> pairings = new ArrayList<>();
        pairings.add(new int[]{dice[0] + dice[1], dice[2] + dice[3]});
        pairings.add(new int[]{dice[0] + dice[2], dice[1] + dice[3]});
        pairings.add(new int[]{dice[0] + dice[3], dice[1] + dice[2]});

        return state.withPairingsAndDice(pairings, dice);
    }
    private static int[] linspace(int start, int end, int num) {
        int ratio= (int)(start+ end)/num;
        int[] line=new int[num+1];
        line[0]=start;
        for(int i=1;i<num+1;i++){
            line[i]=line[i-1]+ratio;
        }
        return line;
    }
}