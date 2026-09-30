package AI.Testing;

import AI.MCTS.ChanceNode;
import AI.GameAction;
import AI.GameState;
import AI.MCTS.MCTSAgent;

public class MCTSvsMCTS {

    public static class TestResult {
        public final int[] winCounts;
        public final int gamesPlayed;

        public TestResult(int[] winCounts, int gamesPlayed) {
            this.winCounts = winCounts;
            this.gamesPlayed = gamesPlayed;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Test Results (" + gamesPlayed + " games):\n");
            for (int i = 0; i < winCounts.length; i++) {
                sb.append("Agent ").append(i).append(": ")
                        .append(winCounts[i]).append(" wins\n");
            }
            return sb.toString();
        }
    }

    public static TestResult runTests(int numGames, int numMCTS, int winTarget, MCTSAgent... agents) {
        if (agents.length != numMCTS) {
            throw new IllegalArgumentException("number of agents must be equal to stated");
        }

        int[] winCounts = new int[numMCTS];

        for (int i = 0; i < numGames; i++) {
            GameState state = GameState.newGame(numMCTS, winTarget);
            int winner = -1;

            while (!state.isTerminal()) {
                int currentPlayerIndex = state.currentPlayer();
                MCTSAgent currentAgent = agents[currentPlayerIndex];

                GameAction action = currentAgent.chooseAction(state);
                if (action instanceof GameAction.Roll) {

                    state = new ChanceNode(state).sampleOutcome();
                } else {
                    state = state.apply(action);
                }

                if (state.isTerminal()) {
                    double[] rewards = state.terminalRewards();
                    for (int p = 0; p < numMCTS; p++) {
                        if (rewards[p] > 0) {
                            winner = p;
                            break;
                        }
                    }
                }
            }
            if (winner != -1) {
                winCounts[winner]++;
                System.out.printf("Finished:NumGames: %-5d \n",i);
            }
        }
        return new TestResult(winCounts, numGames);
    }
}