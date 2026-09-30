package Dice;

import java.util.LinkedHashSet;
import java.util.Set;

public class Dice {
    
    private int value;

    public Dice() {
      this.value = 1;
    }

    public int roll() {
        this.value = (int) (Math.random()*6 + 1); // Generates a number between 1 and 6
        return this.value;
    }

    public int getValue() {
        return value;
    }

    public static int[] pairSums(Dice d1, Dice d2, Dice d3, Dice d4) {
        Dice[] dice = {d1, d2, d3, d4};
        Set<Integer> unique = new LinkedHashSet<>();
        for (int i = 0; i < dice.length; i++) {
            for (int j = i + 1; j < dice.length; j++) {
                unique.add(dice[i].getValue() + dice[j].getValue());
            }
        }
        int[] sums = new int[unique.size()];
        int idx = 0;
        for (Integer s : unique) sums[idx++] = s;
        return sums;
    }

    // roll all provided dice
    public static void rollAll(Dice... dice) {
        for (Dice d : dice) d.roll();
    }

    // return values array for provided dice
    public static int[] values(Dice... dice) {
        int[] vals = new int[dice.length];
        for (int i = 0; i < dice.length; i++) vals[i] = dice[i].getValue();
        return vals;
    }

    public static String valuesString(Dice... dice) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dice.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(dice[i].getValue());
        }
        return sb.toString();
    }

    public static int sumTwo(Dice d1, Dice d2) {
        return d1.getValue() + d2.getValue();
    }

    public static int sumAll(Dice... dice){
        int sum=0;
        for (Dice d:dice){
            sum+=d.getValue();
        }
        return sum;
    }

    public static int RemainingSum(Dice d1, Dice d2, Dice d3, Dice d4, int selectedSum){
        return sumAll(d1,d2,d3,d4)-selectedSum;
    }
}

