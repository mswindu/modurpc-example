package ru.snilov.modu.rpc.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.snilov.modu.rpc.data.ModuRpcRequest;
import ru.snilov.modu.rpc.data.ModuRpcResponse;
import ru.snilov.modu.rpc.serializer.ModuRpcSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ModuRpcDispatcherServlet extends HttpServlet {
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
