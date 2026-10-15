package server.servlets;

import dto.ActionResultDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.Constants;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

import java.io.IOException;

/** POST /login  (username) - logs in with a unique user name. No passwords, by design. */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String current = SessionUtils.getUsername(request);
        if (current != null) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "This client is already logged in as '" + current + "'.");
            return;
        }
        String requestedName = request.getParameter("username");
        respondAs(requestedName, response, (engine, userName) -> {
            String registeredName = engine.loginUser(userName);
            request.getSession(true).setAttribute(Constants.USERNAME_ATTRIBUTE, registeredName);
            // The message is exactly the registered name, so the client shows it with the right casing
            return new ActionResultDTO(registeredName);
        });
    }
}
