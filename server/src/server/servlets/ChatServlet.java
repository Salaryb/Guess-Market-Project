package server.servlets;

import dto.ActionResultDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.chat.ChatManager;
import server.utils.ServletUtils;
import server.utils.SessionUtils;

import java.io.IOException;

/**
 * GET  /chat?from=N   - chat lines from index N (delta fetching) + the current chat version.
 * POST /chat (message) - adds a line written by the logged-in user.
 */
@WebServlet(name = "ChatServlet", urlPatterns = "/chat")
public class ChatServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SessionUtils.getUsername(request) == null) {
            ServletUtils.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "You are not logged in.");
            return;
        }
        int from = parseFrom(request.getParameter("from"));
        ServletUtils.writeJson(response, HttpServletResponse.SC_OK, chat().getLinesFrom(from));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userName = SessionUtils.getUsername(request);
        if (userName == null) {
            ServletUtils.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "You are not logged in.");
            return;
        }
        String message = request.getParameter("message");
        if (message == null || message.isBlank()) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Cannot send an empty message.");
            return;
        }
        chat().addLine(userName, message.trim());
        ServletUtils.writeJson(response, HttpServletResponse.SC_OK, new ActionResultDTO("Sent."));
    }

    private ChatManager chat() {
        return ServletUtils.getChatManager(getServletContext());
    }

    private static int parseFrom(String value) {
        try {
            return value == null ? 0 : Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
