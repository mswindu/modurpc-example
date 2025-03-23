package ru.snilov.modu.rpc.api.exception;

public class ModuRpcTransportException extends RuntimeException {
    public ModuRpcTransportException(String message) {
        super(message);
    }

    public ModuRpcTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
