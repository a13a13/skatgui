/** Skeleton Skat client

    (c) Michael Buro
    licensed under GPLv3
*/

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <fstream>
#include <iostream>
#include <string>
#include <vector>

#include "../jni/Wrapper.h"
#include "SamplePlayer.h"

using namespace std;

void testISSSimpleState(const string& filename)
{
  // read in serialized simplegame and compute move
  ifstream infile(filename.c_str());

  if (!infile) {
		   
    cerr << "Could not open file " << filename << endl;

  } else {

    string gameString;
    string line;
	
    while (getline(infile, line)) {
      gameString += " " + line;
    }

    infile.close();

    istringstream stringStream(gameString);
    Wrapper::SimpleGame *sg = Wrapper::SimpleGame::deserialize(stringStream);

    SamplePlayer player("", "");
    string result;
    
    if (sg) {
		    
      // replay game
      
      for (size_t i=0; i < sg->moves.size(); i++) {
        player.gameChange(*sg, i);
      }
      
      result = player.computeMove(*sg, 0.0);

    } else {
			  
      cout << "Invalid simple game file!" << endl;
    }

    cout << "move = " << result << endl;
  }
}

int main(int argc, char * argv[])
{
  cout << endl << "Sample Skat Player" << endl << endl;

  // initialize random generator
  srand(time(NULL));

  if (argc <= 1) {
    cout << "usage: sampleTest serialized-game-file" << endl;
    cout << "asks SamplePlayer for move at the end of the move sequence encoded by state sequence" << endl;
    cout << "see UNCOMMENT line in src/jni/interface.cpp" << endl;
    
    exit(-10);
  }
  
  // ISS game file was given
  
  testISSSimpleState(string(argv[1]));
  
  return 0;
}
