package client.http;

/** An error returned by the server (or a communication failure), with a message that can be shown to the user. */
public class ServerException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ServerException(String message) {
        super(message);
    }
}
