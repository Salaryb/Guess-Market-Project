package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /users - public info of all users: name, balance, whether he is a market maker. */
@WebServlet(name = "UsersServlet", urlPatterns = "/users")
public class UsersServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> engine.getAllUsers());
    }
}
