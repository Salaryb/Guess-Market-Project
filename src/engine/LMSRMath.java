package engine;

public class LMSRMath {
    public static double calculateCost(int q1, int q2, int b) {
        return b * Math.log(Math.exp((double) q1 / b) + Math.exp((double) q2 / b));
    }

    public static double calculateProbability(int qTarget, int qOther, int b) {
        double expTarget = Math.exp((double) qTarget / b);
        double expOther = Math.exp((double) qOther / b);
        return expTarget / (expTarget + expOther);
    }
}