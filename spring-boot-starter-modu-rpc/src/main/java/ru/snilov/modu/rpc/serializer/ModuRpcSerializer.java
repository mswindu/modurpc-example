package ru.snilov.modu.rpc.serializer;

public interface ModuRpcSerializer {
    byte[] serialize(Object object) throws Exception;

    <T> T deserialize(byte[] data, Class<T> type) throws Exception;
}
