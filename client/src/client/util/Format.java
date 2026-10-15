package client.util;

/** Number formatting: every decimal number is shown with 2 digits after the point. */
public final class Format {

    private Format() {
    }

    public static String money(double value) {
        return String.format("%.2f", value);
    }

    /** For optional statistics (null = not available). */
    public static String moneyOrNA(Double value) {
        return value == null ? "N/A" : money(value);
    }
}
