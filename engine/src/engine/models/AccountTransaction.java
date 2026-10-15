package engine.models;

import java.time.LocalTime;

/** A single change in a user's balance. */
public record AccountTransaction(
        int number,
        LocalTime time,
        String type,
        String description,
        double amount,
        double balanceBefore,
        double balanceAfter
) {}
