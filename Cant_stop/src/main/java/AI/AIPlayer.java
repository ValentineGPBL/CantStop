package AI;

public interface AIPlayer {
    GameAction chooseAction(GameState state);
    int getPlayerIndex();
}
