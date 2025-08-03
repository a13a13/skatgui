#ifndef RNG_H
#define RNG_H

// $Id: RNG.h 7546 2008-10-30 19:16:25Z mburo $
// George Marsaglia's Carry RNG

#include "Global.h"

class RNG
{
private:

  static const int MULTIPLIER = 2083801278; // don't change
  
public:
  
  RNG(uint4 x_=362436069, uint4 carry_= 1234567)
    : x(x_), carry(carry_)
  {
    set_seed(x_, carry_);
  }

  void set_seed(uint4 x_, uint4 carry_)
  {
    x = x_;
    carry = carry_;

    // avoid repetition seeds
    if ((x == 0 && carry == 0) || (x == 0xffffffff && carry == MULTIPLIER-1))
      carry++;
  }

  void get_seed(uint4 &x_, uint4 &carry_) const
  {
    x_ = x;
    carry_ = carry;
  }

  uint4 operator()(uint4 r)
  {
    assert(r > 0);
    return (*this)() % r;
  }
  
  uint4 operator()()
  {
    uint8 axc = (uint8)x * MULTIPLIER + carry; 
    carry = axc >> 32;
    x = (uint4) axc;
    return x;
  }

private:
  
  uint4 x, carry;
};

#endif
