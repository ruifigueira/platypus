package io.platypus;

public class IncompleteImplementationException extends RuntimeException {

    private static final long serialVersionUID = -3705846426001102284L;

    public IncompleteImplementationException() {
        super();
    }

    public IncompleteImplementationException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompleteImplementationException(String message) {
        super(message);
    }

    public IncompleteImplementationException(Throwable cause) {
        super(cause);
    }

}
