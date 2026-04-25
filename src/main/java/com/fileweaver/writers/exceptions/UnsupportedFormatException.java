package com.fileweaver.writers.exceptions;

import com.fileweaver.writers.Format;

public class UnsupportedFormatException extends RuntimeException {
    public UnsupportedFormatException(Format format) {
        super("Unsupported output format: " + format);
    }
}
