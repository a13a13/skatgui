/**
 * general JNI player class
 * C++ library to be used is passed as a parameter
 * 
 * created by Jan Schaefer, 2007-11-06, updated 2011 by Michael Buro
 * licensed under GPLv3
 */
package client;

import common.*;

public class JNIPlayer extends Player
{
  /**
   * Constructor
   */
  public JNIPlayer(String library, String type, String params)
  {
    // load c++ player
    String path = System.getProperty("java.library.path");
    System.err.println("load <" + library + "> library-path=" + path);
    System.loadLibrary(library);
    System.err.println("loaded <" + library + "> library-path=" + path);

    this.name    = "JNIPlayer " + library + " " + type + " " + params;
    this.library = library;
    this.type    = type;
    this.params  = params;

    // ugly, I know ...
    // Alternatively, we could create a class for each jni player

    if (library.equals("xskatplayer"))
      obj = newxskatplayer(type, params);
    else if (library.equals("jniplayer") || library.equals("jniplayer32"))
      obj = newjniplayer(type, params);
    else if (library.equals("skattaplayer"))
      obj = newskattaplayer(type, params);
    else
      Misc.err("unknown jni player type: " + library);

    System.err.println(name + " instantiated");
  }

  // implemented in JNI player libraries

  // I couldn't figure out how to do this dynamically without having
  // to register jni players :-( When loading another library,
  // previous symbols are not replaced. Therefore, a generic newPlayer
  // function doesn't work.
  
  public static native long newxskatplayer (String type, String params);
  public static native long newjniplayer   (String type, String params);
  public static native long newskattaplayer(String type, String params);    

  // player independent

  public static native void deletePlayer(long obj);

  /**
   * JNI method stub
   *
   * @param obj
   *            c++ object address
   * @param simpleGameString
   *            SimpleGame serialized as string
   * @param remainingTime
   *            Remaining time for move calculation
   * @return Move
   */
  public static native String computeMove(long obj, String simpleGameString, double remainingTime);

  public static native void gameChange(long obj, String simpleGameString);  

  public static native void interruptMoveComputation(long obj);

  public static native void reset(long obj);
  
  /** DDS C++ interface 

      trump-games:

      return card point *differential* in view of player to move
      given window (alpha,beta)

      null-games:
      
      return > 0 if ptm wins, < 0 otherwise

      leaf values = +/- (100 + # declarer cards)
  */
  public static native
    int DDS(long obj,                          // address of underlying c++ player object
            int fh, int mh, int rh,            // see below ...
            int toMove,                        
            int declarer,                      
            int gameType,                      
            int trickCardNum,                  
            int trickCard1, int trickCard2,    
            int ptsDeclarer, int ptsDefenders, 
            int alpha, int beta,               
            int clearHash,
            int paranoid
            );
  
  public
    int DDS(int fh, int mh, int rh,            // remaining cards (bit sets)
            int toMove,                        // 0,1,2
            int declarer,                      // 0,1,2
            int gameType,                      // 0,1,2,3 (diamonds..clubs), 4 (grand), 5 (null)
            int trickCardNum,                  // number of cards in trick (0,1,2)
            int trickCard1, int trickCard2,    // trick card indexes
            int ptsDeclarer, int ptsDefenders, // card points thus far - including skat (0..120)
            int alpha, int beta,               // point *differential* bounds for ptm, ignored in null
            int clearHash,                     // !=0: clear hash table before searching
            int paranoid                       // UNUSED! 0: no paranoid, 1: paranoid, 2: unknown skat
            )
  {
    return DDS(obj, fh, mh, rh, toMove, declarer, gameType, trickCardNum, 
               trickCard1, trickCard2, ptsDeclarer, ptsDefenders, alpha, beta, clearHash, paranoid);
  }

  /** Paranoid table lookup C++ interface (10+2 cards) */

  // initialization
  public static native
    void paranoid_init(long object,     // address of c++ player object
                       String filename, // see below ...
                       int hand);       
  
  public void paranoid_init(String filename,   // table filename
                            int hand)          // 10-card hand
  {
    paranoid_init(obj, filename, hand);
  }
  
  /*
      return paranoid value:
         0: win
         1: schneider
         2: schwarz
         3: loss
  */
  public static native
    int paranoid(long obj,             // address of underlying c++ player object
                 int hand,             // see below ...
                 int skat,             
                 int gameType
                 );
  
  public
    int paranoid(int hand,             // hand bit set
                 int skat,             // skat bit set
                 int gameType          // 0,1,2,3 (diamonds..clubs), 4 (grand)
                 )
  {
    return paranoid(obj, hand, skat, gameType);
  }


  // world-paranoid search
  private static native
    int w_paranoid(long obj,                 // address of underlying c++ player object
                   int hand,                 // see below ...
                   int played,
                   int toMove,                        
                   int declarer,                      
                   int gameType,                      
                   int trickCardNum,                  
                   int trickCard1, int trickCard2,    
                   int ptsDeclarer, int ptsDefenders, 
                   int alpha, int beta,
                   int clearHash,
                   int[] hands1,
                   int[] hands2,
                   int[] skats
                   );
  
  public
    int w_paranoid(int declHand,                      // declarer's hand
                   int played,                        // played cards
                   int toMove,                        // 0,1,2
                   int declarer,                      // 0,1,2
                   int gameType,                      // 0,1,2,3 (diamonds..clubs), 4 (grand), 5 (null - not implemented)
                   int trickCardNum,                  // number of cards in trick (0,1,2)
                   int trickCard1, int trickCard2,    // trick card indexes
                   int ptsDeclarer, int ptsDefenders, // card points thus far - including skat (0..120)
                   int alpha, int beta,               // point *differential* bounds for ptm, ignored in null
                   int clearHash,                     // !=0: clear hash table before searching
                   // worlds (array sizes must match)
                   int[] hands1,                      // tomove+1's hands
                   int[] hands2,                      // tomove+2's hands
                   int[] skats                        // skats
                   )
  {
    return w_paranoid(obj, declHand, played, toMove, declarer, gameType,
                      trickCardNum, trickCard1, trickCard2,
                      ptsDeclarer, ptsDefenders, alpha, beta,
                      clearHash, hands1, hands2, skats);
  }

  //=====================================================================
  

  public void dispose()
  {
  }

  public void interrupt() {
    super.interrupt();
    interruptMoveComputation(obj);
  }
  
  public void resetHelper()
  {
    reset(obj);
  }

  /**
   * Call for the next move
   *
   * @param g Current game status
   * @param Remaining time
   * @return Next move
   */
  public String chooseMoveHelper(SimpleGame g, double remainingTime)
  {
    SimpleState s = g.getCurrentState();
    if (!s.isViewerToMove())
      Misc.err("chooseMove: viewer is not to move!");
    
    StringBuffer bf = new StringBuffer();
    g.serialize(g, bf);

    if (false) {
      System.out.println("\nJNI Game: \n" + g.toSgf(false, s.getToMove()));
      System.out.println("\nSerialized JNI Game: \n" + bf.toString());
      System.out.println("END OF SERIALIZED JNI GAME");
    }
    
    // calls the C++ method
    return computeMove(obj, bf.toString(), remainingTime);
  }
  
  /**
   * inform player about game state changes
   * 
   * @param g Current game status
   */
  public void gameChangeHelper(SimpleGame g)
  {
    StringBuffer bf = new StringBuffer();
    g.serialize(g, bf);
    
    // calls the C++ method
    gameChange(obj, bf.toString());
  }

  public void gameOverHelper(String gameHist)
  {
  }

  public void finalize()
  {
    deletePlayer(obj);
  }

  public String getLibrary() { return library; }
  public String getType()    { return type; }  
  public String getParams()  { return params; }  

  public String library, type, params;
  public long obj; // c++ object address
}
