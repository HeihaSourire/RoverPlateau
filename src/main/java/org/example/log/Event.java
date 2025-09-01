package org.example.log;

public interface Event extends AutoCloseable{
    void info(String type, String message, Object... kv);
    void warn(String type, String message, Object... kv);
    @Override default void close() {}
}
