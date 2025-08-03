// $Id$
// (c) Michael Buro, licensed under GPLv3

// player base class

// for java players context arguments are irrelevant. For c++ players,
// context is used to distinguish player incarnations (needed because
// jni functions are static)

package client;

import common.*;


public abstract class Player
{
  String name;
  boolean interrupted; // true => player must stop move computation
  String context = ""; // for c++ to differentiate players

  SimpleGame currentGame; // The last game on which the player was working
  int pos;                // player position in game

  public Player() { pos = -1; }
  
  public void setContext(String c) { context = c; } // for c++
  public String getContext() { return context; } // for c++

  public SimpleGame getCurrentGame() { return currentGame; }
  public int getPos() { return pos; }  
  
  public void interrupt() { interrupted = true; }
  public boolean isInterrupted() { return interrupted; }

  /** indicate that we don't need that player anymore 
      JNIPlayer informs c++ side
  */
  public void dispose() {
    interrupt();
  }

  /** resets player to accept new game, also sets player pos */
  public void reset(int pos)
  {
    if (pos < 0 || pos > 2) Misc.err("invalid pos");
    this.pos = pos;
    currentGame = new SimpleGame(pos);
    resetHelper();
  }
  
  /** inform player that the game state has changed, called whenever
   * the game state is changed (with move and state appended). Note
   * that the last state in currentGame is the state *after* applying
   * action.
   */
  public void gameChange(int player, String action) {
    String m = currentGame.makeMove(player, action, null);
    if (m != null) {
      Misc.msg("Move was: " + action);
      Misc.msg("player was: " + player);
      Misc.msg(m);
      Misc.msg(currentGame.getCurrentState().toString());
      Misc.err("gameChange: invalid move");
    }
    gameChangeHelper(currentGame);
  }

  /** replay game in pos's view
      @return null if OK, error message otherwise
  */
  public String replay(SimpleGame game, int pos)
  {
    reset(pos);

    for (int i = 0; i < 3; i++) {
      currentGame.setPlayerName(i, game.getPlayerName(i));
    }

    SimpleGame newg = new SimpleGame(pos);
    
    String res = newg.replay(game, pos);
    if (res != null) return res;

    // go through move list and call gameChange

    for (Move m : newg.getMoveHist()) {
      gameChange(m.source, m.action);
    }

    return null;
  }

  
  /** ask player to compute a move assuming a continuation in the game
   * from the last time it was called */
  public String chooseMove(double remainingTime)
  {
    // Misc.msg(currentGame.toSgf(false, currentGame.getOwner()));
    
    Misc.msg("Choosing move! Player: " + name);
    if (currentGame.getCurrentState().getToMove() != pos)
      Misc.err("chooseMove: position in game has changed! was: " + pos +
               " tm=" + currentGame.getCurrentState().getToMove());
    
    return chooseMoveHelper(currentGame, remainingTime);
  }

  /** Overloading to deal with some silly problems with hijacking the jni player 
  */
  public String chooseMove(SimpleGame g, double remainingTime)
  {
    Misc.msg("Choosing move! Player: " + name);
    //if (currentGame == null ||
//	currentGame.getCurrentState().getToMove() != pos)
   //   Misc.err("chooseMove: position in game has changed!");
    
    return chooseMoveHelper(g, remainingTime);
  }

  /*  Inform the player that the game is over.
  */
  public void gameOver(String gameHist) {
    gameOverHelper(gameHist);
  }

  // Allow individual players to do what they wish with the game history
  public abstract void gameOverHelper(String gameHist);

  public String getName() { return name; }

  public void setName(String n) { name = n; }

  // to be implemented in subclass
  
  /** actually choose the move, given a SimpleGame. */
  public abstract String chooseMoveHelper(SimpleGame g, double remainingTime);

  /** inform player about game change */
  public abstract void gameChangeHelper(SimpleGame g);

  /** inform player about game change */
  public abstract void resetHelper();


  /** a single player finishes game playing all sides
      assume game view = WORLD
   */
  public static void finishGame(SimpleGame g, Player pl, double time)
  {
    assert g.getOwner() == SimpleState.WORLD_VIEW;
    
    // Misc.msg("finishgame: " + g.toSgf(false, g.getOwner())); // !!!
    
    SimpleState st = g.getCurrentState();
    if (st.getPhase() == SimpleState.DEAL) Misc.err("empty game");
    String[] cardMoves = new String[10];
    
    while (!g.isFinished()) {

      st = g.getCurrentState();
      int toMove = st.getToMove();
      String mv = null;
      
      if (toMove > 2) {

        // world move
        mv = st.generateWorldMove(null);

      } else {

        if (st.getPhase() == SimpleState.CARDPLAY &&
            st.genMoves(cardMoves) == 1) {
          // only one card: play it
          mv = cardMoves[0];
        } else {

          // compute player move
          //Misc.msg("CHOOSE MOVE TM= " + toMove);

          String res = pl.replay(g, toMove);
          if (res != null) {
            Misc.err("ouch " + res);
          }
          mv = pl.chooseMove(time);
        }
      }
      
      String res = g.makeMove(toMove, mv, null);
      if (res != null) {
        Misc.msg(g.toSgf(false, toMove));
        Misc.err("oops : " + res);
      }
    }
  }
}
