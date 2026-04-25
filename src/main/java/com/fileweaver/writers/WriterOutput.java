package com.fileweaver.writers;

public record WriterOutput(byte[] bytes, String contentType, String fileExtension) {}
