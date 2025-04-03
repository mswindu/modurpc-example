package ru.snilov.modu.rpc.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.snilov.modu.rpc.context.ModuRpcContext;
import ru.snilov.modu.rpc.data.ModuRpcRequest;
import ru.snilov.modu.rpc.data.ModuRpcResponse;
import ru.snilov.modu.rpc.serializer.ModuRpcSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class ModuRpcDispatcherServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ModuRpcDispatcherServlet.class);

    private final HandlerRegistry handlerRegistry;
    private final ModuRpcSerializer javaSerializer;

    public ModuRpcDispatcherServlet(HandlerRegistry handlerRegistry, ModuRpcSerializer javaSerializer) {
        this.handlerRegistry = handlerRegistry;
        this.javaSerializer = javaSerializer;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.split("/").length < 3) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid RPC request path");
            return;
        }

        String[] parts = pathInfo.split("/");
        String className = parts[1];
        String methodName = parts[2];

        // Получаем RPC контекст из заголовков
        String requestChainId = req.getHeader("X-MODURPC-Request-Chain-ID");
        String depthStr = req.getHeader("X-MODURPC-Depth");

        ModuRpcContext context = ModuRpcContext.getCurrent();
        context.setRequestChainId(requestChainId != null ? requestChainId : UUID.randomUUID().toString());
        context.setDepth(depthStr != null ? Integer.parseInt(depthStr) : 0);

        logger.debug("invoke method [{}.{}] with context [{}]", className, methodName, context);

        try (InputStream inputStream = req.getInputStream()) {
            byte[] requestBody = inputStream.readAllBytes();
            Object handler = handlerRegistry.getHandler(className);
            if (handler == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No handler found for api [%s]".formatted(className));
                return;
            }

            ModuRpcRequest moduRpcRequest = javaSerializer.deserialize(requestBody, ModuRpcRequest.class);
            Method method = findMethod(handler.getClass(), methodName, moduRpcRequest.getParameterTypes());
            if (method == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No method found [%s] in class [%s]".formatted(methodName, className));
                return;
            }

            Object result = method.invoke(handler, moduRpcRequest.getParameters());
            byte[] responseBody = javaSerializer.serialize(new ModuRpcResponse(result));

            resp.setContentType("application/octet-stream");
            resp.setStatus(HttpServletResponse.SC_OK);
            try (OutputStream outputStream = resp.getOutputStream()) {
                outputStream.write(responseBody);
            }
        } catch (InvocationTargetException e) {
            handleException(resp, e.getCause());
        } catch (Exception e) {
            handleException(resp, e);
        } finally {
            ModuRpcContext.clear();
        }
    }

    private void handleException(HttpServletResponse resp, Throwable e) throws IOException {
        byte[] errorResponse = javaSerializer.serialize(new ModuRpcResponse(e));
        resp.setStatus(HttpServletResponse.SC_OK);
        try (OutputStream outputStream = resp.getOutputStream()) {
            outputStream.write(errorResponse);
        }
    }

    private Method findMethod(Class<?> handlerClass, String methodName, Class<?>[] parameterTypes) {
        try {
            return handlerClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}

