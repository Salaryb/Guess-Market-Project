package server.servlets;

import dto.ActionResultDTO;
import exception.GuessMarketException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;

import java.io.IOException;

/** POST /place-order (event, option, side=buy|sell, shares, price) - submits an order to an order book. */
@WebServlet(name = "PlaceOrderServlet", urlPatterns = "/place-order")
public class PlaceOrderServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> new ActionResultDTO(engine.placeOrder(
                ServletUtils.getRequiredParameter(request, "event"), userName,
                ServletUtils.getRequiredParameter(request, "option"),
                isBuy(ServletUtils.getRequiredParameter(request, "side")),
                ServletUtils.getIntParameter(request, "shares"),
                ServletUtils.getDoubleParameter(request, "price"))));
    }

    private static boolean isBuy(String side) {
        if (side.equalsIgnoreCase("buy")) {
            return true;
        }
        if (side.equalsIgnoreCase("sell")) {
            return false;
        }
        throw new GuessMarketException("'side' must be 'buy' or 'sell' (got '" + side + "').");
    }
}
