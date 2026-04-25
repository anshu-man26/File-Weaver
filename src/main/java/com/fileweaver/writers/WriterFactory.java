package com.fileweaver.writers;

import com.fileweaver.writers.exceptions.UnsupportedFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WriterFactory {

    private static final Logger log = LoggerFactory.getLogger(WriterFactory.class);

    private final Map<Format, Writer> byFormat;

    public WriterFactory(List<Writer> writers) {
        this.byFormat = writers.stream()
            .collect(Collectors.toMap(Writer::format, Function.identity()));
        log.info("Registered {} writers: {}", byFormat.size(), byFormat.keySet());
    }

    public Writer resolve(Format format) {
        Writer w = byFormat.get(format);
        if (w == null) throw new UnsupportedFormatException(format);
        return w;
    }
}
