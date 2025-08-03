/**
   jniClient.java

   A simple skeleton implementation of how to hook up a C++ AI client to the skat server.

   invoke like so:
   
   java -Djava.library.path=./c++ -jar dist/jniClient.jar "$@"

   the library name can be passed by using -lib name (see AIClient.java)
   (default is skattaplayer)
   
   authors: Jeff Long, Michael Buro
   licensed under GPLv3
*/

package client;

public class jniClient extends AIClient
{

  public Player newPlayer()
  {
    // Misc.msg("FOO: " + library + "," + playerType + "," + params);
    
    return new JNIPlayer(library, playerType, params);
  }

  public jniClient(String[] args)
  {
    init(args);
  }

  /**
   * @param args
   *            the command line arguments
   */
  public static void main(String[] args)
  {
    jniClient c = new jniClient(args);
  }
}
