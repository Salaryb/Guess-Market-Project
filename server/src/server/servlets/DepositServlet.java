package server.servlets;

import dto.ActionResultDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.utils.ServletUtils;

import java.io.IOException;

/** POST /deposit (amount) - loads money into the logged-in user's account. */
@WebServlet(name = "DepositServlet", urlPatterns = "/deposit")
public class DepositServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(request, response, (engine, userName) -> {
            double amount = ServletUtils.getDoubleParameter(request, "amount");
            engine.deposit(userName, amount);
            return new ActionResultDTO(String.format("%.2f were added to your account.", amount));
        });
    }
}
