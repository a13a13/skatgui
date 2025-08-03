/**
   sampleClient.java

   A simple skeleton for a Java Skat client.

   invoke like so:
   
   java -jar dist/sampleClient.jar "$@"

   author: Michael Buro
   licensed under GPLv3
*/

package client;

public class sampleClient extends AIClient
{

  public Player newPlayer()
  {
    return new SamplePlayer();
  }

  public sampleClient(String[] args)
  {
    init(args);
  }

  /**
   * @param args
   *            the command line arguments
   */
  public static void main(String[] args)
  {
    sampleClient c = new sampleClient(args);
  }
}
