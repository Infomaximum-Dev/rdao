package com.infomaximum.database.exeption;

public class InconsistentDatabaseException extends DatabaseException {

    public InconsistentDatabaseException(String message) {
        super(message);
    }

    public InconsistentDatabaseException(Throwable cause) {
        super(cause);
    }

    public InconsistentDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
