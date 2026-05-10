package github.io.ddmfuhrmann.outfit.query.application.exception;

import github.io.ddmfuhrmann.outfit.shared.domain.exception.ElasticsearchException;

public class QueryException extends ElasticsearchException {

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
