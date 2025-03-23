package ru.snilov.modu.rpc.client;

import org.springframework.beans.factory.FactoryBean;

public class ModuRpcClientFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> apiInterface;
    private final String url;
    private final ModuRpcClientMethodInterceptor interceptor;

    public ModuRpcClientFactoryBean(Class<T> apiInterface, String url, ModuRpcClientMethodInterceptor interceptor) {
        this.apiInterface = apiInterface;
        this.url = url;
        this.interceptor = interceptor;
    }

    @Override
    public T getObject() {
        return interceptor.createProxy(apiInterface, url);
    }

    @Override
    public Class<?> getObjectType() {
        return apiInterface;
    }
}
