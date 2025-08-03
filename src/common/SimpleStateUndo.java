// stores information to undo card moves

package common;

public class SimpleStateUndo {

  public int toMoveHand;
  public int toMovePlayedCards;
  public byte prevToMove;
  public byte toMoveVoids;
  public byte trickCardNum;
  public byte trickWinner;
  public byte tricksWon0, tricksWon1, tricksWon2;
  public byte trickPoints0, trickPoints1, trickPoints2;
  public Card card;
  public Card winningCard;
  public Card trick0, trick1, trick2;
}
