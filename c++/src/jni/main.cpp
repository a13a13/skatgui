#include "Wrapper.h"

using namespace std;

int xmain()
{
  Wrapper::SimpleGame *sg = Wrapper::SimpleGame::deserialize(cin);
  return sg != 0;
}
