/*
 * Main.java
 *
 * (c) Michael Buro, licensed under GPLv3
 *
 * Added new command-line option, "-m t", so that the GUI can be used in live
 * tournament mode. - Ryan Lagerquist
 */

package gui;

import java.io.*;
import common.*;

public class Main
{
  /** Creates a new instance of Main */
  public Main() {
  }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {

    Options opt = new Options();
    
    opt.put("-m",  "c", "Mode: c(lient) o(utput) e(dit) t(ournament)");
    opt.put("-h",  "skat.dnsalias.net", "client mode: server host");
    opt.put("-p",  new Integer(7000), "client mode: server port");
    opt.put("-id", "", "client mode: id");
    opt.put("-pw", "", "client mode: password");
    opt.put("-cmds", "", "client mode: initial commands (|=sep _=space)");
    opt.put("-s", "skatgui-settings.txt", "persistent settings");
    opt.put("-lang", "", "locale language");
    opt.put("-l", new String("skatgui"), "log messages to this file-log.txt (.=none), also create bug report");
    opt.put("-a", "additional data"); 
    // opt.put("-x", "expert mode"); 
    opt.put("-v", "print version");
    
    if (!opt.parse(args)) {
      Misc.err("option error (skatgui version " + Misc.version() + ")");
    }

    if (opt.getSwitchOn("-v")) {
      System.out.println(ClientWindow.version());
      System.exit(0);
    }
    
    String logFile = opt.getString("-l");
    if (!logFile.equals(".")) {
      Misc.setLogFile(logFile);
    }

    String mode = opt.getString("-m");

    if (mode.equals("c") || mode.equals("t")) {
      final ClientWindow cw;

      if(mode.equals("c")) {

        cw = new ClientWindow(opt.getString("-h"),
                              opt.getInteger("-p").intValue(),
                              opt.getString("-id"),
                              opt.getString("-pw"),
                              opt.getString("-cmds"),
                              opt.getString("-s"),
                              opt.getString("-lang"),
                              opt.getSwitchOn("-a"),
                              false,
                              850, 700);
      } else {

        // Starts SkatGUI in tournament mode:

        cw = new ClientWindow(opt.getString("-h"),
                              opt.getInteger("-p").intValue(),
                              opt.getString("-id"),
                              opt.getString("-pw"),
                              opt.getString("-cmds"),
                              opt.getString("-s"),
                              opt.getString("-lang"),
                              opt.getSwitchOn("-a"),
                              true,
                              950, 570
                              );
      }
      
      Misc.msg("start deadlock detection");

      DeadlockDetection dt = new DeadlockDetection() {
          
          public void detected(String errmsg) {
            Misc.msg("DEADLOCK!\n " + errmsg);
            cw.serious(new Exception("DEADLOCK DETECTED"));
          }
        };

      cw.run();
      System.exit(0);
    }

    if (mode.equals("o")) {
      
      System.out.println("output mode");
      
      MainWindow w = new MainWindow();
      w.run();
      System.exit(0);
    }

    Misc.msg("start deadlock detection");
    
    DeadlockDetection dt = new DeadlockDetection() {
        public void detected(String errmsg) {
          Misc.exception(new Exception("DEADLOCK DETECTED\n" + errmsg));
        }
      };
    
    SimpleGame sg = new SimpleGame(SimpleState.WORLD_VIEW);

    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String r = sg.fromSgf(in);
    if (r == null || !r.equals("")) {
      Misc.err("error reading game from stdin " + ((r != null) ? r : "EOF"));
    }
    
    EditWindow w = new EditWindow(sg, true, 990, 760);
    w.setVisible(true);
    w.run();
  }
}
