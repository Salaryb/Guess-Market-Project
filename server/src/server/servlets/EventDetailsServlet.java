package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;

import java.io.IOException;

/** GET /event-details?event=NAME - everything about a single event (order books / LMSR state, participants). */
@WebServlet(name = "EventDetailsServlet", urlPatterns = "/event-details")
public class EventDetailsServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) ->
                engine.getEventDetails(ServletUtils.getRequiredParameter(request, "event")));
    }
}
