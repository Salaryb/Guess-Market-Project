package engine.models;

/** The three stages of an event's life. */
public enum EventStatus {
    NOT_STARTED("Not Started"),
    ACTIVE("Active"),
    ENDED("Ended");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
