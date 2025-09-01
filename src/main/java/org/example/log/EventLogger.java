package org.example.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.ObjectMessage;

import org.example.Model.Direction;

import java.util.LinkedHashMap;
import java.util.Map;

public class EventLogger implements Event{
    private static final Logger LOG = LogManager.getLogger("events");
    @Override
    public void info(String type, String message, Object... kv) {
        log(Level.INFO, type, message, kv);
    }

    @Override
    public void warn(String type, String message, Object... kv) {
        log(Level.WARN, type, message, kv);
    }

    private static void log(Level level, String type, String msg, Object... kv) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("msg", msg);
        // 将成对 kv 折叠为一个 Map；Direction 用单字符更紧凑
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String key = String.valueOf(kv[i]);
            Object val = kv[i + 1];
            if (val instanceof Direction d) val = String.valueOf(d.name());
            payload.put(key, val);
        }
        LOG.log(level, new ObjectMessage(payload));
    }
}
