package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;

import java.io.IOException;

/** GET /user-event-details?event=NAME - the logged-in user's involvement in a specific event. */
@WebServlet(name = "UserEventDetailsServlet", urlPatterns = "/user-event-details")
public class UserEventDetailsServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) ->
                engine.getUserEventDetails(userName, ServletUtils.getRequiredParameter(request, "event")));
    }
}
