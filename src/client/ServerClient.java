// Card server client, adds network connection to ServiceClient

// (c) Michael Buro, licensed under GPLv3

package client;

import java.util.*;
import java.io.*;
import java.net.*;
import common.*;

public class ServerClient
{
  static final boolean DBG = true;
  
  private ServiceClient.ServiceMsgHandler handler;
  private String place;
  private String host;
  private int    port;
  private String clientId;
  private Socket socket;
  private BufferedWriter output;
  private BufferedReader input;
  private ReaderThread rt;
  private WriterThread wt;
  
  ServiceClient client;

  public final static String ERR_CONNECTION             = "_connection_error";
  public final static String ERR_LOGIN_EXPECTED_PW      = "_login_error_expected_pw";
  public final static String ERR_LOGIN_EXPECTED_WELCOME = "_login_error_expected_welcome";  
  public final static String ERR_VERSION                = "_version_mismatch_error";
  
  public ServiceClient getServiceClient() { return client; }

  public String getClientId() { return clientId; }
  
  /** @return null if OK, error message otherwise */
  public String run(ServiceClient.ServiceMsgHandler handler_, String place_,
                    String host_, int port_, String clientId_, String passwd_,
                    String cmds)
  {
    handler = handler_;
    place = place_;
    host = host_;
    port = port_;
    clientId = clientId_;

    try {
      socket = new Socket(host, port);
      input = new BufferedReader(new InputStreamReader(new BufferedInputStream(socket.getInputStream()), "UTF8"));
      output = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(socket.getOutputStream()), "UTF8"));
    }
    catch (Throwable e) {
      return ERR_CONNECTION;
    }

    try {
    
      output.write(clientId+"\n"); output.flush();
      Misc.msg("SENT: " + clientId);
      String l = input.readLine();
      Misc.msg("RCVD: " + l);
      if (!l.equals("password:")) {
        return ERR_LOGIN_EXPECTED_PW;
      }

      output.write(passwd_+"\n"); output.flush();      
      Misc.msg("SENT: " + passwd_);
      
      l = input.readLine();
      Misc.msg("RCVD: " + l);

      if (!l.startsWith("Welcome")) {
        return ERR_LOGIN_EXPECTED_WELCOME + " : " + l;
      }

      String[] words = l.split(" ");

      for (int i=0; i < words.length; i++) {
        if (words[i].equals("version")) {
          if (i+1 < words.length) {
            if (!Misc.versionMatch(words[i+1])) {
              return ERR_VERSION;
            }
          }
        }
      }

      clientId = words[1]; // changed if using group login

      client = new ServiceClient(place, clientId);
      
      if (cmds != null && !cmds.equals("")) {

	// send commands
	
	String[] parts = cmds.split("\\|");

	for (String c : parts) {

	  if (c.equals("@")) {
	    Misc.sleep(500);
	  } else {
	    String d = c.replace('_', ' ');
	    output.write(d + '\n'); output.flush();
	    Misc.msg("SENT: " + d);
	  }
	}
      }
    }
    catch (Throwable e) {
      return "communication problem";
    }

    // connected, start read/write threads

    rt = new ReaderThread();
    rt.start();

    wt = new WriterThread();
    wt.start();

    // ensure writer thread is waiting on queue so that first message
    // is sent right away
    while (!wt.waiting) {
      Misc.sleep(1);
    }

    return null; // OK
  }

  public void terminate()
  {
    rt.dead = true;
    wt.dead = true;
    synchronized (wt.queue) {
      wt.queue.notifyAll(); // wake up writer thread
    }

    try {
      socket.close();
    }
    catch (Throwable e) {
      // Misc.msg(e.toString());
    }

    rt.interrupt();
    wt.interrupt();
    
    handler.handleServiceMsg(client.newDisconnectMsg(), "");
  }

  synchronized public void send(String msg)
  {
    if (wt == null) return;
    wt.write(msg);
  }
  
  // read loop when client is connected

  class ReaderThread extends Thread
  {
    public boolean dead = false; // true terminates loop

    public ReaderThread()
    {
    }

    public void run()
    {
      if (DBG)
        Misc.msg("ReaderThread created");
    
      try {

        while (!dead) {
        
          String line = input.readLine();

          if (dead) break;
	
          if (line == null) throw new IOException("empty line");

          int l = line.length();
          if (l < 1000) {
            Misc.msg("RCVD: '" + line + "'");
          } else {
            Misc.msg("RCVD: '" + line.substring(0, 1000) + "' ...");
          }
          
	  ServiceClient.ServiceMsg sm;

          try {

            synchronized (getThis()) { // if not synchronized, current trick gfx gets confused!
              sm = client.received(line);
            }

            if (sm != null) {
              handler.handleServiceMsg(sm, line);
            } else {
              Misc.msg("message " + line + " not handled!");
            }
          }
          catch (Throwable e) {
            Misc.exception(e);
          }
        }
      } 

      catch (IOException e) {
        Misc.msg(Misc.stack2string(e));
      }
    
      catch (Throwable e) {
        Misc.exception(e);
      }
    
      finally {
        Misc.msg("Terminating communication dead=" + dead);
        terminate();
      }

      if (DBG)
        Misc.msg("ReaderThread terminating");
    }
  }


  // write loop

  class WriterThread extends Thread
  {
    public boolean dead = false; // true terminates loop  
    public LinkedList<String> queue = new LinkedList<String>();
    public boolean waiting = false;
  
    public WriterThread()
    {
    }

    public void write(String msg)
    {
      synchronized (queue) {
        queue.add(msg);
        queue.notifyAll();
      }
    }
  
    public void run()
    {
      if (DBG)
        Misc.msg("WriterThread created");    

      try {

        while (!dead) {

          // wait for message in queue

          synchronized (queue) {
            waiting = true;
            queue.wait();
          }

          if (dead) break;
	
          boolean done;
        
          do { 
          
            String msg = null;

            synchronized (queue) {
              done = queue.isEmpty();
              if (!done) 
                msg = queue.remove(0);
            }

            if (!done) {
              output.write(msg + '\n');
              output.flush();

	      int l = msg.length();
	      if (l < 100) {
		Misc.msg("SENT: '" + msg + "'");
	      } else {
		Misc.msg("SENT: '" + msg.substring(0, 100) + "' ...");
	      }
            }

          } while (!done);
        }
      }
      catch (Throwable e) {
        Misc.msg(e.toString());
      }
      finally {
        // I/O problems -> kill connection
        terminate();
      }

      if (DBG)
        Misc.msg("WriterThread terminating");    
    }
  }

  private ServerClient getThis() { return this; }
}
