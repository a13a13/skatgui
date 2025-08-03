package common;

import java.io.*;

public class DeepCloner
{
  static public Object deepCopy(Object obj2DeepCopy)
  {
    //obj2DeepCopy must be serializable
    ObjectOutputStream outStream = null;
    ObjectInputStream inStream = null;
    try
      {
       ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
       outStream = new ObjectOutputStream(byteOut);
       // serialize and write obj2DeepCopy to byteOut
         
       outStream.writeObject(obj2DeepCopy);
                        //always flush your stream
         
       outStream.flush();

       ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
       inStream = new ObjectInputStream(byteIn);

       // read the serialized, and deep copied, object and return it
       return inStream.readObject();
       
    } catch(Exception e) {
      //handle the exception
      //it is not a bad idea to throw the exception, so that the caller of the
      //method knows something went wrong
      System.out.println("Exception!! " + e.toString());
      throw new RuntimeException(e);
    }
    finally
    {
      //always close your streams in finally clauses
      try
      {
        outStream.close();
        inStream.close();
      }
      catch(Exception e)
      {
        throw new RuntimeException(e);
      }
    }
  }
}
