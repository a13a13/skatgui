/** Sample client. Makes random moves.
 * (c) Michael Buro
 * licensed under GPLv3
 */

package client;

import common.*;
import java.util.*;

public class SamplePlayer extends Player
{
  RNG rng;

  public SamplePlayer() {
    setName("Sample");
    resetHelper();
  }

  public void resetHelper() {
    rng = new RNG();
    // rng.setSeed(1, 1);  // set a constant seed
    rng.setSeed((new Date()).getTime(), 1);
  }


  // this informs player about game state changes.
  // Not needed for minimal client that only reacts on move requests
  // See c++/src/sample/SamplePlayer.cpp for some examples.

  public void gameChangeHelper(SimpleGame g)
  {
    // do nothing
  }

  public void gameOverHelper(String gameHist) 
  {
    // do nothing    
  }

  public String chooseMoveHelper(SimpleGame g, double time) {
    String[] moves = new String[1000];
    int mn = g.getCurrentState().genMoves(moves);

    // Note: when in phase DISCARD_AND_DECL, genMoves() first only
    // generates the random game type.  With this, makeMove() will set
    // discardHalfPhase to true and the next genMoves() call will
    // generate all pairs of cards to discard (see below).

    // In code for non-random players, one should combine both moves
    // and emit strings of the form Type.Card.Card (eg. NO.CA.DA)
    
    // pick random move
    String bestMove = moves[rng.nextInt(mn)];

    String check = g.makeMove(g.getToMove(), bestMove, null);
    if (check != null) {
      Misc.err("Best move was illegal move.");
    }
    
    if (g.getCurrentState().getDiscardHalfPhase()) {

      String secondHalf = chooseMoveHelper(g, time);
      bestMove = bestMove + "." + secondHalf;
    }
    g.undoMove();

    return bestMove;
  }
}
