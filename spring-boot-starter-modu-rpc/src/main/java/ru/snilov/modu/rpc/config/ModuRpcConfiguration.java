package ru.snilov.modu.rpc.config;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ru.snilov.modu.rpc.client.ModuRpcClientBeanDefinitionRegistryPostProcessor;
import ru.snilov.modu.rpc.client.ModuRpcClientMethodInterceptor;
import ru.snilov.modu.rpc.filter.ModuRpcMetricsFilter;
import ru.snilov.modu.rpc.serializer.KryoModuRpcSerializer;
import ru.snilov.modu.rpc.serializer.KryoPool;
import ru.snilov.modu.rpc.serializer.ModuRpcSerializer;
import ru.snilov.modu.rpc.server.HandlerRegistry;
import ru.snilov.modu.rpc.server.ModuRpcDispatcherServlet;
import ru.snilov.modu.rpc.server.ModuRpcServerBeanPostProcessor;
import io.micrometer.core.instrument.MeterRegistry;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
public class ModuRpcConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public Pool<Kryo> kryoPool(Environment environment) {
        int poolSize = environment.getProperty("modurpc.kryo.pool.size", Integer.class, 16);
        return new KryoPool(poolSize);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModuRpcSerializer rpcSerializer(Pool<Kryo> kryoPool) {
        return new KryoModuRpcSerializer(kryoPool);
    }

    @Bean
    public ModuRpcClientMethodInterceptor rpcClientMethodInterceptor(HttpClient httpClient, ModuRpcSerializer javaSerializer) {
        return new ModuRpcClientMethodInterceptor(httpClient, javaSerializer);
    }

    @Bean
    public ModuRpcClientBeanDefinitionRegistryPostProcessor rpcClientPostProcessor(Environment environment) {
        return new ModuRpcClientBeanDefinitionRegistryPostProcessor(environment);
    }

    @Bean
    public ServletRegistrationBean<ModuRpcDispatcherServlet> moduRpcServlet(
            HandlerRegistry handlerRegistry, ModuRpcSerializer javaSerializer) {
        return new ServletRegistrationBean<>(new ModuRpcDispatcherServlet(handlerRegistry, javaSerializer), "/rpc/*");
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public ModuRpcMetricsFilter moduRpcMetricsFilter(MeterRegistry meterRegistry, ModuRpcSerializer moduRpcSerializer) {
        return new ModuRpcMetricsFilter(meterRegistry, moduRpcSerializer);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public FilterRegistrationBean<ModuRpcMetricsFilter> moduRpcMetricsFilterFilterRegistrationBean(ModuRpcMetricsFilter filter) {
        FilterRegistrationBean<ModuRpcMetricsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/rpc/*"); // Применяем фильтр только к /rpc/*
        registrationBean.setOrder(1); // Приоритет фильтра (чем меньше число, тем раньше он выполняется)

        return registrationBean;
    }

    @Bean
    public HandlerRegistry handlerRegistry() {
        return new HandlerRegistry();
    }

    @Bean
    public ModuRpcServerBeanPostProcessor rpcServerPostProcessor(HandlerRegistry handlerRegistry) {
        return new ModuRpcServerBeanPostProcessor(handlerRegistry);
    }
}


