/* Result table is created and saved to the local machine (in a subdirectory game_results
 * of the directory from which SkatGUI is run) whenever the user exits a table (either
 * by clicking "Leave" or by exiting the SkatGUI altogether).  Still doesn't deal with
 * disconnects and reconnects.
 *
 * (c) Ryan Lagerquist
 */

package gui;

import common.*;
import java.util.*;
import java.awt.*;
import java.io.*;
import java.lang.*;

public class HTMLResultTable
{
    StringBuffer tableInfo = new StringBuffer();
    PrintWriter htmlWriter;
    StringBuffer outputFileName = new StringBuffer();
    java.util.ResourceBundle bundle;
    final static String resourceFile = "data/i18n/gui/Lists";

    Table table;
    ScoreSheet scores;
    int numPlayers;
    int numGames;
    /* If HTML tables must be printed for more than one table at a time, there needs to
       be a way to give them unique file names.  This constant is used as the ending
       condition for a for-loop which produces unique file names, as it's highly doubtful
       that a player is going to be playing more than 20 tables at the same time! */
    final static int NUM_FILES = 20;
    // So that lines in the table can alternate between white b/g and coloured b/g.
    boolean colouredBackground = false;

    public HTMLResultTable(Table table) {
	this.table = table;
	numPlayers = table.getPlayerNum();
	bundle = java.util.ResourceBundle.getBundle(resourceFile);
	scores = table.getScoreSheet();
	numGames = scores.size();
	if (numGames > 0) writeTable();
    }

    String rbs0(String s) { return s; }
    String rbs(String s) { return bundle.getString(s); }

    public void writeTable() {

	writeHeaders();
	writeGameData();

	/* All code from here to the next operation on outputFileName creates directories
	   in which to store HTML tables, if they do not already exist.  The directory
	   structure begins in the directory from which the user starts SkatGUI, with the
	   the directory game_results.  Inside game_results will be a directory for each
	   month; and inside each month directory will be a directory for each date.
	   The date directories will contain the result tables, and the file name of each
	   result table will indicate the time at which it was printed. */

	String resultDirectoryName = rbs("game_results");
	File resultDirectory = new File(resultDirectoryName);
	
	try {
	    resultDirectory.mkdir();
	}
	catch (SecurityException e) {
	    if (!resultDirectory.exists())
		Misc.err("Directory " + resultDirectoryName + " could not be created.");
	}

	String UTCYear = Misc.currentUTCdate().substring(0, 4);
	String UTCMonth = Misc.currentUTCdate().substring(5, 7);
	String monthDirectoryNamePartial = UTCMonth + "_" + UTCYear;

	/*
	if (UTCMonth.startsWith("0"))
	    month = UTCMonth.substring(1, UTCMonth.length());
	else
	    month = UTCMonth;

	int monthNum = Integer.parseInt(month);
	String monthDirectoryNamePartial="";

	switch (monthNum) {
	case 1:
	    monthDirectoryNamePartial = rbs("january") + "_" + UTCYear;
	    break;
	case 2:
	    monthDirectoryNamePartial = rbs("february") + "_" + UTCYear;
	    break;
	case 3:
	    monthDirectoryNamePartial = rbs("march") + "_" + UTCYear;
	    break;
	case 4:
	    monthDirectoryNamePartial = rbs("april") + "_" + UTCYear;
	    break;
	case 5:
	    monthDirectoryNamePartial = rbs("may") + "_" + UTCYear;
	    break;
	case 6:
	    monthDirectoryNamePartial = rbs("june") + "_" + UTCYear;
	    break;
	case 7:
	    monthDirectoryNamePartial = rbs("july") + "_" + UTCYear;
	    break;
	case 8:
	    monthDirectoryNamePartial = rbs("august") + "_" + UTCYear;
	    break;
	case 9:
	    monthDirectoryNamePartial = rbs("september") + "_" + UTCYear;
	    break;
	case 10:
	    monthDirectoryNamePartial = rbs("october") + "_" + UTCYear;
	    break;
	case 11:
	    monthDirectoryNamePartial = rbs("november") + "_" + UTCYear;
	    break;
	case 12:
	    monthDirectoryNamePartial = rbs("december") + "_" + UTCYear;
	    break;
	}
	*/

	String monthDirectoryName = resultDirectoryName + "/" + monthDirectoryNamePartial;
	File monthDirectory = new File(monthDirectoryName);
	
	try {
	    monthDirectory.mkdir();
	}
	catch (SecurityException e) {
	    if (!monthDirectory.exists())
		Misc.err("Directory " + monthDirectoryName + " could not be created.");
	}

	String dateDirectoryNamePartial = Misc.currentUTCdate().substring(8, 10);

	/*
	if (Misc.currentUTCdate().substring(8, 10).startsWith("0"))
	    UTCDate = Misc.currentUTCdate().substring(9, 10);
	else
	    UTCDate = Misc.currentUTCdate().substring(8, 10);

	int UTCDateNum = Integer.parseInt(UTCDate);
	String dateDirectoryNamePartial="";

	switch (UTCDateNum) {
	case 1:
	    dateDirectoryNamePartial = UTCDate + rbs("st");
	    break;
	case 2:
	    dateDirectoryNamePartial = UTCDate + rbs("nd");
	    break;
	case 3:
	    dateDirectoryNamePartial = UTCDate + rbs("rd");
	    break;
	case 21:
	    dateDirectoryNamePartial = UTCDate + rbs("st_two_digits");
	    break;
	case 22:
	    dateDirectoryNamePartial = UTCDate + rbs("nd_two_digits");
	    break;
	case 23:
	    dateDirectoryNamePartial = UTCDate + rbs("rd_two_digits");
	    break;
	case 31:
	    dateDirectoryNamePartial = UTCDate + rbs("st_two_digits");
	    break;
	default:
	    dateDirectoryNamePartial = UTCDate + rbs("th");
	}
	*/

	String dateDirectoryName = monthDirectoryName + "/" + dateDirectoryNamePartial;
	File dateDirectory = new File(dateDirectoryName);

	try {
	    dateDirectory.mkdir();
	}
	catch (SecurityException e) {
	    if (!dateDirectory.exists())
		Misc.err("Directory " + dateDirectoryName + " could not be created.");
	}

	/*
	for (int index = 0; index < NUM_FILES; index++) {
	    outputFileName.delete(0, outputFileName.length());
	    outputFileName.append(dateDirectoryName);
	    outputFileName.append("/");
	    // The colons in the UTC time must be replaced, because colons are illegal characters in Windows file names.
	    outputFileName.append(Misc.currentUTCdate().substring(11, 19).replace(':', '.'));
	    outputFileName.append("_");
	    outputFileName.append(rbs("results"));
	    if (index != 0) outputFileName.append(index);
	    outputFileName.append(".html");
	    
	    File file = new File(outputFileName.toString());
	    if (!file.exists())
		break;
	}
	*/

	for (int index = 0; index < NUM_FILES; index++) {
	    outputFileName.delete(0, outputFileName.length());
	    outputFileName.append(dateDirectoryName);
	    outputFileName.append("/");
	    
	    Long seriesId = new Long(table.getSeriesId());

	    // For some reason every series ID is 0 now?!
	    if (seriesId != null && table.getSeriesId() != 0)
		outputFileName.append(table.getSeriesId());
	    else if (table.getId().startsWith("t:"))
		outputFileName.append(table.getId().substring(2, table.getId().length()));
	    else
		outputFileName.append(Misc.currentUTCdate().substring(11, 19).replace(':', '-'));
	    outputFileName.append("_");
	    outputFileName.append(rbs("scores"));
	    if (index != 0) outputFileName.append(index);
	    outputFileName.append(".html");

	    File file = new File(outputFileName.toString());
	    if (!file.exists())
		break;
	}

	try {
	    htmlWriter = new PrintWriter(new FileWriter(outputFileName.toString()));
	    htmlWriter.print(tableInfo);
	    htmlWriter.close();
	}
	catch (IOException e) {
	    Misc.err("failed to print HTML result table");
	}
    }

    public void writeHeaders() {

	String tableId;

	/* May delete this code later, since I'm now using series ID to identify the table.
	   Guarantees uniqueness, since series IDs are non-repeating while table ID numbers
	   repeat all the time. */
	if (table.getId().startsWith("."))
	    // gets rid of the decimal at the beginning of the table ID number
	    tableId = table.getId().substring(1, table.getId().length());
	else if (table.getId().startsWith("t:"))
	    // gets rid of the "t:" at the beginning of a tourney-table ID
	    tableId = table.getId().substring(2, table.getId().length());
	else
	    tableId = table.getId(); // are there any other kinds of table names?

        tableInfo.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
	tableInfo.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n");
	tableInfo.append("   <head>\n");
	tableInfo.append("      <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></meta>\n");
	tableInfo.append("      <style type=\"text/css\">\n");
	tableInfo.append("         table {\n");
	tableInfo.append("            border-collapse:collapse;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         table, th, td {\n");
	tableInfo.append("            vertical-align:center;\n");
	tableInfo.append("            horizontal-align:center;\n");
	tableInfo.append("         }\n");
        tableInfo.append("         table, td {\n");
	tableInfo.append("            border:2px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         th {\n");
	tableInfo.append("            border:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td {\n");
	tableInfo.append("            padding:2px;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         tr.alternative td {\n");
	tableInfo.append("            color:#282828;\n");
	tableInfo.append("            background-color:#D4F2DB;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         th.thickexceptright {\n");
	tableInfo.append("            border-right:2px solid black;\n");
	tableInfo.append("            border-left:3px solid black;\n");
	tableInfo.append("            border-bottom:3px solid black;\n");
	tableInfo.append("            border-top:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         th.thickexceptleft {\n");
	tableInfo.append("            border-left:2px solid black;\n");
	tableInfo.append("            border-right:3px solid black;\n");
	tableInfo.append("            border-bottom:3px solid black;\n");
	tableInfo.append("            border-top:3px solid black;\n");
	tableInfo.append("         }\n");
        tableInfo.append("         th.thickbottomtop {\n");
	tableInfo.append("            border-right:2px solid black;\n");
	tableInfo.append("            border-left:2px solid black;\n");
	tableInfo.append("            border-bottom:3px solid black;\n");
	tableInfo.append("            border-top:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thickleft {\n");
	tableInfo.append("            border-left:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thickright {\n");
	tableInfo.append("            border-right:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.verythicktop {\n");
	tableInfo.append("            border-top:4px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thickbottom {\n");
	tableInfo.append("            border-bottom:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thickleftright {\n");
	tableInfo.append("            border-left:3px solid black;\n");
	tableInfo.append("            border-right:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.verythicktop {\n");
	tableInfo.append("            border-top:4px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thicktopleft {\n");
	tableInfo.append("            border-top:4px solid black;\n");
	tableInfo.append("            border-left:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thicktopright {\n");
	tableInfo.append("            border-top:4px solid black;\n");
	tableInfo.append("            border-right:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thicktopleftright {\n");
	tableInfo.append("            border-top:4px solid black;\n");
	tableInfo.append("            border-right:3px solid black;\n");
	tableInfo.append("            border-left:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thick {\n");
	tableInfo.append("            border:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thickexceptright {\n");
	tableInfo.append("            border-left:3px solid black;\n");
	tableInfo.append("            border-right:2px solid black;\n");
	tableInfo.append("            border-top:3px solid black;\n");
	tableInfo.append("            border-bottom:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thickexceptleft {\n");
	tableInfo.append("            border-left:2px solid black;\n");
	tableInfo.append("            border-right:3px solid black;\n");
	tableInfo.append("            border-top:3px solid black;\n");
	tableInfo.append("            border-bottom:3px solid black;\n");
	tableInfo.append("         }\n");
        tableInfo.append("         #rowthickbottom {\n");
	tableInfo.append("            border-bottom:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         td.thickesptop {\n");
	tableInfo.append("            border-top:4px solid black;\n");
	tableInfo.append("            border-bottom:3px solid black;\n");
	tableInfo.append("            border-right:3px solid black;\n");
	tableInfo.append("            border-left:3px solid black;\n");
	tableInfo.append("         }\n");
	tableInfo.append("         img {\n");
	tableInfo.append("            border:0px;\n");
	tableInfo.append("            height:auto;\n");
	tableInfo.append("            width:auto;\n");
	tableInfo.append("         }\n");
       	tableInfo.append("         h2 {\n");
       	tableInfo.append("	       text-align:center;\n");
       	tableInfo.append("         }\n");

	tableInfo.append("      </style>\n");

	// For tournament tables, round number will need to be added here.***
	tableInfo.append("      	<title>");
	tableInfo.append(rbs("Results_of_List_for_Table"));
	tableInfo.append(table.getSeriesId() + "</title>\n");
	tableInfo.append("      </head>\n");
	tableInfo.append("<body>\n");
	tableInfo.append("<h2>");
	tableInfo.append(rbs("Results_of_List_for_Table"));
	tableInfo.append(table.getSeriesId() + "</h2>\n");
	tableInfo.append("  	<table>\n");
      	tableInfo.append("	<tr align=\"left\">\n");

	if (numPlayers == 4)
	    tableInfo.append("			 	     <th colspan=\"20\">" + rbs("Date_Colon_Space"));
	else
	    tableInfo.append("                               <th colspan=\"17\">" + rbs("Date_Colon_Space"));

	tableInfo.append(Misc.currentUTCdate());
	tableInfo.append("</th>\n");

	tableInfo.append("				<th colspan=\"4\">");
	tableInfo.append(rbs("Round"));
	tableInfo.append(":</th>\n");
	tableInfo.append("				<th colspan=\"5\">");
	tableInfo.append(rbs("Series_HTML") + ": " + table.getSeriesId());
	tableInfo.append("</th>\n");
      	tableInfo.append("	</tr>\n");
      	tableInfo.append("	<tr align=\"center\">\n");
	tableInfo.append("				<th rowspan=\"2\"><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("game_number.jpg") + "\"\n");
	tableInfo.append("									 alt=\"#\" /></th>\n");
	tableInfo.append("				<th rowspan=\"2\">&nbsp;&nbsp;<img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("game_value.jpg") + "\"\n");
	tableInfo.append("									 alt=\"Value\" />&nbsp;&nbsp;</th>\n");
	tableInfo.append("				<th rowspan=\"2\">&nbsp;<img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("with_jacks.jpg") + "\"\n");
	tableInfo.append("									 alt=\"With\" />&nbsp;</th>\n");
	tableInfo.append("				<th rowspan=\"2\"><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("without_jacks.jpg") + "\"\n");
	tableInfo.append("									 alt=\"Without\" /></th>\n");
	tableInfo.append("				<th colspan=\"7\">" + rbs("Modifiers") + "</th>\n");
	tableInfo.append("				<th colspan=\"2\">" + rbs("Score") + "</th>\n");
	tableInfo.append("				<th colspan=\"4\" align=\"center\">" + table.getPlayerInResultTable(0) + "</th>\n");
	tableInfo.append("				<th colspan=\"4\" align=\"center\">" + table.getPlayerInResultTable(1) + "</th>\n");
	tableInfo.append("				<th colspan=\"4\" align=\"center\">" + table.getPlayerInResultTable(2) + "</th>\n");

	if (numPlayers == 4) {
	    tableInfo.append("				<th colspan=\"4\" align=\"center\">" + table.getPlayerInResultTable(3) + "</th>\n");
	}

	tableInfo.append("				<th rowspan=\"2\"><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("game_passed.jpg") + "\"\n");
	tableInfo.append("									 alt=\"Pass\" /></th>\n");
      	tableInfo.append("	</tr>\n");
      	tableInfo.append("	<tr align=\"center\">\n");
	tableInfo.append("				<th><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("hand.jpg") + "\"\n");
	tableInfo.append("									 alt=\"H\" /></th>\n");
	tableInfo.append("				<th><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("schneider.jpg") + "\"\n");
	tableInfo.append("									 alt=\"S\" /></th>\n");
	tableInfo.append("				<th><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("schneider_ann.jpg") + "\"\n");
	tableInfo.append("									 alt=\"SA\" /></th>\n");
	tableInfo.append("				<th><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("schwarz.jpg") + "\"\n");
	tableInfo.append("									 alt=\"Z\" /></th>\n");
	tableInfo.append("				<th><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("schwarz_ann.jpg") + "\"\n");
	tableInfo.append("									 alt=\"ZA\" /></th>\n");
	tableInfo.append("				<th><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("ouvert.jpg") + "\"\n");
	tableInfo.append("									 alt=\"O\" /></th>\n");
	tableInfo.append("				<th><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	tableInfo.append(rbs("overbid.jpg") + "\"\n");
	tableInfo.append("									 alt=\"V\" /></th>\n");
	tableInfo.append("				<th class=\"thickexceptright\">&nbsp;+&nbsp;</th>\n");
	tableInfo.append("				<th class=\"thickexceptleft\">&nbsp;-&nbsp;</th>\n");

	for (int index = 0; index < numPlayers; index++) {
	    tableInfo.append("				<th class=\"thickexceptright\">&nbsp;&nbsp;" + rbs("Seat") + " " + (index+1) + "&nbsp;&nbsp;</th>\n");
	    tableInfo.append("				<th class=\"thickbottomtop\"><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	    tableInfo.append(rbs("win.jpg") + "\"\n");
	    tableInfo.append("									 alt=\"W\" /></th>\n");
	    tableInfo.append("				<th class=\"thickbottomtop\"><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	    tableInfo.append(rbs("loss.jpg") + "\"\n");
	    tableInfo.append("									 alt=\"L\" /></th>\n");  
	    tableInfo.append("				<th class=\"thickexceptleft\"><img src=\"http://i914.photobucket.com/albums/ac342/ralager/skatgui2/");
	    tableInfo.append(rbs("timeouts.jpg") + "\"\n");
	    tableInfo.append("									 alt=\"T\" /></th>\n");  
	}

      	tableInfo.append("	</tr>\n");
	tableInfo.append("\n");
    }

    public void writeGameData() {
	
	for (int index = 0; index < numGames; index++) {

	    ScoreSheet.Row row = scores.getRow(index);
	    int declarer = row.declarer;

	    if (colouredBackground)
		tableInfo.append("<tr class=\"alternative\"");
	    else
		tableInfo.append("<tr");

	    if ((index + 1) % numPlayers == 0)
		tableInfo.append(" id=\"rowthickbottom\">\n");
	    else
		tableInfo.append(">\n");

	    colouredBackground = !colouredBackground;

	    tableInfo.append("   <td align=\"left\" class=\"thickleftright\">");
	    tableInfo.append((index + 1));
	    tableInfo.append("</td>\n");

	    if (declarer < 0) {
		/* All players passed, so all cells in the current row of the table
		   should be blank except for the last. */

		tableInfo.append("   <td class=\"thickleftright\"></td>\n");
		tableInfo.append("   <td class=\"thickleftright\"></td>\n");
		tableInfo.append("   <td class=\"thickleft\"></td>\n");
		tableInfo.append("   <td class=\"thickright\"></td>\n");
		tableInfo.append("   <td class=\"thickleft\"></td>\n");
		tableInfo.append("   <td></td>\n");
		tableInfo.append("   <td></td>\n");
		tableInfo.append("   <td></td>\n");
		tableInfo.append("   <td></td>\n");
		tableInfo.append("   <td class=\"thickright\"></td>\n");
		tableInfo.append("   <td class=\"thickleft\"></td>\n");
		tableInfo.append("   <td class=\"thickright\"></td>\n");

		for (int index2 = 0; index2 < numPlayers; index2++) {
		    tableInfo.append("   <td class=\"thickleft\"></td>\n");
		    tableInfo.append("   <td></td>\n");
		    tableInfo.append("   <td></td>\n");
		    tableInfo.append("   <td class=\"thickright\"></td>\n");		    
		}

		/* (can't do this now that cells have different left/right borders)
	        int numBlankCells = 12 + 4*numPlayers;

		for (int index2 = 0; index2 < numBlankCells; index2++)
		    tableInfo.append("   <td></td>\n");
		*/

		tableInfo.append("   <td align=\"center\" class=\"thickleftright\">");
		tableInfo.append(row.cumuPass);
		tableInfo.append("</td>\n");
		tableInfo.append("</tr>\n");
	    } else {
		// If not all players passed:

		// append game value
		tableInfo.append("   <td align=\"right\" class=\"thickleftright\">");
		tableInfo.append(row.baseValue);
		tableInfo.append("</td>\n");

		if (row.matadors > 0) {
		    tableInfo.append("   <td align=\"right\" class=\"thickleft\">");
		    tableInfo.append(row.matadors);
		    tableInfo.append("</td>\n");

		    tableInfo.append("   <td class=\"thickright\"></td>\n");
		} else {
		    tableInfo.append("   <td class=\"thickleft\"></td>\n");
		    
		    tableInfo.append("   <td align=\"right\" class=\"thickright\">");
		    tableInfo.append((-row.matadors));
		    tableInfo.append("</td>\n");
		}

		if (row.hand)
		    tableInfo.append("   <td align=\"center\" class=\"thickleft\">X</td>\n");
	        else
		    tableInfo.append("   <td class=\"thickleft\"></td>\n");		    

		if (row.schneider)
		    tableInfo.append("   <td align=\"center\">X</td>\n");
		else
		    tableInfo.append("   <td></td>\n");		    

		if (row.schneiderAnnounced)
		    tableInfo.append("   <td align=\"center\">X</td>\n");
		else
		    tableInfo.append("   <td></td>\n");		    

		if (row.schwarz)
		    tableInfo.append("   <td align=\"center\">X</td>\n");
		else
		    tableInfo.append("   <td></td>\n");		    

		if (row.schwarzAnnounced)
		    tableInfo.append("   <td align=\"center\">X</td>\n");
		else
		    tableInfo.append("   <td></td>\n");		    

		if (row.open)
		    tableInfo.append("   <td align=\"center\">X</td>\n");
		else
		    tableInfo.append("   <td></td>\n");		    

		if (row.overbid)
		    tableInfo.append("   <td align=\"center\" class=\"thickright\">X</td>\n");
		else
		    tableInfo.append("   <td></td>\n");

		if (row.score > 0) {
		    tableInfo.append("   <td align=\"right\" class=\"thickleft\">");
		    tableInfo.append(row.score);
		    tableInfo.append("</td>\n");

		    tableInfo.append("   <td class=\"thickright\"></td>\n");
		} else {
		    tableInfo.append("   <td class=\"thickleft\"></td>\n");
		    
		    tableInfo.append("   <td align=\"right\" class=\"thickright\">");
		    tableInfo.append((-row.score));
		    tableInfo.append("</td>\n");
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
			tableInfo.append("   <td class=\"thickleft\"></td>\n");
			tableInfo.append("   <td></td>\n");
			tableInfo.append("   <td></td>\n");
			tableInfo.append("   <td class=\"thickright\"></td>\n");
		    }

		    // print data for declarer
		    tableInfo.append("   <td align=\"right\" class=\"thickleft\">");
		    tableInfo.append(row.cumulative[declarerIndex].score);
		    tableInfo.append("</td>\n");
		    
		    if (row.score > 0) {
			tableInfo.append("   <td align=\"right\">");
			tableInfo.append(row.cumulative[declarerIndex].wins);
			tableInfo.append("</td>\n");

			tableInfo.append("   <td></td>\n");
			tableInfo.append("   <td class=\"thickright\"></td>\n");
		    } else {
			tableInfo.append("   <td></td>\n");
			
			tableInfo.append("   <td align=\"right\">");
			tableInfo.append(row.cumulative[declarerIndex].losses);
			tableInfo.append("</td>\n");	
			tableInfo.append("   <td class=\"thickright\"></td>\n");
		    }

		    numBlankPlayersAfter = numPlayers - declarerIndex - 1;
		} else {
		    if (timeoutIndex == declarerIndex) {
			// declarer timed out

			for (int index3 = 0; index3 < declarerIndex; index3++) {
			    tableInfo.append("   <td class=\"thickleft\"></td>\n");
			    tableInfo.append("   <td></td>\n");
			    tableInfo.append("   <td></td>\n");
			    tableInfo.append("   <td class=\"thickright\"></td>\n");
			}
			
			// print data for declarer
			tableInfo.append("   <td align=\"right\" class=\"thickleft\">");
			tableInfo.append(row.cumulative[declarerIndex].score);
			tableInfo.append("</td>\n");

			tableInfo.append("   <td></td>\n");
			
			tableInfo.append("   <td align=\"right\">");
			tableInfo.append(row.cumulative[declarerIndex].losses);
			tableInfo.append("</td>\n");

			tableInfo.append("   <td align=\"right\" class=\"thickright\">");
			tableInfo.append(row.cumulative[declarerIndex].penalties);
			tableInfo.append("</td>\n");

			numBlankPlayersAfter = numPlayers - declarerIndex - 1;
		    } else {
			// player other than declarer timed out

			numBlankPlayers = Math.min(timeoutIndex, declarerIndex);

			for (int index3 = 0; index3 < numBlankPlayers; index3++) {
			    tableInfo.append("   <td class=\"thickleft\"></td>\n");
			    tableInfo.append("   <td></td>\n");
			    tableInfo.append("   <td></td>\n");
			    tableInfo.append("   <td class=\"thickright\"></td>\n");
			}

			if (timeoutIndex < declarerIndex) {
			    // add 1 to player's timeouts
			    tableInfo.append("   <td class=\"thickleft\"></td>\n");
			    tableInfo.append("   <td></td>\n");
			    tableInfo.append("   <td></td>\n");
			    tableInfo.append("   <td class=\"thickright\">");
			    // ***Defender timeouts aren't saved in the ScoreSheet object?!
			    tableInfo.append(row.cumulative[timeoutIndex].penalties);
			    tableInfo.append("</td>\n");

			    int numBlankPlayers2 = declarerIndex - timeoutIndex;

			    for (int index3 = 0; index3 < numBlankPlayers; index3++) {
				tableInfo.append("   <td class=\"thickleft\"></td>\n");
				tableInfo.append("   <td></td>\n");
				tableInfo.append("   <td></td>\n");
				tableInfo.append("   <td class=\"thickright\"></td>\n");
			    }

			    // print data for declarer
			    tableInfo.append("   <td align=\"right\" class=\"thickleft\">");
			    tableInfo.append(row.cumulative[declarerIndex].score);
			    tableInfo.append("</td>\n");
			    
			    if (row.score > 0) {
				tableInfo.append("   <td align=\"right\">");
				tableInfo.append(row.cumulative[declarerIndex].wins);
				tableInfo.append("</td>\n");
				
				tableInfo.append("   <td></td>\n");
				tableInfo.append("   <td class=\"thickright\"></td>\n");
			    } else {
				tableInfo.append("   <td></td>\n");
				
				tableInfo.append("   <td align=\"right\">");
				tableInfo.append(row.cumulative[declarerIndex].losses);
				tableInfo.append("</td>\n");	
				tableInfo.append("   <td class=\"thickright\"></td>\n");
			    }

			    numBlankPlayersAfter = numPlayers - declarerIndex - 1;
			} else {
			    // print data for declarer first
			    tableInfo.append("   <td align=\"right\" class=\"thickleft\">");
			    tableInfo.append(row.cumulative[declarerIndex].score);
			    tableInfo.append("</td>\n");
			    
			    if (row.score > 0) {
				tableInfo.append("   <td align=\"right\">");
				tableInfo.append(row.cumulative[declarerIndex].wins);
				tableInfo.append("</td>\n");
				
				tableInfo.append("   <td></td>\n");
				tableInfo.append("   <td></td>\n");
			    } else {
				tableInfo.append("   <td></td>\n");
				
				tableInfo.append("   <td align=\"right\">");
				tableInfo.append(row.cumulative[declarerIndex].losses);
				tableInfo.append("</td>\n");	
				tableInfo.append("   <td class=\"thickright\"></td>\n");
			    }

			    int numBlankPlayers2 = timeoutIndex - declarerIndex;

			    for (int index3 = 0; index3 < numBlankPlayers2; index3++) {
				tableInfo.append("   <td class=\"thickleft\"></td>\n");
				tableInfo.append("   <td></td>\n");
				tableInfo.append("   <td></td>\n");
				tableInfo.append("   <td class=\"thickright\"></td>\n");
			    }

			    // then add 1 to player's timeouts
			    tableInfo.append("   <td class=\"thickleft\"></td>\n");
			    tableInfo.append("   <td></td>\n");
			    tableInfo.append("   <td></td>\n");
			    tableInfo.append("   <td class=\"thickright\">");
			    tableInfo.append(row.cumulative[timeoutIndex].penalties);
			    tableInfo.append("</td>\n");

			    numBlankPlayersAfter = numPlayers - timeoutIndex - 1;
			}	
		    }
		}

		for (int index4 = 0; index4 < numBlankPlayersAfter; index4++) {
		    tableInfo.append("   <td class=\"thickleft\"></td>\n");
		    tableInfo.append("   <td></td>\n");
		    tableInfo.append("   <td></td>\n");
		    tableInfo.append("   <td class=\"thickright\"></td>\n");
		}

		tableInfo.append("   <td class=\"thickleftright\"></td>\n");
		tableInfo.append("</tr>\n");
		tableInfo.append("\n");
	    }
	}

	// May put code below into a separate method called writeEndGameData().

	ScoreSheet.Row row = scores.getRow(numGames - 1);

	if (colouredBackground)
	    tableInfo.append("<tr class=\"alternative\">\n");
	else
	    tableInfo.append("<tr>\n");

	colouredBackground = !colouredBackground;

	tableInfo.append("   <td align=\"left\" colspan=\"13\" class=\"thickesptop\"><b>" + rbs("Totals") + "</b></td>\n");

	for (int index = 0; index < numPlayers; index++) {
	    tableInfo.append("   <td align=\"right\" class=\"thicktopleft\">");
	    tableInfo.append(row.cumulative[index].score);
	    tableInfo.append("</td>\n");
	    
	    tableInfo.append("   <td align=\"right\" class=\"verythicktop\">");
	    tableInfo.append(row.cumulative[index].wins);
	    tableInfo.append("</td>\n");
	    
	    tableInfo.append("   <td align=\"right\" class=\"verythicktop\">");
	    tableInfo.append(row.cumulative[index].losses);
	    tableInfo.append("</td>\n");

	    tableInfo.append("   <td align=\"right\" class=\"thicktopright\">");
	    tableInfo.append(row.cumulative[index].penalties);
	    tableInfo.append("</td>\n");
	}

	tableInfo.append("   <td align=\"right\" class=\"thicktopleftright\">");
	tableInfo.append(row.cumuPass);
	tableInfo.append("</td>\n");
	tableInfo.append("</tr>\n");

    	if (colouredBackground)
	    tableInfo.append("<tr class=\"alternative\">\n");
	else
	    tableInfo.append("<tr>\n");

	colouredBackground = !colouredBackground;

	tableInfo.append("   <td align=\"left\" colspan=\"13\" class=\"thick\"><b>" + rbs("Won_Less_Lost_by_50_HTML") + "</b></td>\n");

	ArrayList<Integer> winPoints = new ArrayList<Integer>();
	
	for (int index = 0; index < numPlayers; index++) {
	    int winPts = (row.cumulative[index].wins - row.cumulative[index].losses) * 50;
	    
	    tableInfo.append("   <td align=\"right\" class=\"thickleft\">");
	    tableInfo.append(winPts);
	    tableInfo.append("</td>\n");
	    tableInfo.append("   <td colspan=\"3\" class=\"thickright\"></td>\n");
	    
	    winPoints.add(winPts);
	}

	tableInfo.append("   <td class=\"thick\"></td>\n");
	tableInfo.append("</tr>\n");
	
      	if (colouredBackground)
	    tableInfo.append("<tr class=\"alternative\">\n");
	else
	    tableInfo.append("<tr>\n");

	colouredBackground = !colouredBackground;
	
	tableInfo.append("   <td align=\"left\" colspan=\"13\" class=\"thick\"><b>" + rbs("Opponent_Losses"));
	
	if (numPlayers == 3)
	    tableInfo.append("40");
	else
	    tableInfo.append("30");
	
	tableInfo.append("</b></td>\n");
	
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
	    
	    tableInfo.append("   <td align=\"right\" class=\"thickleft\">");
	    tableInfo.append(lossPts);
	    tableInfo.append("</td>\n");
	    tableInfo.append("   <td colspan=\"3\" class=\"thickright\"></td>\n");
	    
	    lossPoints.add(lossPts);
	}

	tableInfo.append("   <td class=\"thick\" rowspan=\"3\"></td>\n");    
	tableInfo.append("</tr>\n");

	if (colouredBackground)
	    tableInfo.append("<tr class=\"alternative\">\n");
	else
	    tableInfo.append("<tr>\n");

	colouredBackground = !colouredBackground;     

	tableInfo.append("   <td align=\"left\" colspan=\"13\" class=\"thick\"><b>" + rbs("TimeoutPts_HTML") + "</b></td>\n");
	
	for (int index = 0; index < numPlayers; index++) {
	    tableInfo.append("   <td align=\"right\" class=\"thickleft\"><b>");
	    int timeoutPts = -row.cumulative[index].penalties*Table.PENALTY_PTS;
	    tableInfo.append(timeoutPts);
	    tableInfo.append("</b></td>\n");	
	    tableInfo.append("   <td colspan=\"3\" class=\"thickright\"></td>\n");
	}

	tableInfo.append("</tr>\n");
	
	if (colouredBackground)
	    tableInfo.append("<tr class=\"alternative\">\n");
	else
	    tableInfo.append("<tr>\n");

	colouredBackground = !colouredBackground;     

	tableInfo.append("   <td align=\"left\" colspan=\"13\" class=\"thick\"><b>" + rbs("End_Result_HTML") + "</b></td>\n");
	
	for (int index = 0; index < numPlayers; index++) {
	    int totalPts = row.cumulative[index].score + winPoints.get(index) + lossPoints.get(index)
		- (row.cumulative[index].penalties*Table.PENALTY_PTS);
	    
	    tableInfo.append("   <td align=\"right\" class=\"thickexceptright\"><b>");
	    tableInfo.append(totalPts);
	    tableInfo.append("</b></td>\n");	
	    tableInfo.append("   <td align=\"center\" class=\"thickexceptleft\" colspan=\"3\">");
	    tableInfo.append(row.cumulative[index].losses);
	    tableInfo.append("</td>\n");
	}

	tableInfo.append("</tr></table></body></html>");
    }
}