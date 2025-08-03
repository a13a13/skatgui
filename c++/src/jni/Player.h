#ifndef Player_H
#define Player_H

#include <string>
#include "Wrapper.h"

class Player
{
public:

  Player()
  {
    interrupted = false;
  }

  // signal move computation to exit
  virtual void interruptMoveComputation() { interrupted = true; }

  // reset game
  virtual void reset() = 0;

  /** inform player about game state changes
      @param move_index = -1: last move in sequence, otherwise that move (for easy replay)
  */
  virtual void gameChange(const Wrapper::SimpleGame &game, int move_index = -1) = 0;

  // return move in current game state
  virtual std::string computeMove(const Wrapper::SimpleGame &game, double time) = 0;


  /** DDS C++ interface (C++ is 3+ times faster)

      trump-games:

      return card point *differential* in view of player to move
      given window (alpha,beta)

      null-games:
      
      return > 0 if ptm wins, < 0 otherwise

      leaf values = +/- (100 + # declarer cards)
  */

  virtual int DDS(int fh, int mh, int rh,            // remaining cards (bit sets)
                  int toMove,                        // 0,1,2
                  int declarer,                      // 0,1,2
                  int gameType,        // 0,1,2,3 (diamonds..clubs), 4 (grand), 5 (null)
                  int trickCardNum,                  // number of cards in trick (0,1,2)
                  int trickCard1, int trickCard2,    // trick card indexes
                  int ptsDeclarer, int ptsDefenders, // card points thus far -
                                                     // including skat (0..120)
                  int alpha, int beta,               // point *differential* bounds for ptm
                                                     // (ignored in null-games)
                  int clearHash,                     // !=0: clear hash table before searching
                  int paranoid                       // 0: not paranoid, 2: unknown skat
                  ) = 0;

  /** Paranoid C++ interface */

  virtual void paranoid_init(const char *file,  // table filename
                             int hand           // 10-card hand
                             ) = 0;
  /*
    return paranoid value:
    0: win
    1: schneider
    2: schwarz
    3: loss
  */
  virtual int paranoid(int hand,     // hand bit set
                       int skat,     // skat bit set
                       int gameType  // 0,1,2,3 (diamonds..clubs), 4 (grand)
                       ) = 0;


  // paranoid search on a set of worlds
  virtual int w_paranoid(int declHand,                      // declarer's hand
                         int played,                        // all played moves
                         int toMove,                        // 0,1,2
                         int declarer,                      // 0,1,2
                         int gameType,                      // 0,1,2,3 (diamonds..clubs), 4 (grand), 5 (null - not implemented)
                         int trickCardNum,                  // number of cards in trick (0,1,2)
                         int trickCard1, int trickCard2,    // trick card indexes
                         int ptsDeclarer, int ptsDefenders, // card points thus far - including skat (0..120)
                         int alpha, int beta,               // point *differential* bounds for ptm, ignored in null
                         int clearHash,                     // !=0: clear hash table before searching
                         // worlds (all vectors same size)
                         int n,                             // number of worlds
                         int *hands1,                       // n tomove+1's hands
                         int *hands2,                       // n tomove+2's hands
                         int *skats                         // n skats
                         ) = 0;
  
  virtual ~Player() { }

protected:

  bool isInterrupted() { return interrupted; }

  bool interrupted;
};

#endif
