package github.io.ddmfuhrmann.outfit.shared.domain.exception;

public class ElasticsearchException extends RuntimeException {

    public ElasticsearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
