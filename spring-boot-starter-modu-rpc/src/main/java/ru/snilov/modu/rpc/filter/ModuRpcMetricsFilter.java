package ru.snilov.modu.rpc.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;
import ru.snilov.modu.rpc.data.ModuRpcResponse;
import ru.snilov.modu.rpc.serializer.ModuRpcSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ModuRpcMetricsFilter implements Filter {
    private final MeterRegistry meterRegistry;
    private final ModuRpcSerializer moduRpcSerializer;
    private final Map<String, Timer> timersCache = new ConcurrentHashMap<>();

    public ModuRpcMetricsFilter(MeterRegistry meterRegistry, ModuRpcSerializer moduRpcSerializer) {
        this.meterRegistry = meterRegistry;
        this.moduRpcSerializer = moduRpcSerializer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String pathInfo = httpRequest.getRequestURI();
        if (pathInfo == null || !pathInfo.startsWith("/rpc/")) {
            chain.doFilter(request, response);
            return;
        }

        String[] parts = pathInfo.split("/");
        if (parts.length < 3) {
            chain.doFilter(request, response);
            return;
        }

        String className = parts[2];
        String methodName = parts[3];

        long startTime = System.nanoTime();

        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(httpResponse);

        try {
            chain.doFilter(request, cachingResponse);
        } finally {
            cachingResponse.copyBodyToResponse(); // Важно: передаем данные в оригинальный response

            long duration = System.nanoTime() - startTime;
            String exceptionName = extractExceptionName(cachingResponse.getContentAsByteArray());

            recordMetrics(className, methodName, duration, exceptionName);
        }
    }

    private String extractExceptionName(byte[] responseContent) {
        if (responseContent.length == 0) {
            return "None";
        }

        try {
            ModuRpcResponse response = moduRpcSerializer.deserialize(responseContent, ModuRpcResponse.class);
            return (response.getResult() instanceof Throwable throwable) ? throwable.getClass().getName() : "None";
        } catch (Exception ignored) {
            return "None";
        }
    }

    private void recordMetrics(String className, String methodName, long duration, String exceptionName) {
        String key = className + "." + methodName + "." + exceptionName;

        // Используем кеширование `Timer` по API + методу + исключению
        Timer timer = timersCache.computeIfAbsent(key, k -> Timer.builder("modurpc_server_requests")
                .tag("api", className)
                .tag("method", methodName)
                .tag("exception", exceptionName)
                .register(meterRegistry));

        timer.record(duration, TimeUnit.NANOSECONDS);
    }
}
