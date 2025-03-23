package ru.snilov.modu.rpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.snilov.modu.rpc.data.ModuRpcRequest;
import ru.snilov.modu.rpc.data.ModuRpcResponse;
import ru.snilov.modu.rpc.serializer.ModuRpcSerializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RestController
@RequestMapping("/rpc")
public class ModuRpcServer {
    private final HandlerRegistry handlerRegistry;
    private final ModuRpcSerializer javaSerializer;

    private static final Logger logger = LoggerFactory.getLogger(ModuRpcServer.class);

    @Autowired
    public ModuRpcServer(HandlerRegistry handlerRegistry, ModuRpcSerializer javaSerializer) {
        this.handlerRegistry = handlerRegistry;
        this.javaSerializer = javaSerializer;
    }

    @PostMapping("/{className}/{methodName}")
    public byte[] handleRpcRequest(@PathVariable String className, @PathVariable String methodName, @RequestBody byte[] requestBody) throws Throwable {
        logger.debug("call {}.{} with body length [{}]", className, methodName, requestBody.length);

        try {
            Object handler = handlerRegistry.getHandler(className);
            if (handler == null) {
                throw new IllegalArgumentException("No handler found for api [%s]".formatted(className));
            }

            ModuRpcRequest moduRpcRequest = javaSerializer.deserialize(requestBody, ModuRpcRequest.class);
            Method method = findMethod(handler.getClass(), methodName, moduRpcRequest.getParameterTypes());
            if (method == null) {
                throw new IllegalArgumentException("No method found [%s] in class [%s]".formatted(methodName, className));
            }

            Object result = method.invoke(handler, moduRpcRequest.getParameters());
            return javaSerializer.serialize(new ModuRpcResponse(result));
        } catch (InvocationTargetException e) {
            return javaSerializer.serialize(new ModuRpcResponse(e.getCause()));
        } catch (Throwable e) {
            return javaSerializer.serialize(new ModuRpcResponse(e));
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

