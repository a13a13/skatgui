// Automatically Detecting Thread Deadlocks
//  based on code by Dr. Heinz M. Kabutz

package common;

import java.lang.management.*;
import java.util.*;

abstract public class DeadlockDetection {
  private final Timer threadCheck = new Timer("Thread Monitor", true);
  private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();

  /**
   * The number of milliseconds between checking for deadlocks.
   * It may be expensive to check for deadlocks, and it is not
   * critical to know so quickly.
   */
  private static final int DEADLOCK_CHECK_PERIOD = 2000;
  private static final int MAX_STACK_DEPTH = 50;
  private Set<Long> deadlockedThreads = new HashSet<Long>();

  /**
   * Monitor only deadlocks.
   */
  public DeadlockDetection() {

    threadCheck.schedule(new TimerTask() {
      public void run() {
        long[] ids = mbean.findMonitorDeadlockedThreads();

        if (ids == null || ids.length == 0) return;

        StringBuffer sb = new StringBuffer();

        sb.append("DEADLOCK DETECTED!");
        
        for (Long l : ids) {

          ThreadInfo inf = mbean.getThreadInfo(l, MAX_STACK_DEPTH);

          sb.append("Deadlocked Thread\n:");
          sb.append("------------------\n");
          sb.append(inf.toString() + "\n");
          for (StackTraceElement ste : inf.getStackTrace()) {
            sb.append("\t" + ste + "\n");
          }
        }

        detected(sb.toString());
        Misc.err("APPLICATION STOPS DUE TO A DEADLOCK.");
      }
    }, 10, DEADLOCK_CHECK_PERIOD);
  }

  abstract public void detected(String errmsg);
}
