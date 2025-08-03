/*
 * Creates a tournament-style result table in the Skat GUI.  Extended by ThreeResultTable
 * and FourResultTable, which are specific to 3- and 4-player tables, respectively.
 *
 * (c) Ryan Lagerquist
 */

package gui;

import common.*;
import java.awt.FontMetrics;

public abstract class TournamentResultTable extends javax.swing.JTable
{
    public abstract void addGameData(GameResult gameResult_, GameDeclaration gameDeclaration_);
    public abstract void resize(float f, FontMetrics fontMetrics);
}
