package com.example.policlicabine.exception;

/**
 * Exception thrown when file storage operations fail.
 *
 * <p>This includes failures during:
 * <ul>
 *   <li>File upload/write operations</li>
 *   <li>File deletion operations</li>
 *   <li>Storage directory creation</li>
 *   <li>File system I/O errors</li>
 * </ul>
 *
 * @author PoliclicaBine System
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
