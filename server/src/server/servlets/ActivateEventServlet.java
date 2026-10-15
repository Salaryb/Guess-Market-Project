package server.servlets;

import dto.ActionResultDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;

import java.io.IOException;

/** POST /activate-event (event) - the market maker starts the event. */
@WebServlet(name = "ActivateEventServlet", urlPatterns = "/activate-event")
public class ActivateEventServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> new ActionResultDTO(
                engine.activateEvent(ServletUtils.getRequiredParameter(request, "event"), userName)));
    }
}
