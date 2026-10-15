package server.servlets;

import dto.ActionResultDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** POST /logout - ends the session (the user's account and data stay in the system). */
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> {
            engine.logoutUser(userName);
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            return new ActionResultDTO("Goodbye, " + userName + ".");
        });
    }
}
