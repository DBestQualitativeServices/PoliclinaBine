package com.example.policlicabine.exception;

/**
 * Exception thrown when an uploaded file has an invalid or unsupported type.
 *
 * <p>This occurs when:
 * <ul>
 *   <li>File MIME type is not in the allowed types list</li>
 *   <li>File extension doesn't match MIME type</li>
 *   <li>File type is blacklisted for security reasons</li>
 * </ul>
 *
 * @author PoliclicaBine System
 */
public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException(String message) {
        super(message);
    }

    public InvalidFileTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
