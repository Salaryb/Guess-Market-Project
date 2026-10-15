package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /version - the data version number. Clients pull it often and fetch the data only when it changed. */
@WebServlet(name = "VersionServlet", urlPatterns = "/version")
public class VersionServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> engine.getVersion());
    }
}
