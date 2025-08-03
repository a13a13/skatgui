// $Id: RNG.H 5769 2007-09-01 04:10:26Z mburo $
// using better rng from java library

package common;

import java.util.*;
import java.security.SecureRandom;

public class BetterRNG extends Random
{
  // to make next accessible
  class Wrap extends SecureRandom
  {
    public Wrap() {
      Misc.msg("created");
    }
    
    int myNext(int bits) {
      return next(bits);
    }
  }

  private Wrap srng = new Wrap();

  public BetterRNG()
  {
    setSeed(362436069, 1234567);
  }
  
  public BetterRNG(long x_, long carry_)
  {
    setSeed(x_, carry_);
  }

  public void setOverlay(long overlay)
  {
    this.overlay = overlay ^ (overlay >> 32); // only <= 32 bits used in next()
  }

  synchronized public void setSeed(long x_, long y_)
  {
    byte[] bytes = new byte[] {
      (byte) (x_ >>> 56),
      (byte) (x_ >>> 48),
      (byte) (x_ >>> 40),
      (byte) (x_ >>> 32),
      (byte) (x_ >>> 24), 
      (byte) (x_ >>> 16), 
      (byte) (x_ >>> 8),     
      (byte) (x_),
      (byte) (y_ >>> 56),
      (byte) (y_ >>> 48),
      (byte) (y_ >>> 40),
      (byte) (y_ >>> 32),
      (byte) (y_ >>> 24), 
      (byte) (y_ >>> 16), 
      (byte) (y_ >>> 8),     
      (byte) (y_),
    };

    srng.setSeed(bytes);
  }

  synchronized protected int next(int bits)
  {
    // Misc.msg("called next with " + overlay);
    assert(bits <= 32);
    
    return (int) ((srng.myNext(bits) ^ overlay) & ((1L << bits)-1));
  }

  private long overlay; // external entropy source (xor'ed)
};

