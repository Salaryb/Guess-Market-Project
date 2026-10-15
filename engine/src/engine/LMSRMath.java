package engine;

import java.util.List;

/** The two LMSR equations (see appendix A), generalized to any number of options. */
public final class LMSRMath {

    private LMSRMath() {
    }

    /** C(q1..qn) = b * ln( sum of e^(qi/b) ) */
    public static double calculateCost(List<Integer> quantities, int b) {
        double sum = 0.0;
        for (int q : quantities) {
            sum += Math.exp((double) q / b);
        }
        return b * Math.log(sum);
    }

    /** price(i) = e^(qi/b) / sum of e^(qj/b) */
    public static double calculatePrice(List<Integer> quantities, int index, int b) {
        double sum = 0.0;
        for (int q : quantities) {
            sum += Math.exp((double) q / b);
        }
        return Math.exp((double) quantities.get(index) / b) / sum;
    }
}
