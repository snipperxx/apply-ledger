package io.github.snipperxx.applyledger.common;

/** Input that passes bean validation but breaks a business rule (for example a date in the future). Mapped to 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
