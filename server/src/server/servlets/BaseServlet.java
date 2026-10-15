package server.servlets;

import engine.IGuessMarketEngine;
import exception.GuessMarketException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

import java.io.IOException;

/**
 * Base of all Guess Market servlets.
 * - Verifies the caller is logged in (401 otherwise).
 * - Runs the engine work while holding the engine's lock: many server threads serve many clients
 *   at the same time, and the engine itself is not thread safe ("high" synchronization at the servlet level).
 * - Turns every validation error into a 400 response with a readable JSON message.
 */
public abstract class BaseServlet extends HttpServlet {

    @FunctionalInterface
    protected interface EngineAction {
        /** @return the object to send back to the client as JSON */
        Object execute(IGuessMarketEngine engine, String userName);
    }

    protected void respond(HttpServletRequest request, HttpServletResponse response, EngineAction action)
            throws IOException {
        String userName = SessionUtils.getUsername(request);
        if (userName == null) {
            ServletUtils.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "You are not logged in.");
            return;
        }
        respondAs(userName, response, action);
    }

    protected void respondAs(String userName, HttpServletResponse response, EngineAction action) throws IOException {
        IGuessMarketEngine engine = ServletUtils.getEngine(getServletContext());
        try {
            Object result;
            synchronized (engine) {
                result = action.execute(engine, userName);
            }
            ServletUtils.writeJson(response, HttpServletResponse.SC_OK, result);
        } catch (GuessMarketException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log("Unexpected error", e);
            ServletUtils.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected server error: " + e.getMessage());
        }
    }
}
