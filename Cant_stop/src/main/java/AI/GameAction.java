package AI;

import java.io.Serializable;

public sealed interface GameAction extends Serializable permits GameAction.Stop, GameAction.Pairing, GameAction.Roll {

    record Stop() implements GameAction {}

    // player applies a dice pairing
    record Pairing(int sumA, int sumB) implements GameAction {
        public Pairing {
            if (sumA < 2 || sumA > 12 || sumB < 2 || sumB > 12)
                throw new IllegalArgumentException("Pairing sums must be from 2 to 12");
        }
    }

    record Roll() implements GameAction {}
}

