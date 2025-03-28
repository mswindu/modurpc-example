package ru.snilov.modu.rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import ru.snilov.modu.rpc.api.exception.ModuRpcTransportException;
import ru.snilov.modu.rpc.data.ModuRpcRequest;
import ru.snilov.modu.rpc.data.ModuRpcResponse;
import ru.snilov.modu.rpc.serializer.ModuRpcSerializer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class ModuRpcClientMethodInterceptor implements MethodInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ModuRpcClientMethodInterceptor.class);

    private final HttpClient httpClient;
    private final ModuRpcSerializer javaSerializer;

    private final ConcurrentMap<Class<?>, String> apiUrls = new ConcurrentHashMap<>();

    @Autowired
    public ModuRpcClientMethodInterceptor(HttpClient httpClient, ModuRpcSerializer javaSerializer) {
        this.httpClient = httpClient;
        this.javaSerializer = javaSerializer;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if (isObjectMethod(method)) {
            return handleObjectMethod(obj, method, args);
        }

        String apiUrl = apiUrls.get(method.getDeclaringClass());
        if (apiUrl == null) {
            throw new IllegalArgumentException("No URL configured for API [%s]".formatted(method.getDeclaringClass().getName()));
        }

        String url = "%s/rpc/%s/%s".formatted(apiUrl, method.getDeclaringClass().getName(), method.getName());
        ModuRpcRequest moduRpcRequest = new ModuRpcRequest(args, method.getParameterTypes());

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Content-Type", "application/octet-stream")
                    .method("POST", HttpRequest.BodyPublishers.ofByteArray(javaSerializer.serialize(moduRpcRequest)));

            long startTime = System.nanoTime();

            HttpResponse<byte[]> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());

            long endTime = System.nanoTime();
            long durationMicros = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
            logger.debug("response time: {} microseconds", durationMicros);
            logger.debug("protocol version [{}]", response.version());

            if (response.statusCode() != 200) {
                throw new ModuRpcTransportException("RPC request failed with status: " + response.statusCode());
            }

            ModuRpcResponse moduRpcResponse = javaSerializer.deserialize(response.body(), ModuRpcResponse.class);

            if (moduRpcResponse == null) {
                throw new ModuRpcTransportException("RPC response is null");
            }

            if (moduRpcResponse.getResult() instanceof Throwable) {
                throw (Throwable) moduRpcResponse.getResult();
            }

            if (method.getReturnType().isInstance(moduRpcResponse.getResult())) {
                return method.getReturnType().cast(moduRpcResponse.getResult());
            }

            return moduRpcResponse.getResult();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new ModuRpcTransportException("RPC transport error: " + e.getMessage(), e);
        }
    }

    public <T> T createProxy(Class<T> clazz, String url) {
        apiUrls.put(clazz, url);

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(this);
        return (T) enhancer.create();
    }

    private boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
    }

    private Object handleObjectMethod(Object obj, Method method, Object[] args) {
        return switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(obj);
            case "equals" -> obj == args[0];
            case "toString" -> obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
            default -> throw new IllegalArgumentException("Unexpected method: " + method);
        };
    }
}