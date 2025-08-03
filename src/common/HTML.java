/**
   HTML helpers

   (c) Michael Buro, licensed under GPL3
   Code that deals with table archive files, as opposed to individual game files,
   (c) Ryan Lagerquist
*/

package common;

import java.util.*;
import java.util.regex.*;
import java.text.*;
import java.io.*;

public class HTML
{
  private final static int MAX_PLAYER_NAME_LENGTH = 8;
  static final String[] handNames = new String[] { "FH", "MH", "RH", "Skat" };

    static final String rbs(String s, String lang) {
	if (lang.equals("fr")) {
	    if (s.equals("Results of List for Table "))
		return "R&eacute;sultats de Liste &agrave; Table ";
	    else if (s.equals("Date: "))
		return "Date: ";
	    else if (s.equals("Round"))
		return "Partie";
	    else if (s.equals("Table"))
		return "Table";
	    else if (s.equals("Modifiers"))
		return "Modificateurs";
	    else if (s.equals("Score"))
		return "Points";
	    else if (s.equals("Seat"))
		return "Place";
	    else if (s.equals("Totals"))
		return "Totals";
	    else if (s.equals("(Won - Lost) * 50"))
		return "(Gagn&eacute; - Perdu) * 50";
	    else if (s.equals("Opponent Losses * "))
		return "D&eacute;faites d'Adversaire * ";
	    else if (s.equals("Timeouts * -100"))
		return "D&eacute;passements de Temps * -100";
	    else if (s.equals("End Result"))
		return "R&eacute;sultat Final et D&eacute;faites";
	    else if (s.equals("no deal"))
		return "aucunes cartes donn&eacute;es";
	    else if (s.equals("FH"))
		return "PM";
	    else if (s.equals("MH"))
		return "MM";
	    else if (s.equals("RH"))
		return "DM";
	    else if (s.equals("Bid: "))
		return "Annonce: ";
	    else if (s.equals("pass"))
		return "rien";
	    else if (s.equals("Soloist: "))
		return "D&eacute;clarateur: ";
	    else if (s.equals("No soloist"))
		return "Pas de d&eacute;clarateur";
	    else if (s.equals(", no game type declared <br />"))
		return ", pas de match d&eacute;clar&eacute;. <br />";
	    else if (s.equals(", Game: "))
		return ", Type de Match: ";
	    else if (s.equals("passed"))
		return "Tous ont pass&eacute;s.";
	    else if (s.equals("Score: "))
		return "Points: ";
	    else if (s.equals(" card points,"))
		return " points de cartes,";
	    else if (s.equals(" with "))
		return " avec ";
	    else if (s.equals(" without "))
		return " sans ";
	    else if (s.equals(" schwarz"))
		return " schwarz";
	    else if (s.equals(" schneider"))
		return " schneider";
	    else if (s.equals(" overbid"))
		return " trop annonc&eacute;";
	    else if (s.equals(" timeout"))
		return " temps d&eacute;pass&eacute;";
	    else if (s.equals(" resigned"))
		return " retir&eacute;";
	    else if (s.equals("game not finished"))
		return "match n'&eacute;tait pas fini";
	    else if (s.equals("Grand"))
		return "Grand";
	    else if (s.equals("Null"))
		return "Nul";
	    else if (s.equals("Diamonds"))
		return "Carreaux";
	    else if (s.equals("Hearts"))
		return "C&oelig;urs";
	    else if (s.equals("Spades"))
		return "Piques";
	    else if (s.equals("Clubs"))
		return "Tr&egrave;fles";
	    else if (s.equals(" Ouvert"))
		return " Ouvert";
	    else if (s.equals(" Hand"))
		return " Main";
	    else if (s.equals(" Ouvert Hand"))
		return " Ouvert Main";
	    else if (s.equals(" Schneider"))
		return " Schneider";
	    else if (s.equals(" Schwarz"))
		return " Schwarz";
	    else if (s.equals(" Hand Schneider"))
		return " Main Schneider";
	    else if (s.equals(" Hand Schwarz"))
		return " Main Schwarz";
            else if (s.equals("Unknown"))
              return "Inconnu";
	    else
		return s;
	} else {
	    /* For English we can always just return s, since the first parameter passed to rbs
	       is the exact string that should be returned in English.  Translations for other
	       languages will be added here. - Ryan*/
	    return s;
	}
    }

  static final String cardFile(Card c)
  {
    if (c == null)
      return "/cards/back.gif";
    
    return "/cards/" + c.toString() + ".gif";
  }

  static final String localCardFile(Card c, boolean fromSkatTool)
  {
    if (c == null)
      return "/src/data/cards/.gif";

    if (!fromSkatTool)
      return "src/data/cards/G4/" + (c.getSuit()+1)%4 + "_" + (c.getRank()+6)%13 + ".gif";

    return "../src/data/cards/G4/" + (c.getSuit()+1)%4 + "_" + (c.getRank()+6)%13 + ".gif";
  }

  static final String localTrickCardFile(ArrayList<Card> trick, int i, boolean fromSkatTool)
  {
    i %= 3;
    return localCardFile(i < trick.size() ? trick.get(i) : null, fromSkatTool);
  }

  static final String trickCardFile(ArrayList<Card> trick, int i)
  {
    i %= 3;
    return cardFile(i < trick.size() ? trick.get(i) : null);
  }

  static public String createPage(String body)
  {
    // with this image positioning doesn't work?!
      /*
	return
	    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
	    "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n"
      */

      return
	  "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n" +
	  "<head>\n" +
	  "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n" +	 
	  "<title></title>\n" +
	  "</head>\n" +
	  "<body>\n" +
	  body +
	  "\n</body></html>\n";
  }

    /* To be used for all HTML pages other than game histories, in which image positioning
       is screwed up by the DOCTYPE declaration. */
    static public String createPageWithDoctype(String body)
    {
	return
	    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
	    "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n" +
	    "<head>\n" +
	    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n" +
	    "<title></title>\n" +
	    "</head>\n" +
	    "<body>\n" +
	    body +
	    "\n</body></html>\n";
    }

    /* For archive pages, which require a little bit of CSS. - Ryan */
    static public String createPageWithStyle(String body)
    {
	return
	    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
	    "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n" +
	    "<head>\n" +
	    "<style type=\"text/css\">\n" + 
	    "img {\n" +
	    "  margin-left:5px;\n" +
	    "  margin-right:5px;\n" +
	    "}\n" +
	    "</style>\n" +
	    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n" +
	    "<title></title>\n" +
	    "</head>\n" +
	    "<body>\n" +
	    body +
	    "\n</body></html>\n";
    }
  
    static public String game2HTML(SimpleGame g, String language)
  {
    StringBuffer sb = new StringBuffer();

    if (g.getNumMoves() < 1)
	return rbs("no deal", language);
    
    SimpleState st = g.getState(1); // after dealing
    SimpleState cst = g.getCurrentState(); // game end
    Card[] cards = new Card[10];

    int x = 0;
    int y = 0;
    int w = 70;

    // hands + max bid
    for (int p=0; p < 3; p++) {
      int cn = Hand.toCardArray(st.getHand(p), cards);
      Arrays.sort(cards, new CardComparer(GameDeclaration.GRAND_GAME, false));
      int z = 0;
      int l = 0;
      sb.append("<span style=\"vertical-align:200%; width:40; float:left\">" + rbs(handNames[p], language) + "</span>");
      sb.append("<span>\n");
      for (int i=0; i < cn; i++) {
        if (i > 0) l = -40;
        sb.append(String.format(Misc.locEn,
                                "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"margin-left:%d; z-index:%d;\" />",
                                cardFile(cards[i]), w, cards[i].toString(), l, z));
        l -= 30;
        z++;
      }
      sb.append("</span>\n");
      
      sb.append("<span style=\"vertical-align:top\">[" + g.getPlayerName(p)+"] " + rbs("Bid: ", language) +
                (cst.getMaxBid(p) < 18 ? rbs("pass", language) : cst.getMaxBid(p)) + "</span>\n");
      sb.append("<br />\n");
    }

    // skat
    sb.append(handNames[3] + "<br />");
    sb.append(String.format(Misc.locEn,
                            "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"{position: relative; left: %d; z-index:%d;}\" />",
                            cardFile(st.getSkat0()), w, st.getSkat0().toString(), 0, 0));
    sb.append(String.format(Misc.locEn,
                            "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"{position: relative; left: %d; z-index:%d;}\" />",
                            cardFile(st.getSkat1()), w, st.getSkat1().toString(), 0, 0));
    sb.append("<br />\n");
    
    GameDeclaration gd = g.getGameDeclaration();

    int decl = cst.getDeclarer();
    
    // soloist and game declaration
    if (decl >= 0) {
	sb.append(rbs("Soloist: ", language) + rbs(handNames[decl], language)); 
    } else {
	sb.append(rbs("No soloist", language));
    }
    
    if (gd.type == GameDeclaration.NO_GAME) {
	sb.append(rbs(", no game type declared <br />", language));
    } else {
	sb.append(rbs(", Game: ", language) + rbs(gd.typeToVerboseString(), language) + " "
		  + rbs(gd.modifiersToVerboseString(), language));
      sb.append("<br />\n");

      if (!gd.hand) {
        
        // show 12 card hand and indicate discard

        Card[] c12 = new Card[12];
        int cn = Hand.toCardArray(st.getHand(decl) | st.getSkat(), c12);
        Arrays.sort(c12, new CardComparer(gd.type, false));
        
        int z = 0;
        int l = 0;
        for (int i=0; i < cn; i++) {

          // lower discarded cards
          int lift = 0;
          if (cst.getSkat0().equals(c12[i]) || cst.getSkat1().equals(c12[i])) {
            lift = 10;
          }
          
          sb.append(String.format(Misc.locEn,
                                  "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"{position: relative; left: %d; z-index:%d; top: %d}\" />",
                                  cardFile(c12[i]), w, c12[i].toString(), l, z, lift));
          l -= 30;
          z++;
        }

        sb.append("<br /><br />\n");
      }
    }
    
    // result

    if (g.isFinished()) {

      GameResult gr = new GameResult();

      cst.gameResult(gr);

      if (gr.passed)
	  sb.append(rbs("passed", language));
      else {
	  sb.append(rbs("Score: ", language) + gr.declValue);
	  if (gd.type != GameDeclaration.NULL_GAME) {
	      sb.append(" [ " + gr.declCardPoints + rbs(" card points,", language));
          if (gr.matadors > 0)
	      sb.append(rbs(" with ", language) + gr.matadors);
          else
	      sb.append(rbs(" without ", language) + (-gr.matadors));
          if (gr.schwarz)
	      sb.append(rbs(" schwarz", language));
          else if (gr.schneider)
	      sb.append(rbs(" schneider", language));
          if (gr.overbid)
	      sb.append(rbs(" overbid", language));
          if (gr.timeout >= 0)
	      sb.append(rbs(" timeout", language));
          if (gr.resigned)
	      sb.append(rbs(" resigned", language));
          sb.append(" ]");
        }
      }

    } else {
	sb.append(rbs("game not finished", language));
    }
    sb.append("<br /><br />\n");

    // tricks

    for (int i=0; i < 10; i++) {

      ArrayList<Card> trick = g.getTrick(i);
      if (trick == null) break;

      int trickLeader = g.getToMoveByTrickCard(i, 0);
      if (trickLeader < 0) break;

      int points = 1000;
      
      if (gd.type != GameDeclaration.NULL_GAME && trick.size() == 3) {
        // trick was completed - display card point score for trick winner
        SimpleState s;
        if (i == 9) s = g.getCurrentState();
        else        s = g.getStatePriorToTrick(i+1);
        // tomove won last trick
        points = s.getPartyPoints(s.getParty(s.getToMove()));
        if (s.getParty(s.getToMove()) != 1) points = -points; // defenders
      }

      sb.append(String.
                format("<div style=\"float:left; height:170\">"+
                       "  <div style=\"float:left; width:70; position:relative; overflow:visible\">"+
                       "    <div style=\"float:left\"><br /><br />%2d. %-4s</div>"+
                       "  </div>"+
                       "  <div style=\"float:left; width:120; position:relative; overflow:visible\">",
                       (i+1), (points < 1000 ? ""+points : "")
                       ));

      // trick

      sb.append(String.
                format("    <img src=\"%s\" width=\"70\" alt=\"%s\" style=\"position:relative; top:   0; left:0;  z-index:%d;\" />"+
                       "    <img src=\"%s\" width=\"70\" alt=\"%s\" style=\"position:relative; top: -85; left:40; z-index:%d;\" />"+
                       "    <img src=\"%s\" width=\"70\" alt=\"%s\" style=\"position:relative; top:-170; left:20; z-index:%d;\" />",
                       trickCardFile(trick, 4+decl-trickLeader), trick.get((4+decl-trickLeader) % 3).toString(), (4+decl-trickLeader) % 3,
                       trickCardFile(trick, 5+decl-trickLeader), trick.get((5+decl-trickLeader) % 3).toString(), (5+decl-trickLeader) % 3,
                       trickCardFile(trick, 6+decl-trickLeader), trick.get((6+decl-trickLeader) % 3).toString(), (6+decl-trickLeader) % 3
                       ));

      sb.append(String.
                format("  </div>"+
                       "  <div style=\"width:620; float:left\">"+
                       "    <div style=\"width:620; float:left\">"+
                       "      <div style=\"position: relative; height:80; width:310; overflow:hidden; float: left\">"+
                       "        <center>"));

      // top left: declarer + 1

      int cn = Hand.toCardArray(g.getHandPriorToTrick((decl+1) % 3, i), cards);
      Arrays.sort(cards, 0, cn, new CardComparer(gd.type, false));
      
      int l2 = 0;
      for (int j=0; j < cn; j++) {
	  sb.append(String.format("<img src=\"%s\" width=\"70\" alt=\"%s\" style=\"margin-left:%d\" />",
				  cardFile(cards[j]), cards[j].toString(), l2));
        l2 = -50;
      }
  
      sb.append(String.
                format("        </center>"+
                       "      </div>"+
                       "      <div style=\"position: relative; height:80; width:310; overflow:hidden; float: left\">"+
                       "        <center>"));

      // top right: declarer + 2
      cn = Hand.toCardArray(g.getHandPriorToTrick((decl+2) % 3, i), cards);
      Arrays.sort(cards, 0, cn, new CardComparer(gd.type, false));
      
      l2 = 0;
      for (int j=0; j < cn; j++) {
	  sb.append(String.format("<img src=\"%s\" width=\"70\" alt=\"%s\" style=\"margin-left:%d\" />",
				  cardFile(cards[j]), cards[j].toString(), l2));
        l2 = -50;
      }
  
      sb.append(String.
                format("        </center>"+
                       "      </div>"+
                       "    </div>"+
                       "    <div style=\"height:80; width:620; overflow:hidden; float left\">"+
                       "      <center>"+
                       "      <div style=\"float:left\"><br /><br />%s:</div>",
                       rbs(handNames[decl], language)
                       ));

      // bottom (declarer)

      cn = Hand.toCardArray(g.getHandPriorToTrick(decl % 3, i), cards);
      Arrays.sort(cards, 0, cn, new CardComparer(gd.type, false));
      
      l2 = -60;
      for (int j=0; j < cn; j++) {
	  sb.append(String.format("<img src=\"%s\" width=\"70\" alt=\"%s\" style=\"margin-left:%d\" />",
				  cardFile(cards[j]), cards[j].toString(), l2));
        l2 = -30;
      }

      sb.append(String.
                format("      </center>"+
                       "    </div>"+
                       "  </div>"+
                       "</div>"));

      // -----------

      if (false) {
        sb.append(String.
                  format(Misc.locEn,
                         "<div>\n"+
                         "<div style=\"float:left; width:%d; position:relative; overflow:visible\">\n"+
                         "<div style=\"float:left\"><br /><br />%d. %s</div>\n" +
                         "</div>\n"+
                         "<div style=\"float:left; width:120; position:relative; overflow:visible\">\n",
                         w, (i+1), (points < 1000 ? ""+points : "")));

        sb.append(String.
                  format(Misc.locEn,
                         "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"position:relative; top:0;    left:0;  z-index:%d;\" />\n"+
                         "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"position:relative; top:-80;  left:30; z-index:%d;\" />\n"+
                         "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"position:relative; top:-160; left:15; z-index:%d;\" />\n",
                         "/cards/CA.gif", w, "CA", (3+trickLeader) % 3,
                         "/cards/CA.gif", w, "CA", (4+trickLeader) % 3,
                         "/cards/CA.gif", w, "CA", (5+trickLeader) % 3));

        sb.append("</div>\n"+
                  "<div style=\"width:620; float:left\">\n");

        // MH
        sb.append("<div style=\"position: relative; height:80; width:310; overflow:hidden; float:left\">\n"+
                  "<center>\n");
        int l = 0;
        for (int j=0; j < 10; j++) {
          sb.append(String.
                    format(Misc.locEn,
                           "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"margin-left:%d; margin-bottom:%d;\" />\n",
                           "/cards/CA.gif", w, "CA", l, (j == 0 ? 10 : 0)));
          l = -50;
        }
        sb.append("</center>\n"+
                  "</div>\n");

        // RH
        sb.append("<div style=\"position: relative; height:80; width:310; overflow:hidden; float:left\">\n"+
                  "<center>\n");
        l = 0;
        for (int j=0; j < 10; j++) {
          sb.append(String.
                    format(Misc.locEn,
                           "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"margin-left:%d; margin-bottom:%d;\" />\n",
                           "/cards/CA.gif", w, "CA", l, (j == 0 ? 10 : 0)));
          l = -50;
        }

        sb.append("</center>"+
                  "</div>");

        // FH
      
        sb.append("<br />\n"+
                  "<div style=\"height:80; width:620; overflow:hidden; float left\">\n"+
                  "<center>\n"+
                  "<div style=\"float:left\"><br />&nbsp;&nbsp;&nbsp;FH</div>\n");

        l = -60;
        for (int j=0; j < 10; j++) {
          sb.append(String.
                    format(Misc.locEn,
                           "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"margin-left:%d; margin-bottom:%d;\" />\n",
                           "/cards/CA.gif", w, "CA", l, (j == 0 ? 10 : 0)));
          l = -30;
        }
        sb.append("</center>"+
                  "</div>"+
                  "</div>"+
                  "</div>"+
                  "<br />\n");

      }
    }
    return sb.toString();
  }

  static public String localGame2HTML(SimpleGame g, String language, boolean fromSkatTool)
  {
    StringBuffer sb = new StringBuffer();

    if (g.getNumMoves() < 1)
	return rbs("no deal", language);
    
    SimpleState st = g.getState(1); // after dealing
    SimpleState cst = g.getCurrentState(); // game end
    Card[] cards = new Card[10];

    int x = 0;
    int y = 0;
    int w = 70;

    // hands + max bid
    for (int p=0; p < 3; p++) {
      int cn = Hand.toCardArray(st.getHand(p), cards);
      Arrays.sort(cards, new CardComparer(GameDeclaration.GRAND_GAME, false));
      int z = 0;
      int l = 0;
      sb.append("<span style=\"vertical-align:200%; width:40; float:left\">" + rbs(handNames[p], language) + "</span>");
      sb.append("<span>\n");
      for (int i=0; i < cn; i++) {
        if (i > 0) l = -40;
        sb.append(String.format(Misc.locEn,
                                "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"margin-left:%d; z-index:%d;\" />",
                                localCardFile(cards[i], fromSkatTool), w, cards[i].toString(), l, z));
        l -= 30;
        z++;
      }
      sb.append("</span>\n");

      String name = g.getPlayerName(p);
      if (name.length() > MAX_PLAYER_NAME_LENGTH)
        name = rbs("Unknown", language);
      
      sb.append("<span style=\"vertical-align:top\">[" + name + "] " + rbs("Bid: ", language) +
                (cst.getMaxBid(p) < 18 ? rbs("pass", language) : cst.getMaxBid(p)) + "</span>\n");
      sb.append("<br />\n");
    }

    // skat
    sb.append(handNames[3] + "<br />");
    sb.append(String.format(Misc.locEn,
                            "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"{position: relative; left: %d; z-index:%d;}\" />",
                            localCardFile(st.getSkat0(), fromSkatTool), w, st.getSkat0().toString(), 0, 0));
    sb.append(String.format(Misc.locEn,
                            "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"{position: relative; left: %d; z-index:%d;}\" />",
                            localCardFile(st.getSkat1(), fromSkatTool), w, st.getSkat1().toString(), 0, 0));
    sb.append("<br />\n");
    
    GameDeclaration gd = g.getGameDeclaration();

    int decl = cst.getDeclarer();
    
    // soloist and game declaration
    if (decl >= 0) {
	sb.append(rbs("Soloist: ", language) + rbs(handNames[decl], language)); 
    } else {
	sb.append(rbs("No soloist", language));
    }
    
    if (gd.type == GameDeclaration.NO_GAME) {
	sb.append(rbs(", no game type declared <br />", language));
    } else {
	sb.append(rbs(", Game: ", language) + rbs(gd.typeToVerboseString(), language) + " "
		  + rbs(gd.modifiersToVerboseString(), language));
      sb.append("<br />\n");

      if (!gd.hand) {
        
        // show 12 card hand and indicate discard

        Card[] c12 = new Card[12];
        int cn = Hand.toCardArray(st.getHand(decl) | st.getSkat(), c12);
        Arrays.sort(c12, new CardComparer(gd.type, false));
        
        int z = 0;
        int l = 0;
        for (int i=0; i < cn; i++) {

          // lower discarded cards
          int lift = 0;
          if (cst.getSkat0().equals(c12[i]) || cst.getSkat1().equals(c12[i])) {
            lift = 10;
          }
          
          sb.append(String.format(Misc.locEn,
                                  "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"{position: relative; left: %d; z-index:%d; top: %d}\" />",
                                  localCardFile(c12[i], fromSkatTool), w, c12[i].toString(), l, z, lift));
          l -= 30;
          z++;
        }

        sb.append("<br /><br />\n");
      }
    }
    
    // result

    if (g.isFinished()) {

      GameResult gr = new GameResult();

      cst.gameResult(gr);

      if (gr.passed)
	  sb.append(rbs("passed", language));
      else {
	  sb.append(rbs("Score: ", language) + gr.declValue);
	  if (gd.type != GameDeclaration.NULL_GAME) {
	      sb.append(" [ " + gr.declCardPoints + rbs(" card points,", language));
          if (gr.matadors > 0)
	      sb.append(rbs(" with ", language) + gr.matadors);
          else
	      sb.append(rbs(" without ", language) + (-gr.matadors));
          if (gr.schwarz)
	      sb.append(rbs(" schwarz", language));
          else if (gr.schneider)
	      sb.append(rbs(" schneider", language));
          if (gr.overbid)
	      sb.append(rbs(" overbid", language));
          if (gr.timeout >= 0)
	      sb.append(rbs(" timeout", language));
          if (gr.resigned)
	      sb.append(rbs(" resigned", language));
          sb.append(" ]");
        }
      }

    } else {
	sb.append(rbs("game not finished", language));
    }
    sb.append("<br /><br />\n");

    // tricks

    for (int i=0; i < 10; i++) {

      ArrayList<Card> trick = g.getTrick(i);
      if (trick == null) break;

      int trickLeader = g.getToMoveByTrickCard(i, 0);
      if (trickLeader < 0) break;

      int points = 1000;
      
      if (gd.type != GameDeclaration.NULL_GAME && trick.size() == 3) {
        // trick was completed - display card point score for trick winner
        SimpleState s;
        if (i == 9) s = g.getCurrentState();
        else        s = g.getStatePriorToTrick(i+1);
        // tomove won last trick
        points = s.getPartyPoints(s.getParty(s.getToMove()));
        if (s.getParty(s.getToMove()) != 1) points = -points; // defenders
      }

      sb.append(String.
                format("<div style=\"float:left; height:170\">"+
                       "  <div style=\"float:left; width:70; position:relative; overflow:visible\">"+
                       "    <div style=\"float:left\"><br /><br />%2d. %-4s</div>"+
                       "  </div>"+
                       "  <div style=\"float:left; width:120; position:relative; overflow:visible\">",
                       (i+1), (points < 1000 ? ""+points : "")
                       ));

      // trick

      sb.append(String.
                format("    <img src=\"%s\" width=\"70\" alt=\"%s\" style=\"position:relative; top:   0; left:0;  z-index:%d;\" />"+
                       "    <img src=\"%s\" width=\"70\" alt=\"%s\" style=\"position:relative; top: -85; left:40; z-index:%d;\" />"+
                       "    <img src=\"%s\" width=\"70\" alt=\"%s\" style=\"position:relative; top:-170; left:20; z-index:%d;\" />",
                       localTrickCardFile(trick, 4+decl-trickLeader, fromSkatTool), trick.get((4+decl-trickLeader) % 3).toString(), (4+decl-trickLeader) % 3,
                       localTrickCardFile(trick, 5+decl-trickLeader, fromSkatTool), trick.get((5+decl-trickLeader) % 3).toString(), (5+decl-trickLeader) % 3,
                       localTrickCardFile(trick, 6+decl-trickLeader, fromSkatTool), trick.get((6+decl-trickLeader) % 3).toString(), (6+decl-trickLeader) % 3
                       ));

      sb.append(String.
                format("  </div>"+
                       "  <div style=\"width:620; float:left\">"+
                       "    <div style=\"width:620; float:left\">"+
                       "      <div style=\"position: relative; height:80; width:310; overflow:hidden; float: left\">"+
                       "        <center>"));

      // top left: declarer + 1

      int cn = Hand.toCardArray(g.getHandPriorToTrick((decl+1) % 3, i), cards);
      Arrays.sort(cards, 0, cn, new CardComparer(gd.type, false));
      
      int l2 = 0;
      for (int j=0; j < cn; j++) {
	  sb.append(String.format("<img src=\"%s\" width=\"70\" alt=\"%s\" style=\"margin-left:%d\" />",
				  localCardFile(cards[j], fromSkatTool), cards[j].toString(), l2));
        l2 = -50;
      }
  
      sb.append(String.
                format("        </center>"+
                       "      </div>"+
                       "      <div style=\"position: relative; height:80; width:310; overflow:hidden; float: left\">"+
                       "        <center>"));

      // top right: declarer + 2
      cn = Hand.toCardArray(g.getHandPriorToTrick((decl+2) % 3, i), cards);
      Arrays.sort(cards, 0, cn, new CardComparer(gd.type, false));
      
      l2 = 0;
      for (int j=0; j < cn; j++) {
	  sb.append(String.format("<img src=\"%s\" width=\"70\" alt=\"%s\" style=\"margin-left:%d\" />",
				  localCardFile(cards[j], fromSkatTool), cards[j].toString(), l2));
        l2 = -50;
      }
  
      sb.append(String.
                format("        </center>"+
                       "      </div>"+
                       "    </div>"+
                       "    <div style=\"height:80; width:620; overflow:hidden; float left\">"+
                       "      <center>"+
                       "      <div style=\"float:left\"><br /><br />%s:</div>",
                       rbs(handNames[decl], language)
                       ));

      // bottom (declarer)

      cn = Hand.toCardArray(g.getHandPriorToTrick(decl % 3, i), cards);
      Arrays.sort(cards, 0, cn, new CardComparer(gd.type, false));
      
      l2 = -60;
      for (int j=0; j < cn; j++) {
	  sb.append(String.format("<img src=\"%s\" width=\"70\" alt=\"%s\" style=\"margin-left:%d\" />",
				  localCardFile(cards[j], fromSkatTool), cards[j].toString(), l2));
        l2 = -30;
      }

      sb.append(String.
                format("      </center>"+
                       "    </div>"+
                       "  </div>"+
                       "</div>"));

      // -----------

      if (false) {
        sb.append(String.
                  format(Misc.locEn,
                         "<div>\n"+
                         "<div style=\"float:left; width:%d; position:relative; overflow:visible\">\n"+
                         "<div style=\"float:left\"><br /><br />%d. %s</div>\n" +
                         "</div>\n"+
                         "<div style=\"float:left; width:120; position:relative; overflow:visible\">\n",
                         w, (i+1), (points < 1000 ? ""+points : "")));

        sb.append(String.
                  format(Misc.locEn,
                         "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"position:relative; top:0;    left:0;  z-index:%d;\" />\n"+
                         "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"position:relative; top:-80;  left:30; z-index:%d;\" />\n"+
                         "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"position:relative; top:-160; left:15; z-index:%d;\" />\n",
                         "/cards/CA.gif", w, "CA", (3+trickLeader) % 3,
                         "/cards/CA.gif", w, "CA", (4+trickLeader) % 3,
                         "/cards/CA.gif", w, "CA", (5+trickLeader) % 3));

        sb.append("</div>\n"+
                  "<div style=\"width:620; float:left\">\n");

        // MH
        sb.append("<div style=\"position: relative; height:80; width:310; overflow:hidden; float:left\">\n"+
                  "<center>\n");
        int l = 0;
        for (int j=0; j < 10; j++) {
          sb.append(String.
                    format(Misc.locEn,
                           "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"margin-left:%d; margin-bottom:%d;\" />\n",
                           "/cards/CA.gif", w, "CA", l, (j == 0 ? 10 : 0)));
          l = -50;
        }
        sb.append("</center>\n"+
                  "</div>\n");

        // RH
        sb.append("<div style=\"position: relative; height:80; width:310; overflow:hidden; float:left\">\n"+
                  "<center>\n");
        l = 0;
        for (int j=0; j < 10; j++) {
          sb.append(String.
                    format(Misc.locEn,
                           "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"margin-left:%d; margin-bottom:%d;\" />\n",
                           "/cards/CA.gif", w, "CA", l, (j == 0 ? 10 : 0)));
          l = -50;
        }

        sb.append("</center>"+
                  "</div>");

        // FH
      
        sb.append("<br />\n"+
                  "<div style=\"height:80; width:620; overflow:hidden; float left\">\n"+
                  "<center>\n"+
                  "<div style=\"float:left\"><br />&nbsp;&nbsp;&nbsp;FH</div>\n");

        l = -60;
        for (int j=0; j < 10; j++) {
          sb.append(String.
                    format(Misc.locEn,
                           "<img src=\"%s\" width=\"%d\" alt=\"%s\" style=\"margin-left:%d; margin-bottom:%d;\" />\n",
                           "/cards/CA.gif", w, "CA", l, (j == 0 ? 10 : 0)));
          l = -30;
        }
        sb.append("</center>"+
                  "</div>"+
                  "</div>"+
                  "</div>"+
                  "<br />\n");

      }
    }
    return sb.toString();
  }

    /* Because this method is static, it cannot access non-static variables.  Also, I cannot
       make this method non-static, since I will then be unable to access it from httpRequest()
       in Server.java.  Therefore, all functionality associated with creating an HTML result
       table from a table archive file must be inside this method.  Otherwise, I would have liked
       to split up the code into at least 3 methods -- table2HTML, which would have called 
       writeHeaders() and writeGameData() -- similar to what happens in HTMLResultTable, so that
       things would not be so cluttered.  But since both writeHeaders() and writeGameData() would
       need to access variables that can be initialized only here in table2HTML and since I cannot
       initialize class variables from table2HTML, all code that would have been in writeHeaders()
       or writeGameData() needs to be in here. - Ryan*/
    public static String table2HTML(String sgfFileName, String language, Table table) {
	Random rand = new Random();
	BufferedReader buff;

	try {
	    buff = new BufferedReader(new FileReader(sgfFileName));
	}
	catch (FileNotFoundException e) {
	    Misc.err("table archive file " + sgfFileName + " not found");
	    return null;
	}

	table.load(rand, buff);
	String dateTime = table.getDateTime();
	String tableId;

	if (table.getId().startsWith("."))
	    // gets rid of the decimal at the beginning of the table ID number
	    tableId = table.getId().substring(1, table.getId().length());
	else if (table.getId().startsWith("t:"))
	    // gets rid of the "t:" at the beginning of a tourney-table ID
	    tableId = table.getId().substring(2, table.getId().length());
	else
	    tableId = table.getId(); // are there any other kinds of table names?

	int numPlayers = table.getPlayerNum();
	String[] playersInTable = table.getPlayersInResultTable();
	ScoreSheet scores = table.getScoreSheet();
	int numGames = table.getGameNum();	

	StringBuffer sb = new StringBuffer();
	boolean colouredBackground = false; // allows lines in the table to have alternating b/g's

        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
	sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n");
	sb.append("   <head>\n");
	sb.append("      <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n");
	sb.append("      <style type=\"text/css\">\n");
	sb.append("         table {\n");
	sb.append("            border-collapse:collapse;\n");
	sb.append("         }\n");
	sb.append("         table, th, td {\n");
	sb.append("            vertical-align:center;\n");
	sb.append("            horizontal-align:center;\n");
	sb.append("         }\n");
	sb.append("         table, td {\n");
	sb.append("            border:2px solid black;\n");
	sb.append("         }\n");
	sb.append("         th {\n");
	sb.append("            border:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td {\n");
	sb.append("            padding:2px;\n");
	sb.append("         }\n");
	sb.append("         tr.alternative td {\n");
	sb.append("            color:#282828;\n");
	sb.append("            background-color:#D4F2DB;\n");
	sb.append("         }\n");
	sb.append("         th.thickexceptright {\n");
	sb.append("            border-right:2px solid black;\n");
	sb.append("            border-left:3px solid black;\n");
	sb.append("            border-bottom:3px solid black;\n");
	sb.append("            border-top:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         th.thickexceptleft {\n");
	sb.append("            border-left:2px solid black;\n");
	sb.append("            border-right:3px solid black;\n");
	sb.append("            border-bottom:3px solid black;\n");
	sb.append("            border-top:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         th.thickbottomtop {\n");
	sb.append("            border-right:2px solid black;\n");
	sb.append("            border-left:2px solid black;\n");
	sb.append("            border-bottom:3px solid black;\n");
	sb.append("            border-top:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thickleft {\n");
	sb.append("            border-left:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thickright {\n");
	sb.append("            border-right:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.verythicktop {\n");
	sb.append("            border-top:4px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thickbottom {\n");
	sb.append("            border-bottom:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thickleftright {\n");
	sb.append("            border-left:3px solid black;\n");
	sb.append("            border-right:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.verythicktop {\n");
	sb.append("            border-top:4px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thicktopleft {\n");
	sb.append("            border-top:4px solid black;\n");
	sb.append("            border-left:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thicktopright {\n");
	sb.append("            border-top:4px solid black;\n");
	sb.append("            border-right:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thicktopleftright {\n");
	sb.append("            border-top:4px solid black;\n");
	sb.append("            border-right:3px solid black;\n");
	sb.append("            border-left:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thick {\n");
	sb.append("            border:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thickexceptright {\n");
	sb.append("            border-left:3px solid black;\n");
	sb.append("            border-right:2px solid black;\n");
	sb.append("            border-top:3px solid black;\n");
	sb.append("            border-bottom:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thickexceptleft {\n");
	sb.append("            border-left:2px solid black;\n");
	sb.append("            border-right:3px solid black;\n");
	sb.append("            border-top:3px solid black;\n");
	sb.append("            border-bottom:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         td.thickesptop {\n");
	sb.append("            border-top:4px solid black;\n");
	sb.append("            border-bottom:3px solid black;\n");
	sb.append("            border-right:3px solid black;\n");
	sb.append("            border-left:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         #rowthickbottom {\n");
	sb.append("            border-bottom:3px solid black;\n");
	sb.append("         }\n");
	sb.append("         img {\n");
	sb.append("            border:0px;\n");
	sb.append("            height:auto;\n");
	sb.append("            width:auto;\n");
	sb.append("         }\n");
       	sb.append("         h2 {\n");
       	sb.append("	       text-align:center;\n");
       	sb.append("         }\n");
	sb.append("      </style>\n");

	/* Are round numbers not saved in table archive files?  Would eventually like to specify 
	   tournament name, round number, and table name (or maybe just assign a number to each
	   table in a tournament and use the number as the name) to page title.*** */
	sb.append("      	<title>");
	sb.append(rbs("Results of List for Table ", language));
	sb.append(tableId + "</title>\n");
	sb.append("      </head>\n");
	sb.append("<body>\n");
	sb.append("<h2>");
	sb.append(rbs("Results of List for Table ", language));
	sb.append(tableId + "</h2>\n");
	sb.append("  	<table>\n");
	sb.append("	<tr align=\"left\">\n");

	if (numPlayers == 4)
	    sb.append("			 	     <th colspan=\"20\">" + rbs("Date: ", language));
	else
	    sb.append("                               <th colspan=\"17\">" + rbs("Date: ", language));

	sb.append(dateTime);
	sb.append("</th>\n");

	sb.append("				<th colspan=\"4\">");
	sb.append(rbs("Round", language));
	sb.append(":</th>\n");
	sb.append("				<th colspan=\"5\">");
	sb.append(rbs("Table", language) + ": " + tableId); // for tournament tables, better to have table name than series ID
	sb.append("</th>\n");
	sb.append("	</tr>\n");
	sb.append("	<tr align=\"center\">\n");
	sb.append("				<th rowspan=\"2\"><img src=\"/images/");
	sb.append("gn" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"#\" /></th>\n");
	sb.append("				<th rowspan=\"2\">&nbsp;&nbsp;<img src=\"/images/");
	sb.append("bv" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"Value\" />&nbsp;&nbsp;</th>\n");
	sb.append("				<th rowspan=\"2\">&nbsp;<img src=\"/images/");
	sb.append("wi" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"With\" />&nbsp;</th>\n");
	sb.append("				<th rowspan=\"2\"><img src=\"/images/");
	sb.append("wo" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"Without\" /></th>\n");
	sb.append("				<th colspan=\"7\">" + rbs("Modifiers", language) + "</th>\n");
	sb.append("				<th colspan=\"2\">" + rbs("Score", language) + "</th>\n");
	sb.append("				<th colspan=\"4\" align=\"center\">" + playersInTable[0] + "</th>\n");
	sb.append("				<th colspan=\"4\" align=\"center\">" + playersInTable[1] + "</th>\n");
	sb.append("				<th colspan=\"4\" align=\"center\">" + playersInTable[2] + "</th>\n");

	if (numPlayers == 4) {
	    sb.append("				<th colspan=\"4\" align=\"center\">" + playersInTable[3] + "</th>\n");
	}

	sb.append("				<th rowspan=\"2\"><img src=\"/images/");
	sb.append("gp" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"Pass\" /></th>\n");
	sb.append("	</tr>\n");
	sb.append("	<tr align=\"center\">\n");
	sb.append("				<th><img src=\"/images/");
	sb.append("hn" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"H\" /></th>\n");
	sb.append("				<th><img src=\"/images/");
	sb.append("sc" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"S\" /></th>\n");
	sb.append("				<th><img src=\"/images/");
	sb.append("sa" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"SA\" /></th>\n");
	sb.append("				<th><img src=\"/images/");
	sb.append("sz" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"Z\" /></th>\n");
	sb.append("				<th><img src=\"/images/");
	sb.append("za" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"ZA\" /></th>\n");
	sb.append("				<th><img src=\"/images/");
	sb.append("ou" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"O\" /></th>\n");
	sb.append("				<th><img src=\"/images/");
	sb.append("ov" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	sb.append("									 alt=\"V\" /></th>\n");
	sb.append("				<th class=\"thickexceptright\">&nbsp;+&nbsp;</th>\n");
	sb.append("				<th class=\"thickexceptleft\">&nbsp;-&nbsp;</th>\n");

	for (int index = 0; index < numPlayers; index++) {
	    sb.append("				<th class=\"thickexceptright\">&nbsp;&nbsp;" + rbs("Seat", language) + " " + (index+1) + "&nbsp;&nbsp;</th>\n");
	    sb.append("				<th class=\"thickbottomtop\"><img src=\"/images/");
	    sb.append("wn" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	    sb.append("									 alt=\"W\" /></th>\n");
	    sb.append("				<th class=\"thickbottomtop\"><img src=\"/images/");
	    sb.append("ls" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	    sb.append("									 alt=\"L\" /></th>\n");  
	    sb.append("				<th class=\"thickexceptleft\"><img src=\"/images/");
	    sb.append("to" + (language.equals("en") ? "" : ("-" + language)) + ".gif\"\n");
	    sb.append("									 alt=\"T\" /></th>\n");  
	}

	sb.append("	</tr>\n");
	sb.append("\n");

	String[] directories = sgfFileName.split("/");
	StringBuffer gameLink = new StringBuffer(); // for creating links to indiv. game histories

	for (int index = 0; index < numGames; index++) {

	    ScoreSheet.Row row = scores.getRow(index);
	    int declarer = row.declarer;

	    if (colouredBackground)
		sb.append("<tr class=\"alternative\"");
	    else
		sb.append("<tr");

	    if ((index + 1) % numPlayers == 0)
		sb.append(" id=\"rowthickbottom\">\n");
	    else
		sb.append(">\n");

	    colouredBackground = !colouredBackground;

	    gameLink.delete(0, gameLink.length());
	    gameLink.append(directories[directories.length - 1]);
	    gameLink.append(".");
	    gameLink.append(index);
	    gameLink.append(".game");

	    // the game number becomes a link to the complete graphical history of that game
	    sb.append("   <td align=\"left\" class=\"thickleftright\">");
	    sb.append("<a href=\"");
	    sb.append(gameLink);
	    sb.append("\">");
	    sb.append((index + 1));
	    sb.append("</a></td>\n");

	    if (declarer < 0) {
		/* All players passed, so all cells in the current row of the table
		   should be blank except for the last. */

		sb.append("   <td class=\"thickleftright\"></td>\n");
		sb.append("   <td class=\"thickleftright\"></td>\n");
		sb.append("   <td class=\"thickleft\"></td>\n");
		sb.append("   <td class=\"thickright\"></td>\n");
		sb.append("   <td class=\"thickleft\"></td>\n");
		sb.append("   <td></td>\n");
		sb.append("   <td></td>\n");
		sb.append("   <td></td>\n");
		sb.append("   <td></td>\n");
		sb.append("   <td class=\"thickright\"></td>\n");
		sb.append("   <td class=\"thickleft\"></td>\n");
		sb.append("   <td class=\"thickright\"></td>\n");

		for (int index2 = 0; index2 < numPlayers; index2++) {
		    sb.append("   <td class=\"thickleft\"></td>\n");
		    sb.append("   <td></td>\n");
		    sb.append("   <td></td>\n");
		    sb.append("   <td class=\"thickright\"></td>\n");		    
		}

		sb.append("   <td align=\"center\" class=\"thickleftright\">");
		sb.append(row.cumuPass);
		sb.append("</td>\n");
		sb.append("</tr>\n");
	    } else {
		// If not all players passed:

		// append game value
		sb.append("   <td align=\"right\" class=\"thickleftright\">");
		sb.append(row.baseValue);
		sb.append("</td>\n");

		if (row.matadors > 0) {
		    sb.append("   <td align=\"right\" class=\"thickleft\">");
		    sb.append(row.matadors);
		    sb.append("</td>\n");

		    sb.append("   <td class=\"thickright\"></td>\n");
		} else {
		    sb.append("   <td class=\"thickleft\"></td>\n");
		    
		    sb.append("   <td align=\"right\" class=\"thickright\">");
		    sb.append((-row.matadors));
		    sb.append("</td>\n");
		}

		if (row.hand)
		    sb.append("   <td align=\"center\" class=\"thickleft\">X</td>\n");
		else
		    sb.append("   <td class=\"thickleft\"></td>\n");		    

		if (row.schneider)
		    sb.append("   <td align=\"center\">X</td>\n");
		else
		    sb.append("   <td></td>\n");		    

		if (row.schneiderAnnounced)
		    sb.append("   <td align=\"center\">X</td>\n");
		else
		    sb.append("   <td></td>\n");		    

		if (row.schwarz)
		    sb.append("   <td align=\"center\">X</td>\n");
		else
		    sb.append("   <td></td>\n");		    

		if (row.schwarzAnnounced)
		    sb.append("   <td align=\"center\">X</td>\n");
		else
		    sb.append("   <td></td>\n");		    

		if (row.open)
		    sb.append("   <td align=\"center\">X</td>\n");
		else
		    sb.append("   <td></td>\n");		    

		if (row.overbid)
		    sb.append("   <td align=\"center\" class=\"thickright\">X</td>\n");
		else
		    sb.append("   <td></td>\n");

		if (row.score > 0) {
		    sb.append("   <td align=\"right\" class=\"thickleft\">");
		    sb.append(row.score);
		    sb.append("</td>\n");

		    sb.append("   <td class=\"thickright\"></td>\n");
		} else {
		    sb.append("   <td class=\"thickleft\"></td>\n");
		    
		    sb.append("   <td align=\"right\" class=\"thickright\">");
		    sb.append((-row.score));
		    sb.append("</td>\n");
		}

		int declarerIndex = (declarer + index + 1) % numPlayers;
		int timeoutIndex;
		int numBlankPlayers;
		int numBlankPlayersAfter; // number of blank cells after both timeout and declarer data have been printed

		if (row.p0 > 0)
		    timeoutIndex = (index + 1) % numPlayers;
		else if (row.p1 > 0)
		    timeoutIndex = (index + 2) % numPlayers;
		else if (row.p2 > 0)
		    timeoutIndex = (index + 3) % numPlayers;
		else {
		    timeoutIndex = numPlayers;
		}

		if (timeoutIndex == numPlayers) {
		    for (int index3 = 0; index3 < declarerIndex; index3++) {
			sb.append("   <td class=\"thickleft\"></td>\n");
			sb.append("   <td></td>\n");
			sb.append("   <td></td>\n");
			sb.append("   <td class=\"thickright\"></td>\n");
		    }

		    // print data for declarer
		    sb.append("   <td align=\"right\" class=\"thickleft\">");
		    sb.append(row.cumulative[declarerIndex].score);
		    sb.append("</td>\n");
		    
		    if (row.score > 0) {
			sb.append("   <td align=\"right\">");
			sb.append(row.cumulative[declarerIndex].wins);
			sb.append("</td>\n");

			sb.append("   <td></td>\n");
			sb.append("   <td class=\"thickright\"></td>\n");
		    } else {
			sb.append("   <td></td>\n");
			
			sb.append("   <td align=\"right\">");
			sb.append(row.cumulative[declarerIndex].losses);
			sb.append("</td>\n");	
			sb.append("   <td class=\"thickright\"></td>\n");
		    }

		    numBlankPlayersAfter = numPlayers - declarerIndex - 1;

		} else {

		    if (timeoutIndex == declarerIndex) {
			// declarer timed out

			for (int index3 = 0; index3 < declarerIndex; index3++) {
			    sb.append("   <td class=\"thickleft\"></td>\n");
			    sb.append("   <td></td>\n");
			    sb.append("   <td></td>\n");
			    sb.append("   <td class=\"thickright\"></td>\n");
			}
			
			// print data for declarer
			sb.append("   <td align=\"right\" class=\"thickleft\">");
			sb.append(row.cumulative[declarerIndex].score);
			sb.append("</td>\n");

			sb.append("   <td></td>\n");
			
			sb.append("   <td align=\"right\">");
			sb.append(row.cumulative[declarerIndex].losses);
			sb.append("</td>\n");

			sb.append("   <td align=\"right\" class=\"thickright\">");
			sb.append(row.cumulative[declarerIndex].penalties);
			sb.append("</td>\n");

			numBlankPlayersAfter = numPlayers - declarerIndex - 1;

		    } else {
			// player other than declarer timed out

			numBlankPlayers = Math.min(timeoutIndex, declarerIndex);

			for (int index3 = 0; index3 < numBlankPlayers; index3++) {
			    sb.append("   <td class=\"thickleft\"></td>\n");
			    sb.append("   <td></td>\n");
			    sb.append("   <td></td>\n");
			    sb.append("   <td class=\"thickright\"></td>\n");
			}

			if (timeoutIndex < declarerIndex) {
			    // add 1 to player's timeouts
			    sb.append("   <td class=\"thickleft\"></td>\n");
			    sb.append("   <td></td>\n");
			    sb.append("   <td></td>\n");
			    sb.append("   <td class=\"thickright\">");
			    // ***Defender timeouts aren't saved in the ScoreSheet object?!
			    sb.append(row.cumulative[timeoutIndex].penalties);
			    sb.append("</td>\n");

			    int numBlankPlayers2 = declarerIndex - timeoutIndex;

			    for (int index3 = 0; index3 < numBlankPlayers; index3++) {
				sb.append("   <td class=\"thickleft\"></td>\n");
				sb.append("   <td></td>\n");
				sb.append("   <td></td>\n");
				sb.append("   <td class=\"thickright\"></td>\n");
			    }

			    // print data for declarer
			    sb.append("   <td align=\"right\" class=\"thickleft\">");
			    sb.append(row.cumulative[declarerIndex].score);
			    sb.append("</td>\n");
			    
			    if (row.score > 0) {
				sb.append("   <td align=\"right\">");
				sb.append(row.cumulative[declarerIndex].wins);
				sb.append("</td>\n");
				
				sb.append("   <td></td>\n");
				sb.append("   <td class=\"thickright\"></td>\n");
			    } else {
				sb.append("   <td></td>\n");
				
				sb.append("   <td align=\"right\">");
				sb.append(row.cumulative[declarerIndex].losses);
				sb.append("</td>\n");	
				sb.append("   <td class=\"thickright\"></td>\n");
			    }

			    numBlankPlayersAfter = numPlayers - declarerIndex - 1;

			} else {

			    // print data for declarer first
			    sb.append("   <td align=\"right\" class=\"thickleft\">");
			    sb.append(row.cumulative[declarerIndex].score);
			    sb.append("</td>\n");
			    
			    if (row.score > 0) {
				sb.append("   <td align=\"right\">");
				sb.append(row.cumulative[declarerIndex].wins);
				sb.append("</td>\n");
				
				sb.append("   <td></td>\n");
				sb.append("   <td></td>\n");
			    } else {
				sb.append("   <td></td>\n");
				
				sb.append("   <td align=\"right\">");
				sb.append(row.cumulative[declarerIndex].losses);
				sb.append("</td>\n");	
				sb.append("   <td class=\"thickright\"></td>\n");
			    }

			    int numBlankPlayers2 = timeoutIndex - declarerIndex;

			    for (int index3 = 0; index3 < numBlankPlayers2; index3++) {
				sb.append("   <td class=\"thickleft\"></td>\n");
				sb.append("   <td></td>\n");
				sb.append("   <td></td>\n");
				sb.append("   <td class=\"thickright\"></td>\n");
			    }

			    // then add 1 to player's timeouts
			    sb.append("   <td class=\"thickleft\"></td>\n");
			    sb.append("   <td></td>\n");
			    sb.append("   <td></td>\n");
			    sb.append("   <td class=\"thickright\">");
			    sb.append(row.cumulative[timeoutIndex].penalties);
			    sb.append("</td>\n");

			    numBlankPlayersAfter = numPlayers - timeoutIndex - 1;
			}	
		    }
		}

		for (int index4 = 0; index4 < numBlankPlayersAfter; index4++) {
		    sb.append("   <td class=\"thickleft\"></td>\n");
		    sb.append("   <td></td>\n");
		    sb.append("   <td></td>\n");
		    sb.append("   <td class=\"thickright\"></td>\n");
		}

		sb.append("   <td class=\"thickleftright\"></td>\n");
		sb.append("</tr>\n");
		sb.append("\n");
	    }
	}

	// May put code below into a separate method called writeEndGameData().

	ScoreSheet.Row row = scores.getRow(numGames - 1);

	if (colouredBackground)
	    sb.append("<tr class=\"alternative\">\n");
	else
	    sb.append("<tr>\n");

	colouredBackground = !colouredBackground;

	sb.append("   <td align=\"left\" colspan=\"13\" class=\"thickesptop\"><b>" + rbs("Totals", language) + "</b></td>\n");

	for (int index = 0; index < numPlayers; index++) {
	    sb.append("   <td align=\"right\" class=\"thicktopleft\">");
	    sb.append(row.cumulative[index].score);
	    sb.append("</td>\n");
	    
	    sb.append("   <td align=\"right\" class=\"verythicktop\">");
	    sb.append(row.cumulative[index].wins);
	    sb.append("</td>\n");
	    
	    sb.append("   <td align=\"right\" class=\"verythicktop\">");
	    sb.append(row.cumulative[index].losses);
	    sb.append("</td>\n");

	    sb.append("   <td align=\"right\" class=\"thicktopright\">");
	    sb.append(row.cumulative[index].penalties);
	    sb.append("</td>\n");
	}

	sb.append("   <td align=\"right\" class=\"thicktopleftright\">");
	sb.append(row.cumuPass);
	sb.append("</td>\n");
	sb.append("</tr>\n");

	if (colouredBackground)
	    sb.append("<tr class=\"alternative\">\n");
	else
	    sb.append("<tr>\n");

	colouredBackground = !colouredBackground;

	sb.append("   <td align=\"left\" colspan=\"13\" class=\"thick\"><b>" + rbs("(Won - Lost) * 50", language) + "</b></td>\n");

	ArrayList<Integer> winPoints = new ArrayList<Integer>();
	
	for (int index = 0; index < numPlayers; index++) {
	    int winPts = (row.cumulative[index].wins - row.cumulative[index].losses) * 50;
	    
	    sb.append("   <td align=\"right\" class=\"thickleft\">");
	    sb.append(winPts);
	    sb.append("</td>\n");
	    sb.append("   <td colspan=\"3\" class=\"thickright\"></td>\n");
	    
	    winPoints.add(winPts);
	}

	sb.append("   <td class=\"thick\"></td>\n");
	sb.append("</tr>\n");
	
	if (colouredBackground)
	    sb.append("<tr class=\"alternative\">\n");
	else
	    sb.append("<tr>\n");

	colouredBackground = !colouredBackground;
	
	sb.append("   <td align=\"left\" colspan=\"13\" class=\"thick\"><b>" + rbs("Opponent Losses * ", language));
	
	if (numPlayers == 3)
	    sb.append("40");
	else
	    sb.append("30");
	
	sb.append("</b></td>\n");
	
	ArrayList<Integer> lossPoints = new ArrayList<Integer>();
	
	for (int index = 0; index < numPlayers; index++) {
	    int losses = 0;
	    
	    for (int index2 = 0; index2 < numPlayers; index2++) {
		if (index2 != index)
		    losses += row.cumulative[index2].losses; 
	    }
	    
	    int lossPts;
	    
	    if (numPlayers == 3)
		lossPts = losses*40;
	    else
		lossPts = losses*30;
	    
	    sb.append("   <td align=\"right\" class=\"thickleft\">");
	    sb.append(lossPts);
	    sb.append("</td>\n");
	    sb.append("   <td colspan=\"3\" class=\"thickright\"></td>\n");
	    
	    lossPoints.add(lossPts);
	}
	
	sb.append("   <td class=\"thick\" rowspan=\"3\"></td>\n");
	sb.append("</tr>\n");

	if (colouredBackground)
	    sb.append("<tr class=\"alternative\">\n");
	else
	    sb.append("<tr>\n");

	colouredBackground = !colouredBackground;     

	sb.append("   <td align=\"left\" colspan=\"13\" class=\"thick\"><b>" + rbs("Timeouts * -100", language) + "</b></td>\n");
	
	for (int index = 0; index < numPlayers; index++) {
	    sb.append("   <td align=\"right\" class=\"thickleft\"><b>");
	    int timeoutPts = -row.cumulative[index].penalties*Table.PENALTY_PTS;
	    sb.append(timeoutPts);
	    sb.append("</b></td>\n");	
	    sb.append("   <td colspan=\"3\" class=\"thickright\"></td>\n");
	}

	sb.append("</tr>\n");
	
	if (colouredBackground)
	    sb.append("<tr class=\"alternative\">\n");
	else
	    sb.append("<tr>\n");

	colouredBackground = !colouredBackground;     

	sb.append("   <td align=\"left\" colspan=\"13\" class=\"thick\"><b>" + rbs("End Result", language) + "</b></td>\n");
	
	for (int index = 0; index < numPlayers; index++) {
	    int totalPts = row.cumulative[index].score + winPoints.get(index) + lossPoints.get(index)
		- (row.cumulative[index].penalties*Table.PENALTY_PTS);
	    
	    sb.append("   <td align=\"right\" class=\"thickexceptright\"><b>");
	    sb.append(totalPts);
	    sb.append("</b></td>\n");	
	    sb.append("   <td align=\"center\" class=\"thickexceptleft\" colspan=\"3\">");
	    sb.append(row.cumulative[index].losses);
	    sb.append("</td>\n");
	}

	sb.append("</tr></table></body></html>");

	return sb.toString();
    }
}