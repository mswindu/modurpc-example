package ru.snilov.modu.rpc.data;

public class ModuRpcRequest {
    private Object[] parameters;
    private Class<?>[] parameterTypes;

    public ModuRpcRequest(Object[] parameters, Class<?>[] parameterTypes) {
        this.parameters = parameters;
        this.parameterTypes = parameterTypes;
    }

    // Getters and Setters
    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }
}
