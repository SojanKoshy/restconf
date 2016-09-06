package org.onosproject.restconf.utils.exceptions;

/**
 * Represents class of errors related to YDT parse utils.
 */
public class YdtParseException extends RuntimeException {
    /**
     * Constructs an exception with the specified message.
     *
     * @param message the message describing the specific nature of the error
     */
    public YdtParseException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message and the underlying cause.
     *
     * @param message the message describing the specific nature of the error
     * @param cause   the underlying cause of this error
     */
    public YdtParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
