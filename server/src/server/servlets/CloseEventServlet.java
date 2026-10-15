package server.servlets;

import dto.ActionResultDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;

import java.io.IOException;

/** POST /close-event (event, winner) - the market maker closes the event and decides the winning option. */
@WebServlet(name = "CloseEventServlet", urlPatterns = "/close-event")
public class CloseEventServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> new ActionResultDTO(engine.closeEvent(
                ServletUtils.getRequiredParameter(request, "event"), userName,
                ServletUtils.getRequiredParameter(request, "winner"))));
    }
}
