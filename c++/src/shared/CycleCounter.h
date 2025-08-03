#ifndef CycleCounter_H
#define CycleCounter_H

// (c) Michael Buro, licensed under the GPL

// x86 cycle counter, useful for profiling
// SYSTEM DEPENDENT!

#include "Global.h"

struct CycleCounter
{
public:

  union {
    uint8 c8;
    struct { uint4 l, h; } c4;
  } count_;
  
  void stamp()
  {
#if defined(__i386__) || defined(__x86_64__) 
    __asm__ __volatile__ (".byte 0x0f,0x31" : "=a"(count_.c4.l),"=d"(count_.c4.h
                                                                     ));
#else
#warning cycle counter disabled    
    count_.c8 = 0;
#endif
  }

  uint8 count() const { return count_.c8; }

  CycleCounter() { stamp(); }
};

#endif
