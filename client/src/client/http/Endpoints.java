package client.http;

/** The server's endpoints (relative to ServerApi.BASE_URL). */
public final class Endpoints {
    public static final String LOGIN = "/login";
    public static final String LOGOUT = "/logout";
    public static final String VERSION = "/version";
    public static final String EVENTS = "/events";
    public static final String EVENT_DETAILS = "/event-details";
    public static final String USERS = "/users";
    public static final String USER_DETAILS = "/user-details";
    public static final String USER_EVENT_DETAILS = "/user-event-details";
    public static final String UPLOAD_FILE = "/upload-file";
    public static final String DEPOSIT = "/deposit";
    public static final String ACTIVATE_EVENT = "/activate-event";
    public static final String CLOSE_EVENT = "/close-event";
    public static final String BUY_SHARES = "/buy-shares";
    public static final String PLACE_ORDER = "/place-order";
    public static final String CHAT = "/chat";

    private Endpoints() {
    }
}
