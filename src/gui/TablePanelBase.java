package gui;

import common.*;
import javax.swing.*;

public abstract class TablePanelBase extends javax.swing.JPanel
{

  public abstract void send(String msg);
  public abstract void resize(float f);
  public abstract Table getTable();
  public abstract void game2Window(ClientWindow.TablePanelUpdateAction action, String[] params);
  public abstract void setTableFrame(JFrame frame);
}