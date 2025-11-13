package com.duckbot.services.impl;

import com.duckbot.services.LogService;
import com.duckbot.util.DataPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple log service that fans out to SLF4J and a daily log file.
 */
public final class FileLogService implements LogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private final Logger logger = LoggerFactory.getLogger(FileLogService.class);
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void debug(String fmt, Object... args) {
        logger.debug(fmt, args);
        write("DEBUG", format(fmt, args));
    }

    @Override
    public void info(String fmt, Object... args) {
        logger.info(fmt, args);
        write("INFO", format(fmt, args));
    }

    @Override
    public void warn(String fmt, Object... args) {
        logger.warn(fmt, args);
        write("WARN", format(fmt, args));
    }

    @Override
    public void error(String fmt, Object... args) {
        logger.error(fmt, args);
        write("ERROR", format(fmt, args));
    }

    private void write(String level, String message) {
        Path logFile = DataPaths.logFileForToday();
        String line = String.format("%s [%s] %s%n", FORMATTER.format(Instant.now()), level, message);
        lock.lock();
        try {
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        } finally {
            lock.unlock();
        }
    }

    private String format(String fmt, Object... args) {
        if (args == null || args.length == 0) {
            return fmt;
        }
        StringBuilder builder = new StringBuilder();
        int argIndex = 0;
        for (int i = 0; i < fmt.length(); i++) {
            if (i < fmt.length() - 1 && fmt.charAt(i) == '{' && fmt.charAt(i + 1) == '}' && argIndex < args.length) {
                builder.append(String.valueOf(args[argIndex++]));
                i++; // Skip closing brace
            } else {
                builder.append(fmt.charAt(i));
            }
        }
        while (argIndex < args.length) {
            builder.append(' ').append(String.valueOf(args[argIndex++]));
        }
        return builder.toString();
    }
}