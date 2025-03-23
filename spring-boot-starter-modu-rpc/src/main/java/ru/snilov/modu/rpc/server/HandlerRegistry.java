package ru.snilov.modu.rpc.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HandlerRegistry {
    private final Map<String, Object> handlers = new ConcurrentHashMap<>();

    public <T> void registerHandler(Class<T> clazz, Object handler) {
        handlers.put(clazz.getName(), handler);
    }

    public Object getHandler(String className) {
        return handlers.get(className);
    }
}
