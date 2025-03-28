package ru.snilov.modu.rpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import ru.snilov.modu.rpc.api.ModuRpcApi;

public class ModuRpcServerBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(ModuRpcServerBeanPostProcessor.class);

    private final HandlerRegistry handlerRegistry;
    private ApplicationContext applicationContext;

    public ModuRpcServerBeanPostProcessor(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof FactoryBean) {
            return bean; // Пропускаем фабрики, так как они создают прокси
        }

        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) applicationContext;
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) ctx.getBeanFactory();

        // Пропускаем сгенерированные клиенты для RPC
        if (registry.containsBeanDefinition(beanName)) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            Object isRpcClient = beanDefinition.getAttribute("modurpc.client");
            if (Boolean.TRUE.equals(isRpcClient)) {
                return bean;
            }
        }

        // Регистрируем обработчики для сервера
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
