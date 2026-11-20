package io.github.snipperxx.applyledger.common;

/** The request is valid but clashes with the current state (duplicate name, illegal status change). Mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
