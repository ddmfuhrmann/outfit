package github.io.ddmfuhrmann.outfit.query.application.exception;

import github.io.ddmfuhrmann.outfit.shared.domain.exception.ElasticsearchException;

public class IndexingException extends ElasticsearchException {

    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}
