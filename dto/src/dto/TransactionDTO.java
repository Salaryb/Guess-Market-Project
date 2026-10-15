package dto;

/** A single line in a user's account (every change of the balance creates one). */
public record TransactionDTO(
        int number,
        String time,
        String type,
        String description,
        double amount,
        double balanceBefore,
        double balanceAfter
) {}
