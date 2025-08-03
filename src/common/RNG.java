// $Id: RNG.H 5769 2007-09-01 04:10:26Z mburo $
// George Marsaglia's Carry RNG

package common;

import java.util.*;

public class RNG extends Random
{
  static final long MULTIPLIER = 2083801278; // don't change
  static final long MASK32 = 0xffffffffL;
  
  public class Seed
  {
    long x;
    long carry;
    
    Seed(long x_, long carry_) {
      x = x_;
      carry = carry_;
    }
  };
  
  public RNG()
  {
    setSeed(362436069, 1234567);
  }
  
  public RNG(long x_, long carry_)
  {
    setSeed(x_, carry_);
  }

  public void setOverlay(long overlay)
  {
    this.overlay = overlay ^ (overlay >> 32); // only <= 32 bits used in next()
  }

  synchronized public void setSeed(long x_)
  {
    setSeed(x_, x_);
  }
  
  synchronized public void setSeed(long x_, long carry_)
  {
    x = x_ & MASK32;
    carry = carry_ & MASK32;

    // avoid repetition seeds
    if ((x == 0 && carry == 0) || (x == MASK32 && carry == MULTIPLIER-1))
      carry++;
  }

  synchronized public Seed getSeed()
  {
    return new Seed(x, carry);
  }

  synchronized public int next(int bits)
  {
    assert bits <= 32;
    long axc = x * MULTIPLIER + carry; 
    carry = (axc >> 32) & MASK32;
    x = axc & MASK32;
    return (int) ((x ^ carry ^ overlay) & ((1L << bits)-1));
  }

  private long x, carry;
  private long overlay; // external entropy source (xor'ed with x)
};

