package server.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class SessionUtils {

    private SessionUtils() {
    }

    /** The name of the user logged in with this session, or null. */
    public static String getUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object name = session != null ? session.getAttribute(Constants.USERNAME_ATTRIBUTE) : null;
        return name != null ? name.toString() : null;
    }
}
