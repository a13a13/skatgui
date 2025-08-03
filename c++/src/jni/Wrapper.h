#ifndef Wrapper_H
#define Wrapper_H

// C++ wrapper for the SimpleGame Java class
// which contains all skat game information

// (c) Michael Buro, licensed under GPLv3

#include <iostream>
#include <sstream>
#include <vector>
#include <cstdlib>
#include <cassert>
//#include "../shared/Global.h"

namespace Wrapper
{
  static bool expect(const std::string &what, std::istream &is)
  {
    std::string t;
    is >> t;
    
    //std::cout << "read " + t << std::endl;

    if (t == what+"Null") return true;
  
    if (t != what) {
      std::cerr << "ERROR: " << __FILE__ << " " << __FUNCTION__ << "() (line " << __LINE__ << "): "
                << "SimpleGame: expected " << what << ", but got " << t << std::endl;
      exit(-1);
    }

    return false;
  }

  static void read(std::istream &is, int &i)
  {
    std::string t;
    is >> t;
    std::istringstream iss(t);
    iss >> i;
    if (!is) {
      std::cerr << "ERROR: " << __FILE__ << " " << __FUNCTION__ << "() (line " << __LINE__ << "): "
                << "read int error: " << t << std::endl;
      exit(-1);
    }
    //std::cout << "int: " << i << std::endl;
  }

//   static void read(std::istream &is, uint1 &u)
//   {
//     int i;
//     is >> i;
//     if (!is) ERR("read uint1 error");
//     u = i;
//     //std::cout << "uint1: " << u << std::endl;
//   }

  static void read(std::istream &is, bool &b)
  {
    std::string s;
    is >> s;
    if (s == "true")
      b = true;
    else if (s == "false")
      b = false;
    else {
      std::cerr << "ERROR: " << __FILE__ << " " << __FUNCTION__ << "() (line " << __LINE__ << "): "
                << "true/false expected, but got " << s << std::endl;
      exit(-1);
    }
  
    //std::cout << "bool: " << b << std::endl;
  }

  static void read(std::istream &is, std::string &s)
  {
    is >> s; 
    //std::cout << "string: " << s << std::endl;
  }


  struct Move
  {
    int source;         // The player who made the move (-1: world, 0-2)
    std::string action; // the player action

    static Move *deserialize(std::istream &is)
    {
      if (expect("Move", is)) return 0;    

      Move* m = new Move;
      read(is, m->source);
      read(is, m->action);
      return m;
    }
  };


  struct Card
  {
    static const int SUIT_NONE     = -1;
    static const int SUIT_DIAMONDS = 0;
    static const int SUIT_HEARTS   = 1;
    static const int SUIT_SPADES   = 2;
    static const int SUIT_CLUBS    = 3;

    int suit;
    int rank; // 0..7: 789TJQKA

    Card() { suit = rank = -1; }

    static Card *deserialize(std::istream &is)
    {
      if (expect("Card", is)) return 0;
      Card* c = new Card;
      read(is, c->suit);
      read(is, c->rank);
      return c;
    }
  };


  struct GameDeclaration
  {
    // game type
    static const int NO_GAME = Card::SUIT_NONE;
    static const int DIAMONDS_GAME = Card::SUIT_DIAMONDS;
    static const int HEARTS_GAME = Card::SUIT_HEARTS;
    static const int SPADES_GAME = Card::SUIT_SPADES;
    static const int CLUBS_GAME = Card::SUIT_CLUBS;
    static const int GRAND_GAME = Card::SUIT_CLUBS + 1;
    static const int NULL_GAME = Card::SUIT_CLUBS + 2;
    
    int type;
    bool hand, ouvert, schneiderAnnounced, schwarzAnnounced;

    static GameDeclaration *deserialize(std::istream &is)
    {
      if (expect("GameDeclaration", is)) return 0;
    
      GameDeclaration *gd = new GameDeclaration;
    
      read(is, gd->type);
      read(is, gd->hand);
      read(is, gd->ouvert);
      read(is, gd->schneiderAnnounced);
      read(is, gd->schwarzAnnounced);
    
      return gd;
    }
  };


  struct SimpleState
  {
    // View / toMove
    static const int FORE_HAND = 0;
    static const int MIDDLE_HAND = 1;
    static const int REAR_HAND = 2;
    static const int PUBLIC_VIEW = 3;
    static const int WORLD_VIEW = 4;

    // Phase
    static const int DEAL = 0;
    static const int BID  = 1;
    static const int ANSWER = 2;
    static const int SKAT_OR_HAND_DECL = 3;
    static const int GET_SKAT = 4;
    static const int DISCARD_AND_DECL = 5;
    static const int CARDPLAY = 6;
    static const int FINISHED = 7;

    // Player information
    
    struct PlayerInfo {
      int tricksWon;    // #tricks won so far
      int trickPoints;  // #points won so far
      int maxBid;       // maximum bids, 0=no info, 1=passed at 18, >=18 held that bid
      int resigned;     // true for all players who resigned
      int hand;       // current player hand
      int playedCards; // cards played so far by player
      int voids;       // flags: 1<<suit, 1<<5 = jacks (in trump games)
    };
    
    int view;             // state view: see View constants above
    int phase, prevPhase; // current and previous game phase
    int declarer;         // >=0 means bidding is over
    // (doesn't mean there is a game declaration yet! decl.type != NO_GAME indicates this)
    int maxBid;           // current maximum bid
    int nextBid;          // next bid value (-1 for none available)
    int asked;            // player asked in bidding phase
    int toMove;           // player who is to move
    int bidder;           // who is bidding in bidding phase?
    int trickWinner;      // who won previous trick
    int left, timeout;    // >= 0: player left or exceeded time
    Card* winningCard;    // which card won previous trick
    GameDeclaration* decl;// current game declaration 
    int declarerHandBeforeCardplay; // needed in declResult
    PlayerInfo pinfos[3];  // information about individual players
    Card *skat0, *skat1;   // original skat or discarded cards
    Card *trick0, *trick1, *trick2; // current trick
    int trickCardNum;               // number of cards in current trick

    SimpleState() {
      winningCard = skat0 = skat1 = trick0 = trick1 = trick2 = 0;
      decl = 0;
    }
    
    static SimpleState *deserialize(std::istream &is)
    {
      if (expect("SimpleState", is)) return 0;

      SimpleState *s = new SimpleState;
    
      read(is, s->view);
      read(is, s->phase);
      read(is, s->prevPhase);
      read(is, s->declarer);
      read(is, s->maxBid);
      read(is, s->nextBid);
      read(is, s->asked);
      read(is, s->toMove);
      read(is, s->bidder);
      read(is, s->trickWinner);
      read(is, s->left);
      read(is, s->timeout);
      s->winningCard = Card::deserialize(is);
      s->decl = GameDeclaration::deserialize(is);
      read(is, s->declarerHandBeforeCardplay);

      for (int i = 0; i < 3; ++i) {
        PlayerInfo &pi = s->pinfos[i];
	read(is, pi.tricksWon);
	read(is, pi.trickPoints);
	read(is, pi.maxBid);
	read(is, pi.resigned);
	read(is, pi.hand);
        read(is, pi.playedCards);
	read(is, pi.voids);        
      }

      s->skat0 = Card::deserialize(is);
      s->skat1 = Card::deserialize(is);    

      read(is, s->trickCardNum);
      
      s->trick0 = s->trick1 = s->trick2 = 0;
      
      if (s->trickCardNum >= 1) {
        s->trick0 = Card::deserialize(is);
        if (s->trickCardNum >= 2) {
          s->trick1 = Card::deserialize(is);          
          if (s->trickCardNum >= 3) {
            s->trick2 = Card::deserialize(is);
          }
        }
      }
      
      return s;
    }

    ~SimpleState()
    {
      delete winningCard;
      delete decl;
      delete skat0;
      delete skat1;
      delete trick0;
      delete trick1;
      delete trick2;      
    }
  };


  struct SimpleGame
  {
    std::vector<Move*> moves;
    std::vector<SimpleState*> stateHist;

    static SimpleGame *deserialize(std::istream &is)
    {
      if (expect("SimpleGame", is)) return 0;

      SimpleGame *sg = new SimpleGame;
      int n;

      // move vector

      expect("MoveVector", is); read(is, n);

      for (int i = 0; i < n; ++i) {
	sg->moves.push_back(Move::deserialize(is));
      }

      // state vector

      expect("SimpleStateVector", is); read(is, n);

      for (int i = 0; i < n; ++i) {
	sg->stateHist.push_back(SimpleState::deserialize(is));
      }
      
      return sg;
    }

    ~SimpleGame()
    {
      for (size_t i = 0; i < moves.size(); ++i) delete moves[i];
      for (size_t i = 0; i < stateHist.size(); ++i) delete stateHist[i];
      //      FORALL (moves, i) delete *i;
      //      FORALL (stateHist, s) delete *s;
    }
  };
  
}
#endif
