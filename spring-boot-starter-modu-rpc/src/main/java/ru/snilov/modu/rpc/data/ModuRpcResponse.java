package ru.snilov.modu.rpc.data;

public class ModuRpcResponse {
    private Object result;

    public ModuRpcResponse(Object result) {
        this.result = result;
    }

    // Getters and Setters
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
