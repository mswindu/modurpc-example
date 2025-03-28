package ru.snilov.modu.rpc.serializer;

public interface ModuRpcSerializer {
    byte[] serialize(Object object);

    <T> T deserialize(byte[] data, Class<T> type);
}
