package com.image.generation.common;

/**
 * The two failures the API has an answer for, kept side by side because they are read
 * together: {@link ApiExceptionHandler} turns the first into a 404 and the second into
 * a 409, and there is nothing else to either of them.
 *
 * Both are checked, so a method that can fail this way says so in its signature rather
 * than leaving it to the caller to find out. Both also take an optional cause — the
 * one-argument constructors pass {@code null}, which {@link Throwable} treats as "not
 * yet set" rather than as a cause of its own.
 *
 * Java allows one public class per file; nesting them here is what lets the two share
 * one. Import the nested names directly
 * ({@code import com.image.generation.common.ApiExceptions.NotFoundException;}) so call
 * sites stay as short as they were.
 */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static class NotFoundException extends Exception {

        public NotFoundException(String message) {
            this(message, null);
        }

        public NotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public static NotFoundException of(String what, Object id) {
            return new NotFoundException(what + " " + id + " does not exist");
        }
    }

    public static class ConflictException extends Exception {

        public ConflictException(String message) {
            this(message, null);
        }

        public ConflictException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
