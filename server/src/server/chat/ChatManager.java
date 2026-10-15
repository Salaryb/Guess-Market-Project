package server.chat;

import dto.ChatDTO;
import dto.ChatLineDTO;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Keeps all chat lines in memory. Thread safe: every method is synchronized. */
public class ChatManager {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final List<ChatLineDTO> lines = new ArrayList<>();

    public synchronized void addLine(String userName, String text) {
        lines.add(new ChatLineDTO(userName, text, LocalTime.now().format(TIME_FORMAT)));
    }

    public synchronized ChatDTO getLinesFrom(int from) {
        int start = Math.min(from, lines.size());
        return new ChatDTO(lines.size(), new ArrayList<>(lines.subList(start, lines.size())));
    }
}
