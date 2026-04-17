package com.shweit.serverapi.handlers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class LogHandler extends Handler {
    private static final int MAX_LOG_ENTRIES = 1000;
    private final Deque<HashMap<String, String>> log = new ConcurrentLinkedDeque<>();

    @Override
    public void publish(final LogRecord record) {
        HashMap<String, String> logRecord = new HashMap<>();
        logRecord.put("level", record.getLevel().getName());
        logRecord.put("message", record.getMessage());

        String readableTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(record.getMillis());
        logRecord.put("time", readableTime);
        log.addLast(logRecord);
        while (log.size() > MAX_LOG_ENTRIES) {
            log.pollFirst();
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }

    public List<HashMap<String, String>> getLog() {
        return new ArrayList<>(log);
    }
}
