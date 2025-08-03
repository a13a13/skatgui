// (c) Michael Buro, licensed under GPLv3

package common;

import java.io.*;
import java.util.*;

public class ClientData
{
  static public int PERM_USER       = 0;
  static public int PERM_SUPER_USER = 1;
  static public int PERM_ROOT       = 2;

  // not serialized
  public int cloneNum; // how many instances are connected?
  
  public String id;      // player/group name
  public String password;
  public String email;
  public String regDate;     // registration date
  public String lastIPaddress;
  public String lastLoginTime;
  public int permission;     // see PERM_* above
  public int games3;         // #games played at 3-table
  public int games4;         // #games played at at 4-table (not counting sitouts)
  public int gamesDecl3;     // #games played as declarer at 3-table
  public int gamesDecl3Won;  // #games won as declarer at 3-table   
  public int gamesDef3;      // #games played as defender at 3-table
  public int gamesDef3Won;   // #games won as defender at 3-table   
  public int gamesDecl4;     // #games played as declarer at 4-table
  public int gamesDecl4Won;  // #games won as declarer at 4-table
  public int gamesDef4;      // #games played as defender at 4-table
  public int gamesDef4Won;   // #games won as defender at 4-table   
  public int gamesSitOut;    // #games dealt at 4-table
  public int gamesSitOutWon; // #games won as dealer at 4-table  
  public int decl3Pts;       // score total as declarer at 3-table (before tourn. adj.)
  public int decl4Pts;       // score total as declarer at 4-table (before tourn. adj.)
  public int penalty;        // #times player got a penalty
  public int timeout;        // #times player exceeded time
  public int disconnect;     // #times player disconnected while playing
  public double rating;
  public String languages = "E";  // default: english
  public int    group;      // != 0: multiple clients can connect simultaneously
  public String dummy3;
  public String dummy4;
  public String dummy5;
  public String dummy6;
  public String dummy7;
  public String dummy8;
  public String dummy9;    

  static public ClientData copy(ClientData x)
  {
    if (x == null)
      return null;

    ClientData cd = new ClientData();

    cd.id =             x.id;           
    cd.password =       x.password;     
    cd.email =          x.email;        
    cd.regDate =        x.regDate;       
    cd.lastIPaddress =  x.lastIPaddress;
    cd.lastLoginTime =  x.lastLoginTime;
    cd.permission =     x.permission;    
    cd.games3 =         x.games3;        
    cd.games4 =         x.games4;        
    cd.gamesDecl3 =     x.gamesDecl3;    
    cd.gamesDecl3Won =  x.gamesDecl3Won; 
    cd.gamesDef3 =      x.gamesDef3;     
    cd.gamesDef3Won =   x.gamesDef3Won;  
    cd.gamesDecl4 =     x.gamesDecl4;    
    cd.gamesDecl4Won =  x.gamesDecl4Won; 
    cd.gamesDef4 =      x.gamesDef4;     
    cd.gamesDef4Won =   x.gamesDef4Won;  
    cd.gamesSitOut =    x.gamesSitOut;   
    cd.gamesSitOutWon = x.gamesSitOutWon;
    cd.decl3Pts =       x.decl3Pts;      
    cd.decl4Pts =       x.decl4Pts;      
    cd.penalty =        x.penalty;       
    cd.timeout =        x.timeout;       
    cd.disconnect =     x.disconnect;    
    cd.rating =         x.rating;       
    cd.languages =      x.languages;       
    cd.group  =         x.group;
    cd.dummy3 =         x.dummy3;       
    cd.dummy4 =         x.dummy4;       
    cd.dummy5 =         x.dummy5;       
    cd.dummy6 =         x.dummy6;       
    cd.dummy7 =         x.dummy7;       
    cd.dummy8 =         x.dummy8;       
    cd.dummy9 =         x.dummy9;       

    return cd;
  }

  public int totalGames()
  {
    return games3 + games4 + gamesSitOut;
  }
  
  public double avgScore()
  {
    int games = totalGames();
    if (games == 0)
      return 0.0;
    
    return
      (0.0 + decl3Pts + decl4Pts +
       Table.DECL_WIN_PTS * (2*gamesDecl3Won - gamesDecl3) +
       Table.DEF3_WIN_PTS * gamesDef3Won +
       Table.DECL_WIN_PTS * (2*gamesDecl4Won - gamesDecl4) +
       Table.DEF4_WIN_PTS * (gamesDef4Won + gamesSitOutWon)
       ) / games;
  }

  // game counts sound?
  public String check()
  {
    if (games3 < 0) return "games3 < 0";
    if (games4 < 0) return "games3 < 0";  

    if (gamesDecl3 < 0)    return "gamesDecl3 < 0";
    if (gamesDecl3Won < 0) return "gamesDecl3Won < 0";
    if (gamesDef3 < 0)     return "gamesDef3 < 0";
    if (gamesDef3Won < 0)  return "gamesDef3Won < 0";
    if (gamesDecl4 < 0)    return "gamesDecl4 < 0";
    if (gamesDecl4Won < 0) return "gamesDecl4Won < 0";
    if (gamesDef4 < 0)     return "gamesDef4 < 0";
    if (gamesDef4Won < 0)  return "gamesDef4Won < 0";
    if (penalty < 0)       return "penalty < 0";
    if (timeout < 0)       return "timeout < 0";
    if (disconnect < 0)    return "disconnect < 0";

    if (gamesDecl3 < gamesDecl3Won) return "gamesDecl3 < gamesDecl3Won";
    if (gamesDef3  < gamesDef3Won)  return "gamesDef3 < gamesDef3Won";
    if (gamesDecl4 < gamesDecl4Won) return "gamesDecl4 < gamesDecl4Won";
    if (gamesDef4  < gamesDef4Won)  return "gamesDef4 < gamesDef4Won";    
    
    return null;
  }


  // for skatgui clientList
  public String toString()
  {
    String s = id + " " + languages;
    if (permission == PERM_SUPER_USER) return s + " S";
    if (permission == PERM_ROOT) return s + " R";    
    return s;
  }

  public void write(PrintStream out) throws IOException
  {
    out.println("id " + id);
    out.println("password " + password);
    out.println("email " + email);
    out.println("regDate " + regDate);
    out.println("lastIPaddress " + lastIPaddress);
    out.println("lastLoginTime " + lastLoginTime);    
    out.println("permission " + permission);
    out.println("games3 " + games3);
    out.println("games4 " + games4);
    out.println("gamesDecl3 " + gamesDecl3);
    out.println("gamesDecl3Won " + gamesDecl3Won);
    out.println("gamesDef3 " + gamesDef3);     
    out.println("gamesDef3Won " + gamesDef3Won);
    out.println("gamesDecl4 " + gamesDecl4);
    out.println("gamesDecl4Won " + gamesDecl4Won);
    out.println("gamesDef4 " + gamesDef4);     
    out.println("gamesDef4Won " + gamesDef4Won);
    out.println("gamesSitOut " + gamesSitOut);    
    out.println("gamesSitOutWon " + gamesSitOutWon);
    out.println("decl3Pts " + decl3Pts);
    out.println("decl4Pts " + decl4Pts);
    out.println("penalty " + penalty);
    out.println("timeout " + timeout);    
    out.println("disconnect " + disconnect);
    out.println("rating " + rating);
    out.println("languages " + languages);
    out.println("group " + group);
    out.println("dummy3 0");
    out.println("dummy4 0");
    out.println("dummy5 0");
    out.println("dummy6 0");
    out.println("dummy7 0");
    out.println("dummy8 0");
    out.println("dummy9 0");    
  }

  private int readValue(Scanner s, String var) throws Exception
  {
    expect(s, var);
    return Integer.parseInt(s.next());
  }
  
  public void read(Scanner s) throws Exception
  {
    expect(s, "id"); id = s.next();    
    expect(s, "password"); password = s.next();
    expect(s, "email"); email = s.next();
    expect(s, "regDate"); regDate = s.next();
    expect(s, "lastIPaddress"); lastIPaddress = s.next();
    expect(s, "lastLoginTime"); lastLoginTime = s.next();

    expect(s, "permission"); permission = Integer.parseInt(s.next());
    if (permission < PERM_USER || permission > PERM_ROOT) {
      throw new Exception(id + " permission out of range " + permission);
    }

    games3         = readValue(s, "games3");
    games4         = readValue(s, "games4");    
    gamesDecl3     = readValue(s, "gamesDecl3");
    gamesDecl3Won  = readValue(s, "gamesDecl3Won");
    gamesDef3      = readValue(s, "gamesDef3");
    gamesDef3Won   = readValue(s, "gamesDef3Won");
    gamesDecl4     = readValue(s, "gamesDecl4");
    gamesDecl4Won  = readValue(s, "gamesDecl4Won");
    gamesDef4      = readValue(s, "gamesDef4");
    gamesDef4Won   = readValue(s, "gamesDef4Won");
    gamesSitOut    = readValue(s, "gamesSitOut");
    gamesSitOutWon = readValue(s, "gamesSitOutWon");

    String r = check();
    if (r != null)
      throw new Exception(id + " game counts corrupt " + r);
    
    expect(s, "decl3Pts"); decl3Pts = Integer.parseInt(s.next());
    expect(s, "decl4Pts"); decl4Pts = Integer.parseInt(s.next());
    expect(s, "penalty");  penalty  = Integer.parseInt(s.next());
    expect(s, "timeout");  timeout  = Integer.parseInt(s.next());
    expect(s, "disconnect"); disconnect = Integer.parseInt(s.next());
    expect(s, "rating");  rating  = Double.parseDouble(s.next());

    expect(s, "languages"); languages = s.next();
    expect(s, "group");  group = Integer.parseInt(s.next());
    expect(s, "dummy3"); dummy3 = s.next();
    expect(s, "dummy4"); dummy4 = s.next();
    expect(s, "dummy5"); dummy5 = s.next();
    expect(s, "dummy6"); dummy6 = s.next();
    expect(s, "dummy7"); dummy7 = s.next();
    expect(s, "dummy8"); dummy8 = s.next();
    expect(s, "dummy9"); dummy9 = s.next();
  }

  static private void expect(Scanner s, String word) throws Exception
  {
    if (!s.hasNext())
      throw new Exception("text ended scanning for " + word);

    String t = s.next();
    if (!t.equals(word))
      throw new Exception("expected " + word + ", but read " + t);

    if (!s.hasNext())
      throw new Exception("text ended scanning for " + word + " parameter");
  }
  
}