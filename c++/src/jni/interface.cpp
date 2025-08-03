/** JNI interface to C++ Skat player

    (c) Michael Buro
    licensed under GPLv3
*/

#include <iostream>
#include <sstream>
#include <string>

//#include "PlayerDispatcher.h"
#include "Player.h"
#include "interface.h"
#include "Wrapper.h"

using namespace std;

/*
 * Class:     client_JNIPlayer
 * Method:    deletePlayer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_client_JNIPlayer_deletePlayer
(JNIEnv *, jclass, jlong obj)
{
  delete reinterpret_cast<Player*>(obj);
}

/*
 * Class:     client_JNIPlayer
 * Method:    computeMove
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;D)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_client_JNIPlayer_computeMove
(JNIEnv * env, jclass,
 jlong obj,
 jstring game, jdouble remainingTime)
{
  const char *game_s = env->GetStringUTFChars(game, 0);

  istringstream is(string(game_s), istringstream::in);

  // UNCOMMENT THIS LINE FOR DEBUGGING. Save the output to a file and
  // start your client with this file (see sampleTest for an example)
  
  //cout << "compute move game=\n" << cppSimpleGameString << endl; // !!!

  Wrapper::SimpleGame *sg = Wrapper::SimpleGame::deserialize(is);

  string result = reinterpret_cast<Player*>(obj)->computeMove(*sg, double(remainingTime));
  delete sg;

  env->ReleaseStringUTFChars(game, game_s);

  return env->NewStringUTF(result.c_str());
}
 
/*
 * Class:     client_JNIPlayer
 * Method:    interruptMoveComputation
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_client_JNIPlayer_interruptMoveComputation
(JNIEnv *, jclass, jlong obj)
{
  reinterpret_cast<Player*>(obj)->interruptMoveComputation();
}

/*
 * Class:     client_JNIPlayer
 * Method:    reset
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_client_JNIPlayer_reset
(JNIEnv *, jclass, jlong obj)
{
  reinterpret_cast<Player*>(obj)->reset();
}

/*
 * Class:     client_JNIPlayer
 * Method:    gameChange
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT void JNICALL Java_client_JNIPlayer_gameChange
(JNIEnv * env, jclass, jlong obj, jstring game)
{
  const char *game_s = env->GetStringUTFChars(game, 0);

  istringstream is(string(game_s), istringstream::in);

  // UNCOMMENT THIS LINE FOR DEBUGGING. Save the output to a file and
  // start your client with this file (see sampleTest for an example)
  
  //cout << "compute move game=\n" << cppSimpleGameString << endl; // !!!

  Wrapper::SimpleGame *sg = Wrapper::SimpleGame::deserialize(is);

  reinterpret_cast<Player*>(obj)->gameChange(*sg);
	
  delete sg;

  env->ReleaseStringUTFChars(game, game_s);
}

/*
 * Class:     client_JNIPlayer
 * Method:    DDS
 * Signature: (Ljava/lang/String;IIIIIIIIIIIIII)I
 */

/*
  public static native int DDS(int fh, int mh, int rh,            // remaining cards (bit sets)
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
                               );
*/

JNIEXPORT jint JNICALL Java_client_JNIPlayer_DDS
(JNIEnv *, jclass,
 jlong obj,
 jint fh, jint mh, jint rh,
 jint toMove,
 jint declarer,
 jint gameType,
 jint trickCardNum,
 jint trickCard1, jint trickCard2,
 jint ptsDeclarer, jint ptsDefenders,
 jint alpha, jint beta,
 jint clearHash,
 jint paranoid)
{
  return reinterpret_cast<Player*>(obj)->DDS(fh, mh, rh,
                                             toMove,
                                             declarer,
                                             gameType,
                                             trickCardNum,
                                             trickCard1, trickCard2,
                                             ptsDeclarer, ptsDefenders,
                                             alpha, beta,
                                             clearHash,
                                             paranoid);
}


/** Paranoid C++ interface */

// initialization

/*
 * Class:     client_JNIPlayer
 * Method:    paranoid_init
 * Signature: (JLjava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_client_JNIPlayer_paranoid_1init
  (JNIEnv *env, jclass,
   jlong obj,        // address of underlying c++ player object
   jstring filename, // table file name
   jint pos          // declarer position
   )
{
  const char *filename_s = env->GetStringUTFChars(filename, 0);

  reinterpret_cast<Player*>(obj)->paranoid_init(filename_s, pos);

  env->ReleaseStringUTFChars(filename, filename_s);
}

/*    
    return paranoid value:
    -1: loss
     0: win
     1: schneider
     2: schwarz
*/


/*
 * Class:     client_JNIPlayer
 * Method:    paranoid
 * Signature: (JIIII)I
 */
JNIEXPORT jint JNICALL Java_client_JNIPlayer_paranoid
(JNIEnv *, jclass,
 jlong obj,    // address of underlying c++ player object 
 jint hand,    // hand bitset
 jint skat,    // skat bitset
 jint gameType // 0,1,2,3 (diamonds..clubs), 4 (grand)
 )
{
  return reinterpret_cast<Player*>(obj)->paranoid(hand, skat, gameType);
}

/*
 * Class:     client_JNIPlayer
 * Method:    w_paranoid
 * Signature: (JIIIIIIIIIIII[I[I[I)I
 */
JNIEXPORT jint JNICALL Java_client_JNIPlayer_w_1paranoid
(JNIEnv *env, jclass,
 jlong obj,
 jint declHand,                       // declarer hand
 jint played,                         // all played cards
 jint toMove,                         // 0,1,2 
 int declarer,                        // 0,1,2 
 jint gameType,                       // 0,1,2,3 (diamonds..clubs), 4 (grand), 5 (null - not implemented) 
 jint trickCardNum,                   // number of cards in trick (0,1,2)
 jint trickCard1, jint trickCard2,    // trick card indexes
 jint ptsDeclarer, jint ptsDefenders, // card points thus far - including skat (0..120) 
 jint alpha, jint beta,               // point *differential* bounds for ptm, ignored in null             
 jint clearHash,                      // !=0: clear hash table before searching 
 jintArray hands1,
 jintArray hands2,
 jintArray skats
 )
{
  jsize n = env->GetArrayLength(hands1);
  if (n != env->GetArrayLength(hands2) ||
      n != env->GetArrayLength(skats)) {
    cerr << "w_paranoid size mismatch" << endl; exit(-1);
  }
      
  jint *c_hands1 = env->GetIntArrayElements(hands1, 0);
  jint *c_hands2 = env->GetIntArrayElements(hands2, 0);
  jint *c_skats  = env->GetIntArrayElements(skats,  0);
 
  int v = reinterpret_cast<Player*>(obj)->w_paranoid(declHand, played, toMove, declarer, gameType,
                                                     trickCardNum, trickCard1, trickCard2,
                                                     ptsDeclarer, ptsDefenders,
                                                     alpha, beta, clearHash,
                                                     n, c_hands1, c_hands2, c_skats);
  env->ReleaseIntArrayElements(skats,  c_skats,  0);
  env->ReleaseIntArrayElements(hands2, c_hands2, 0);
  env->ReleaseIntArrayElements(hands1, c_hands1, 0);  

  return v;
}
