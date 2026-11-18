package com.assignment.transporter.api.error;

import java.io.IOException;

/** The request body is bigger than {@code transporter.limits.max-body-bytes}. */
public class PayloadTooLargeException extends IOException {

    private final long maxBytes;

    public PayloadTooLargeException(long maxBytes) {
        super("The request body must be at most " + maxBytes + " bytes.");
        this.maxBytes = maxBytes;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
