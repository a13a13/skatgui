/* $Id: Options.java 4878 2006-08-29 09:10:42Z orts_mburo $
   
   (c) Michael Buro, licensed under GPLv2

   Options class

   Allows to map strings to values of different types and
   to set overwrite default values by command line arguments

   The latest addition is indirection which is used for
   default parameters

   example:

   // definition:
   
   options.put("-port", new Integer(5000), "program listens to this port");

   // reading args:

   options.putLine(arg_string, true);

   // using option:

   Integer port = options.getInteger("port");


   // Indirection

   options.put("def.foo", new Integer(5), "blah"); // default
   
   options.puti("os.foo", "def.foo", "blah"); // indirection

   if os.foo is not set in putLine, get("os.foo") refers
   to value of def.foo. Otherwise, a new value with type
   of def.foo is created and used for os.foo.
   
*/

package common;

import java.util.*;

public class Options
{
  static final int STR    = 1;
  static final int INT    = 2;
  static final int LONG   = 3;  
  static final int GEO    = 4;
  static final int SWITCHON = 5;  
  static final int BOOL   = 6;
  static final int DOUBLE = 7;
  static final int INDIR  = 8; // indirection to other option

  private Hashtable<String, Opt> ht = new Hashtable<String, Opt>();

  private abstract class Opt
  {
    boolean orig = true;
    int type = 0;
    String description;
    abstract void fromString(String s) throws IllegalArgumentException;
    abstract Opt copy();
    boolean hasParam() { return true; }
  }
  
  private class OptSwitchOn extends Opt
  {
    Boolean value = false;

    OptSwitchOn(Boolean v, String descr) { value = v; type = SWITCHON; description = descr; }

    void fromString(String s)
    {
      value = true;
    }

    public OptSwitchOn copy() { return new OptSwitchOn(value, description); }

    public String toString() {
      if (value) return "SWITCHON:1";
      else       return "SWITCHON:0";
    }

    boolean hasParam() { return false; }    
  }

  private class OptBoolean extends Opt
  {
    Boolean value;

    OptBoolean(Boolean v, String descr) { value = v; type = BOOL; description = descr;}

    void fromString(String s)
    {
      if (s.equals("0")) 
	value = Boolean.FALSE;
      else if (s.equals("1"))
	value = Boolean.TRUE;
      else
        throw new IllegalArgumentException("0/1 expected");
    }

    public OptBoolean copy() { return new OptBoolean(value, description); }

    public String toString() {
      if (value) return "BOOL:1";
      else       return "BOOL:0";
    }
    
  }

  private class OptInt extends Opt
  {
    Integer value;

    OptInt(Integer v, String descr) { value = v; type = INT; description = descr; }
    
    void fromString(String s) { value = Integer.decode(s); }

    public OptInt copy() { return new OptInt(value, description); }

    public String toString() { return "INT:" + value.toString(); }
  }

  private class OptLong extends Opt
  {
    Long value;

    OptLong(Long v, String descr) { value = v; type = LONG; description = descr; }
    
    void fromString(String s) { value = Long.decode(s); }

    public OptLong copy() { return new OptLong(value, description); }

    public String toString() { return "LONG:" + value.toString(); }
  }

  private class OptStr extends Opt
  {
    String value;

    OptStr(String v, String descr) { value = v; type = STR; description = descr; }

    void fromString(String s) { value = s; }

    public OptStr copy() { return new OptStr(value, description); }

    public String toString() { return "STR:" + value; }    
  }

  private class OptDouble extends Opt
  {
    Double value;

    OptDouble(Double v, String descr) { value = v; type = DOUBLE; description = descr; }

    void fromString(String s) { value = Double.valueOf(s); }

    public OptDouble copy() { return new OptDouble(value, description); }

    public String toString() { return "DBL:" + value.toString(); }
  }

  private class OptGeo extends Opt
  {
    Geometry value;

    OptGeo(Geometry v, String descr) { value = v; type = GEO; description = descr; }

    void fromString(String s) { value = Geometry.valueOf(s); }

    public OptGeo copy() { return new OptGeo(value, description); }

    public String toString() { return "GEOM:" + value.toString(); }
  }

  /** indirection */
  private class OptInd extends Opt
  {
    String value;

    OptInd(String v, String descr) { value = v; type = INDIR; description = descr; }

    void fromString(String s) { value = s; }

    public OptInd copy() { return new OptInd(value, description); }

    public String toString() { return "IND:" + value; }
  }


  public Options() { }

  /** check whether key is already present */
  private void check(String key)
  {
    if (ht.get(key) != null)
      Misc.err("options: multiple definition of " + key);
  }

  // ------------------ int
  public void put(String key, Integer v, String descr)
  {
    check(key);
    put2(key, v, descr);
  }
  
  // overwrite  
  public void put2(String key, Integer v, String descr)
  {
    ht.put(key, new OptInt(v, descr));
  }
      
  // ------------------ long
  public void put(String key, Long v, String descr)
  {
    check(key);
    put2(key, v, descr);
  }
  
  // overwrite  
  public void put2(String key, Long v, String descr)
  {
    ht.put(key, new OptLong(v, descr));
  }
      
  // ------------------ double
  public void put(String key, Double v, String descr)
  {
    check(key);
    put2(key, v, descr);
  }
  
  // overwrite  
  public void put2(String key, Double v, String descr)
  {
    ht.put(key, new OptDouble(v, descr));
  }

  // ------------------ boolean 
  public void put(String key, Boolean v, String descr)
  {
    check(key);
    put2(key, v, descr);
  }

  // overwrite  
  public void put2(String key, Boolean v, String descr)
  {
    ht.put(key, new OptBoolean(v, descr));
  }

  // ------------------ boolean on
  public void put(String key, String descr)
  {
    check(key);
    put2(key, descr);
  }

  // overwrite  
  public void put2(String key, String descr)
  {
    ht.put(key, new OptSwitchOn(false, descr));
  }

  // ------------------ string
  public void put(String key, String v, String descr)
  {
    check(key);
    put2(key, v, descr);
  }

  // overwrite  
  public void put2(String key, String v, String descr)
  {
    ht.put(key, new OptStr(v, descr));
  }

  // ------------------ geometry
  public void put(String key, Geometry v, String descr)
  {
    check(key);
    put2(key, v, descr);
  }

  // overwrite  
  public void put2(String key, Geometry v, String descr)
  {
    ht.put(key, new OptGeo(v, descr));
  }

  // ------------------ redirection
  public void puti(String key, String v, String descr)
  {
    check(key);
    put2i(key, v, descr);
  }

  // overwrite  
  public void put2i(String key, String v, String descr)
  {
    ht.put(key, new OptInd(v, descr));
  }

  // ------------------  

  // get value (possibly after one step of indirection)
  private Opt getv(String key)
  {
    Opt o = ht.get(key);

    if (o == null)
      return null;
    
    if (o instanceof OptInd) {
      // Global.dbgmsg("indirection " + key + " -> " + ((OptInd)o).value);
      return ht.get(((OptInd)o).value); // indirection
    }
    return o;
  }
  
  public Boolean getSwitchOn(String key)
  {
    OptSwitchOn opt = (OptSwitchOn)getv(key);
    if (opt == null || opt.type != SWITCHON) return null;
    return opt.value;
  }

  public Boolean getBoolean(String key)
  {
    OptBoolean opt = (OptBoolean)getv(key);
    if (opt == null || opt.type != BOOL) return null;
    return opt.value;
  }

  public Integer getInteger(String key)
  {
    OptInt opt = (OptInt)getv(key);
    if (opt == null || opt.type != INT) return null;
    return opt.value;
  }

  public Long getLong(String key)
  {
    OptLong opt = (OptLong)getv(key);
    if (opt == null || opt.type != LONG) return null;
    return opt.value;
  }

  public Double getDouble(String key)
  {
    OptDouble opt = (OptDouble)getv(key);
    if (opt == null || opt.type != DOUBLE)
      return null;
    return opt.value;
  }

  public String getString(String key)
  {
    OptStr opt = (OptStr)getv(key);
    if (opt == null || opt.type != STR) return null;
    return opt.value;
  }

  public Geometry getGeometry(String key)
  {
    OptGeo opt = (OptGeo)getv(key);
    if (opt == null || opt.type != GEO) return null;
    return opt.value;
  }

  public boolean parse(String[] args)
  {
    return parse(args, 0);
  }

  /** parse options from argument strings
   *
   * @return true iff options ok
   */
  public boolean parse(String[] args, int start)
  {
    // parse option line
    // % separates options, observe \%
    // _ -> space, observe \_
    // .w  -> w is prefix for following options

    // Global.dbgmsg("opt: "+line0);

    int i;
    for (i=start; i < args.length; i++) {

      String key = args[i];
      
      Opt hopt = ht.get(key);
      
      if (hopt == null) {
        
        // option not recognized
        
        System.err.println("WARNING: option " + key + " not recognized");
        break;
      }

      if (hopt instanceof OptInd) {

        // indirection: create copy of pointed to variable to be overwritten

        OptInd o = (OptInd)hopt;
        hopt = ht.get(o.value);

        if (hopt == null) {
          System.err.println("WARNING: indirection not found "+key+" -> "+o.value);
          break;
        }

        hopt = hopt.copy();
        ht.put(key, hopt);
      }

      String param = "1";
      
      if (hopt.hasParam()) {
        if (i >= args.length-1) break; // not enough parameters
        param = args[++i];
      }
      
      System.err.println("set opt \""+key+"\" to \""+param+"\"");

      try { hopt.fromString(param); }
      catch (NumberFormatException e) {
        Misc.msg(e + ": " + param);
        break;
      }
      catch (IllegalArgumentException e) {
        Misc.msg(e + ": " + param);
        break;
      }
    }

    if (i < args.length) {
      System.err.println("Usage:");
      write();
      return false;
    }

    return true;
  }
  

  public void write()
  {
    Enumeration en = ht.keys();
    ArrayList<String> l = new ArrayList<String>();
    while (en.hasMoreElements()) {
      l.add((String) en.nextElement());
    }

    Collections.sort(l);

    for (String key : l) 
    {
      System.err.println(String.format(Locale.US, "%-8s %-12s %s", key, ht.get(key).toString(), ht.get(key).description));
    }
  }

  public static void test(String[] args)
  {
    // test options
    Options opt = new Options();
    
    opt.put("-i", new Integer(5000),  "int");
    opt.put("-o",                     "bool on");
    opt.put("-b", new Boolean(false), "bool");
    opt.put("-s", new String("blah"), "string");
    opt.put("-d", new Double("0.9"),  "double");        
    
    if (!opt.parse(args)) {
      Misc.err("option error");
    }
    
    Integer i = opt.getInteger("-i");
    Boolean b = opt.getBoolean("-b");
    Boolean o = opt.getSwitchOn("-o");
    Double  d = opt.getDouble("-d");
    String  s = opt.getString("-s");
    
    Misc.msg("i=" + i + " b=" + b + " o=" + o + " d=" + d + " s=" + s);
  }

}
