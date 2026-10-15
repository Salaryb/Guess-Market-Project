package server.utils;

import com.google.gson.Gson;
import dto.ActionResultDTO;
import engine.GuessMarketEngineImpl;
import engine.IGuessMarketEngine;
import exception.GuessMarketException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.chat.ChatManager;

import java.io.IOException;

/** Shared server helpers: the single engine instance, JSON writing and parameter reading. */
public final class ServletUtils {
    public static final Gson GSON = new Gson();
    private static final Object ENGINE_LOCK = new Object();
    private static final Object CHAT_LOCK = new Object();

    private ServletUtils() {
    }

    /** The one engine of the server, created on first use and kept in the servlet context. */
    public static IGuessMarketEngine getEngine(ServletContext context) {
        synchronized (ENGINE_LOCK) {
            if (context.getAttribute(Constants.ENGINE_ATTRIBUTE) == null) {
                context.setAttribute(Constants.ENGINE_ATTRIBUTE, new GuessMarketEngineImpl());
            }
        }
        return (IGuessMarketEngine) context.getAttribute(Constants.ENGINE_ATTRIBUTE);
    }

    public static ChatManager getChatManager(ServletContext context) {
        synchronized (CHAT_LOCK) {
            if (context.getAttribute(Constants.CHAT_ATTRIBUTE) == null) {
                context.setAttribute(Constants.CHAT_ATTRIBUTE, new ChatManager());
            }
        }
        return (ChatManager) context.getAttribute(Constants.CHAT_ATTRIBUTE);
    }

    public static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(GSON.toJson(body));
    }

    public static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, new ActionResultDTO(message));
    }

    public static String getRequiredParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new GuessMarketException("Missing required parameter: '" + name + "'.");
        }
        return value.trim();
    }

    public static int getIntParameter(HttpServletRequest request, String name) {
        String value = getRequiredParameter(request, name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new GuessMarketException("'" + name + "' must be a whole number (got '" + value + "').");
        }
    }

    public static double getDoubleParameter(HttpServletRequest request, String name) {
        String value = getRequiredParameter(request, name);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new GuessMarketException("'" + name + "' must be a number (got '" + value + "').");
        }
    }
}
