package common;
// A simple class for holding deals of cards to the other players


public class HandDeal {

  public int p2hand;
  public int p3hand;
  public int skat;

  public HandDeal(int p2, int p3, int s) {
    p2hand = p2;
    p3hand = p3;
    skat = s;
  }

  public String toString() {
    String answer = "";
    answer += Hand.toString(p2hand, true);
    answer += "\n" + Hand.toString(p3hand, true);
    answer += "\n" + Hand.toString(skat, true);
    return answer;
  }

}