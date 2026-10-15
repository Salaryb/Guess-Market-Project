package server.utils;

import engine.IGuessMarketEngine;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/** When a session ends (logout or timeout) the user is marked as logged out, so his name can log in again. */
@WebListener
public class SessionListener implements HttpSessionListener {

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        Object userName = event.getSession().getAttribute(Constants.USERNAME_ATTRIBUTE);
        if (userName != null) {
            IGuessMarketEngine engine = ServletUtils.getEngine(event.getSession().getServletContext());
            synchronized (engine) {
                engine.logoutUser(userName.toString());
            }
        }
    }
}
