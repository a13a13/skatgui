#ifndef SAMPLEPLAYER_H_
#define SAMPLEPLAYER_H_

#include <string>

#include "../jni/Wrapper.h"
#include "../jni/Player.h"

class SamplePlayer : public Player
{
public:
  SamplePlayer(const std::string &playerType, const std::string &params);
  virtual ~SamplePlayer();

  void gameChange(const Wrapper::SimpleGame &sg, int move_index=-1);
  std::string computeMove(const Wrapper::SimpleGame &sg, double time);
  void reset();
  void interrupt();
};

#endif

