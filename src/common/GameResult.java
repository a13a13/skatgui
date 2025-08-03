// $Id$
// (c) Michael Buro, licensed under GPLv3

package common;

/** declarer game result */
public class GameResult implements Cloneable
{
  public boolean unknown;
  public boolean passed;
  public boolean penalty;
  public int declValue;
  public int matadors;
  public boolean schneider;
  public boolean schwarz;
  public boolean overbid;
  public int declarer, declCardPoints, declTricks, left, timeout;
  public int penalty0, penalty1, penalty2;
  public boolean resigned; // one party resigned
  
  public GameResult()
  {
    init();
  }

  public void init()
  {
    unknown = true;
    passed = false;
    penalty = false;
    declValue = 0;
    matadors = 0;
    schneider = schwarz = overbid = false;
    declarer = -1;
    declCardPoints = declTricks = left = timeout = 0;
    penalty0 = penalty1 = penalty2 = 0;
    resigned = false;
  }

  public int getPenalty(int i)
  {
    if (i == 0) return penalty0;
    if (i == 1) return penalty1;
    return penalty2;
  }
  
  public void setPenalty(int i, int value)
  {
    if (i == 0) { penalty0 = value; return; }
    if (i == 1) { penalty1 = value; return; }
    penalty2 = value;
  }
  
  public String toString()
  {
    if (unknown)
      return "unknown";
      
    if (passed) {
      return "passed";
    }

    return "d:"+declarer + (penalty ? " penalty" : (declValue > 0 ? " win" : " loss"))
      + " v:" + declValue
      + " m:" + matadors + (overbid ? " overbid" : " bidok")
      + " p:" + declCardPoints + " t:" + declTricks
      + " s:" + (schneider ? '1' : '0') + " z:" + (schwarz ? '1' : '0')
      + " p0:" + penalty0 + " p1:" + penalty1 + " p2:" + penalty2 
      + " l:" + this.left + " to:" + this.timeout + " r:" + (resigned ? '1' : '0');
  }


  /*  
  static public GameResult copy(GameResult x)
  {
    GameResult gr = null;

    try {
      gr = (GameResult)x.clone();
    }
    catch (CloneNotSupportedException e) {
      Misc.exception(e);
    }

    gr.penalty0 = x.penalty0;
    gr.penalty1 = x.penalty1;
    gr.penalty2 = x.penalty2;    
    return gr;
  }
  */
}
