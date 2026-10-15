package server.servlets;

import dto.ActionResultDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import server.utils.ServletUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * POST /upload-file (multipart, part "file") - uploads an events XML file.
 * The content is read into memory and parsed directly; nothing is written to the server's disk.
 */
@WebServlet(name = "UploadFileServlet", urlPatterns = "/upload-file")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class UploadFileServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Part filePart;
        try {
            filePart = request.getPart("file");
        } catch (ServletException | IllegalStateException e) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Failed reading the uploaded file: " + e.getMessage());
            return;
        }
        if (filePart == null) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "No file was uploaded (expected a part named 'file').");
            return;
        }
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            ServletUtils.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "The file '" + fileName + "' is not an XML file (it must end with .xml).");
            return;
        }

        byte[] content;
        try (InputStream in = filePart.getInputStream()) {
            content = in.readAllBytes();
        }
        respond(request, response, (engine, userName) -> {
            List<String> added = engine.loadEventsFromXml(new ByteArrayInputStream(content), userName);
            return new ActionResultDTO("The file '" + fileName + "' was loaded successfully. "
                    + added.size() + " event(s) were added and you are their market maker: " + String.join(", ", added));
        });
    }
}
