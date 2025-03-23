package ru.snilov.modu.rpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import ru.snilov.modu.rpc.api.ModuRpcApi;

public class ModuRpcServerBeanPostProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ModuRpcServerBeanPostProcessor.class);

    private final HandlerRegistry handlerRegistry;

    public ModuRpcServerBeanPostProcessor(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        for (Class<?> iface : beanClass.getInterfaces()) {
            if (iface.isAnnotationPresent(ModuRpcApi.class)) {
                handlerRegistry.registerHandler(iface, bean);
                logger.info("Registered RPC handler: {}", iface.getName());
            }
        }
        return bean;
    }
}
