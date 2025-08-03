// maintain global user interface settings (currently just font sizes)
// to be able to resize frames

// (c) Michael Buro

package common;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import javax.swing.plaf.FontUIResource;
import net.miginfocom.layout.PlatformDefaults;
import common.*;

public class GlobalUI
{
  public GlobalUI()
  {
    FontUIResource fb = new FontUIResource("Dialog", 1, 14); 
    FontUIResource fs = new FontUIResource("Dialog", 1, 12);
    //    FontUIResource fss = new FontUIResource("Dialog", 1, 11);
    //    FontUIResource fm = new FontUIResource("Lucida Sans Typewriter", 1, 12);
    FontUIResource fm = new FontUIResource("Monospaced", 1, 12);    

    // set fonts to dialog bold,14pt and some to bold,12pt

    @SuppressWarnings("unchecked")    
    Set entries = new HashSet(UIManager.getLookAndFeelDefaults().keySet());

    for (Iterator it = entries.iterator(); it.hasNext();) {
      String key = it.next().toString();
      Object value = UIManager.get(key);
      
      if (value instanceof Font) {

        //Misc.msg("key <" + key + ">");
        
        Font f = fb;
        if (key.equals("Table.font") || key.equals("TableHeader.font") ||
            key.equals("MenuBar.font") || key.equals("Menu.font") ||
            key.equals("TitledBorder.font")) {

          f = fs;
          
        } else if (key.equals("TextArea.font")) {

          f = fm;
        }
        
        UIManager.put(key, f);
        origDefaults.put(key, f);
      }
    }
  }

  // register frame
  public void add(ResizeFrame frame)
  {
    frames.add(frame);
  }

  /** @return true iff frame has been removed */
  public boolean remove(JFrame frame)
  {
    int l = frames.size();
    for (int i=0; i < l; i++) {
      if (frames.get(i) == frame) {
        frames.remove(i);
        return true;
      }
    }
    return false;
  }
  
  public void resize(float f)
  {
    PlatformDefaults.setHorizontalScaleFactor(.1f);// Only so that the cache will be invalidated for sure
    PlatformDefaults.setHorizontalScaleFactor(f);
    PlatformDefaults.setVerticalScaleFactor(f);

    Set entries = origDefaults.entrySet();

    for (@SuppressWarnings("unchecked")    
           Iterator<Map.Entry<String, Font>> it = entries.iterator(); it.hasNext();) {
      Map.Entry<String, Font> e = it.next();
      Font origFont = e.getValue();

      //  Misc.msg("i= " + e.getKey());
      UIManager.put(e.getKey(), new FontUIResource(origFont.deriveFont(origFont.getSize() * f)));
    }

    for (ResizeFrame frame : frames) {
      // Misc.msg("RESIZE : " + frame);
      SwingUtilities.updateComponentTreeUI(frame);
      frame.resize(f);
      frame.validate();
    }
  }

  static HashMap<String, Font> origDefaults = new HashMap<String, Font>();
  ArrayList<ResizeFrame> frames = new ArrayList<ResizeFrame>();
}
