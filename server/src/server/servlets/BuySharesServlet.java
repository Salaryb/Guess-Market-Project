package server.servlets;

import dto.ActionResultDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;

import java.io.IOException;

/** POST /buy-shares (event, option, shares) - buys shares in an LMSR event. */
@WebServlet(name = "BuySharesServlet", urlPatterns = "/buy-shares")
public class BuySharesServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> new ActionResultDTO(engine.buyLmsrShares(
                ServletUtils.getRequiredParameter(request, "event"), userName,
                ServletUtils.getRequiredParameter(request, "option"),
                ServletUtils.getIntParameter(request, "shares"))));
    }
}
