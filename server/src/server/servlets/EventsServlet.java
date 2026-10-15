package server.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /events - all the events in the system (any type, any status). */
@WebServlet(name = "EventsServlet", urlPatterns = "/events")
public class EventsServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> engine.getAllEvents());
    }
}
