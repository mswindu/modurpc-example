package ru.snilov.modu.rpc.config;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ru.snilov.modu.rpc.client.ModuRpcClientBeanDefinitionRegistryPostProcessor;
import ru.snilov.modu.rpc.client.ModuRpcClientMethodInterceptor;
import ru.snilov.modu.rpc.serializer.KryoModuRpcSerializer;
import ru.snilov.modu.rpc.serializer.ModuRpcSerializer;
import ru.snilov.modu.rpc.server.HandlerRegistry;
import ru.snilov.modu.rpc.server.ModuRpcServer;
import ru.snilov.modu.rpc.server.ModuRpcServerBeanPostProcessor;

import java.net.http.HttpClient;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class ModuRpcConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public Kryo kryo() {
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        kryo.setRegistrationRequired(false);
        kryo.register(UUID.class, new DefaultSerializers.UUIDSerializer());

        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
        kryo.register(Throwable.class, new JavaSerializer());
        return kryo;
    }

    @Bean
    @ConditionalOnMissingBean
    public ModuRpcSerializer rpcSerializer(Kryo kryo) {
        return new KryoModuRpcSerializer(kryo);
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
    @ConditionalOnMissingBean
    public ModuRpcServer rpcServer(HandlerRegistry handlerRegistry, ModuRpcSerializer javaSerializer) {
        return new ModuRpcServer(handlerRegistry, javaSerializer);
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
