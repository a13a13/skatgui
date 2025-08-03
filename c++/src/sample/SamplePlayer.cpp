/** Sample player

    (c) Michael Buro
    licensed under GPLv3
*/

#include <sstream>
#include "../jni/Player.h"
#include "SamplePlayer.h"

using namespace std;

// implement new-function here ...


SamplePlayer::SamplePlayer(const std::string &playerType, const std::string &params)
{
  cout << "construct SamplePlayer " << playerType << " " << params << endl;
}

SamplePlayer::~SamplePlayer()
{
}

void SamplePlayer::reset()
{
}

void SamplePlayer::interrupt() {
  cout << "fixme: implement interrupt" << endl; exit(0);
}

/**
 * Handles one state change (prev state, move -> new state)
 */
void SamplePlayer::gameChange(const Wrapper::SimpleGame &sg, int move_index)
{
  size_t mn = sg.moves.size();
  assert(mn >= 1 && sg.stateHist.size() == mn+1);
  assert(move_index >= -1 && move_index < (int)mn);
  if (move_index < 0) move_index = mn-1;
  
  const Wrapper::SimpleState &prev_state = *sg.stateHist[move_index];
  const Wrapper::Move        &move       = *sg.moves[move_index];
  const Wrapper::SimpleState &new_state  = *sg.stateHist[move_index+1];

  cout << "game change " << prev_state.phase << " move " << move.action << endl;
	
  if (prev_state.phase == prev_state.DEAL) {
		
    cout << "SamplePlayer: game change, deal move" << endl;

    // inform the player about the start of the game
    // 
    // e.g. setCards(new_state);
    
  } else if (prev_state.phase == prev_state.BID) {
		
    cout << "SamplePlayer: game change bid" << endl;

    // checkme: no action?

  } else if (prev_state.phase == prev_state.ANSWER) {
		
    cout << "SamplePlayer: game change answer "
         << prev_state.maxBid << " " << prev_state.nextBid << " "
         << prev_state.asked << " " << prev_state.toMove << " "
         << prev_state.bidder
         << endl;

    // checkme: no action?    

  } else if (prev_state.phase == prev_state.SKAT_OR_HAND_DECL) {
		
    cout << "SamplePlayer: game change SKAT_OR_HAND_DECL move" << endl;

    if (new_state.phase == new_state.CARDPLAY) {

      // hand game

      cout << "hand game announcement" << endl;
      assert(new_state.decl->hand);
      
      //player->startGame(prev_state.view, getGameTypeIDISS2SamplePlayer(new_state.decl->type),
      //                        new_state.declarer, 0, new_state.decl->hand, new_state.decl->ouvert);
      // cardPlayStarted = true;
      // cardInTrick = 0;
    }

  } else if (prev_state.phase == prev_state.GET_SKAT) {  

    cout << "SamplePlayer: game change GET_SKAT move" << endl;

    // world move: reveal skat if player is declarer
    
    if (prev_state.declarer == prev_state.view) {
      //...
    }

  } else if (prev_state.phase == prev_state.DISCARD_AND_DECL) {
		
    cout << "SamplePlayer: game change DISCARD_AND_DECL move" << endl;

    if (prev_state.declarer == prev_state.view) {
    
      // discard 2 cards
      
      // ...
    }

    //...

  } else if (prev_state.phase == prev_state.CARDPLAY) {
		
    cout << "SamplePlayer: CARDPLAY" << endl;
    cout << "state.view move.source state.toMove" << endl;
    cout << prev_state.view << " " << move.source << " " << prev_state.toMove << endl;
		
    if (move.action.length() > 2) {

      // fixme: implement
      // SC.XX.XX.XX.XX.XX.XX.XX.XX (declarer shows cards)

      cout << "SamplePlayer: declarer shows cards. Handle it!" << endl;

    } else if (move.action == "RE") {

      if (prev_state.declarer != prev_state.view) {
      
        // RE was played by partner, resign too
        // resignGame = true;
      }
      
    } else {

      cout << "card played" << endl;
      cout << "SamplePlayer lastCard: " << move.action << endl;

      // ...
    }

  } else {
		
    cout << "Unhandled game phase..." << endl;
    exit(10);
  }
}

/**
 * Computes the next move
 * fixme: make this interruptible
 */
std::string SamplePlayer::computeMove(const Wrapper::SimpleGame &sg, double /*time*/)
{
  cout << "SamplePlayer: computeMove()" << endl;

  if (sg.stateHist.size() <= 1) {
    cout << "SamplePlayer: ERROR nothing dealt!" << endl;
    return "error-nodeal";
  }
  
  const Wrapper::SimpleState &state = *sg.stateHist[sg.stateHist.size() - 1];
  // Wrapper::Move *move = 0;

  if (state.toMove != state.view) {
    cout << "SamplePlayer: ERROR player to move " << state.toMove << " != viewer " << state.view << endl;
    return "error-tomove";
  }

  string result;
  
  if (state.phase == state.BID) {
		
    cout << "SamplePlayer: bidding maxBid asked toMove bidder" << endl;
    cout << state.maxBid << " " << state.asked << " " << state.toMove << " " << state.bidder << endl;
    
    if (true /*declareBid(state.nextBid) */) {
      
      ostringstream s;
      s << state.nextBid;
      result = s.str();
      
    } else {
      
      result = "p"; // pass
    }

  } else if (state.phase == state.ANSWER) {
		
    cout << "SamplePlayer: ANSWER maxBid nextBid asked toMove bidder" << endl;
    cout << state.maxBid << " " << state.nextBid << " " << state.asked << " " << state.toMove
         << " " << state.bidder << endl;
		
    if (true /*holdBid(state.maxBid)*/ ) {
			
      result = "y"; // yes

    } else {
			
      result = "p"; // pass
    }

  } else if (state.phase == state.SKAT_OR_HAND_DECL) {
		
    cout << "SamplePlayer: SKAT_OR_HAND_DECL" << endl;
		
    if (true /*lookIntoSkat()*/) {
			
      result = "s";
      // lookIntoSkat = true;

    } else {
			
      // announce a hand game
      result = "DH"; /*announceGame(true);*/
    }

  } else if (state.phase == state.DISCARD_AND_DECL) {
		
    cout << "SamplePlayer: DISCARD_AND_DECL" << endl;
		
    // player->takeSkatCards(getCardIDISS2SkatTA(state.skat0->suit, state.skat0->rank),
    //                       getCardIDISS2SkatTA(state.skat1->suit, state.skat1->rank));
    // player->discardCards();

    // announce a normal game
    result = "D"; /*announceGame(false);*/
    
  } else if (state.phase == state.CARDPLAY) {
		
    cout << "SamplePlayer: CARDPLAY" << endl;
    cout << "state.view move->source state.toMove" << endl;
    cout << state.view << " " << state.toMove << endl;

    result = "H8"; /*calculateNextMove();*/

  } else {
		
    cout << "SamplePlayer: ERROR unhandled game phase..." << endl;
    result = "error-phase";
  }
	
  cout << "Decision: " << result << endl;
  return result;
}


#if 0

// SkatTA sample code 

/**
 * Sets the cards in the SkatTA player object
 */
void SkatTAPlayer::setCards(const Wrapper::SimpleState &state) {
	
  int playerID = state.view;
  int cards[32];

  // initialize card array
  for (int i = 0; i < 32; i++) {
    cards[i] = -1;
  }
	
  // set index of player cards

  int currCardIndex = playerID * 10;

  for (int suit = 0; suit < 4; suit++) {
    	
    switch(suit) {
    case 0:
      cout << "Diamonds: ";
      break;
    case 1:
      cout << "Hearts: ";
      break;
    case 2:
      cout << "Spades: ";
      break;
    case 3:
      cout << "Clubs: ";
      break;
    }
    	
    int currSuitCards = (state.pinfos[state.view].hand >> (8*suit)) & 0xff;
    	
    for (int i = 0; i < 8; i++) {
	    	
      if ((currSuitCards >> i) & 1 == 1) {
	    		
        cout << "(" << getCardIDISS2SkatTA(suit, i) << "->"
             << IOHelper::getCardString(getCardIDISS2SkatTA(suit, i)) << ")";
        
        cards[currCardIndex] = getCardIDISS2SkatTA(suit, i);
        currCardIndex++;
      }
      else {
	    		
        cout << "0";
      }
    }
    cout << endl;
  }
    
  player->takeCards(cards, 32);
  player->startBidding(playerID);
}

/**
 * Translates SkatTA card encoding into ISS encoding
 */
string SkatTAPlayer::getCardStringSkatTA2ISS(int skatTACardID) {
	
  string result = IOHelper::getColorString(skatTACardID / 8, 0);
  result.append(IOHelper::getValueString(skatTACardID % 8));
    
  return result;
}

/**
 * Calls announceGame() of a SkatTA player
 * and translates the SkatTA game type into ISS encoding
 */
string SkatTAPlayer::announceGame(bool handGame) {
	
  string result;

  player->announceGame();
  int gameType = player->getGameType();
	
  ostringstream s;

  // TODO announcing of ouvert
  // TODO announcing of schneider
  // TODO announcing of schwarz
	
  if (handGame) {
    s << getGameTypeStringSkatTA2ISS(gameType) << "H";

  } else {
    // normal game
    s << getGameTypeStringSkatTA2ISS(gameType) << "." << 
      getCardStringSkatTA2ISS(player->getDiscardedCard(0)) << "." << 
      getCardStringSkatTA2ISS(player->getDiscardedCard(1));
  }

  result = s.str();

  return result;
}

/**
 * Translates the SkatTA game type into ISS encoding
 */
string SkatTAPlayer::getGameTypeStringSkatTA2ISS(int skatTAGameType) {
	
  string result;
	
  switch(skatTAGameType) {
	
  case -1: // Null
    result = "N";
    break;
  case(0): // Diamonds
    result = "D";
    break;
  case(1): // Hearts
    result = "H";
    break;
  case(2): // Spades
    result = "S";
    break;
  case(3): // Clubs
    result = "C";
    break;
  case(4): // Grand
    result = "G";
    break;
  }
	
  return result;
}

/**
 * Translates the ISS game type into SkatTA encoding
 */
int SkatTAPlayer::getGameTypeIDISS2SkatTA(int issGameType) {

  int result = 0;
	
  switch(issGameType) {
		
  case 0: // Diamonds
  case 1: // Hearts
  case 2: // Spades
  case 3: // Clubs
  case 4: // Grand
    result = issGameType;
    break;
  case 5: // Null
    result = -1;
    break;
  }
	
  return result;
}

/**
 * Calls makeNextMove() of a SkatTA player
 * and translates the result into ISS encoding
 */
string SkatTAPlayer::calculateNextMove() {
	
  cout << "SkatTA: calculateNextMove()" << endl;
  
  string result;
  
  if (resignGame) {
	  
    // another player decides too resign the game
    result = "RE";

  } else {
	  
    // calculate next move
    result = getCardStringSkatTA2ISS(player->makeNextMove());
  }
  
  return result;
}

#endif
