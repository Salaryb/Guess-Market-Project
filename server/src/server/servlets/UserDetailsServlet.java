package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /user-details - the logged-in user's private details: balance, events, account transactions. */
@WebServlet(name = "UserDetailsServlet", urlPatterns = "/user-details")
public class UserDetailsServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> engine.getUserDetails(userName));
    }
}
