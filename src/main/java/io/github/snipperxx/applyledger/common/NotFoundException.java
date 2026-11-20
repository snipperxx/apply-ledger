package io.github.snipperxx.applyledger.common;

/** The requested resource does not exist. Mapped to 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, long id) {
        super(what + " " + id + " not found");
    }
}
