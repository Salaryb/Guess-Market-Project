package engine.models;

/**
 * The result of matching an incoming order against a resting one.
 * TRADE: shares move from seller to buyer at buyerPrice.
 * MINT: new shares are created - the buyer gets 'option' at buyerPrice and the
 * other buyer gets 'otherOption' at otherPrice (buyerPrice + otherPrice == d).
 */
public record Fill(
        FillType type,
        String buyer,
        String option,
        double buyerPrice,
        String counterparty,
        String otherOption,
        double otherPrice,
        int shares
) {
    public enum FillType { TRADE, MINT }

    public static Fill trade(String buyer, String seller, String option, int shares, double price) {
        return new Fill(FillType.TRADE, buyer, option, price, seller, null, price, shares);
    }

    public static Fill mint(String buyer, String option, double buyerPrice,
                            String otherBuyer, String otherOption, double otherPrice, int shares) {
        return new Fill(FillType.MINT, buyer, option, buyerPrice, otherBuyer, otherOption, otherPrice, shares);
    }
}
