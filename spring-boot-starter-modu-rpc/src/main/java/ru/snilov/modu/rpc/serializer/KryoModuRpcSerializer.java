package ru.snilov.modu.rpc.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.snilov.modu.rpc.api.exception.ModuRpcTransportException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

@Component
public class KryoModuRpcSerializer implements ModuRpcSerializer {
    private static final Logger logger = LoggerFactory.getLogger(KryoModuRpcSerializer.class);

    private final Pool<Kryo> kryoPool;

    public KryoModuRpcSerializer(Pool<Kryo> kryoPool) {
        this.kryoPool = kryoPool;
    }

    @Override
    public byte[] serialize(Object object) {
        long startTime = System.nanoTime();
        Kryo kryo = kryoPool.obtain();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            kryo.writeObject(output, object);
            output.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new ModuRpcTransportException("Failed to serialize object", e);
        } finally {
            kryoPool.free(kryo); // Вернем экземпляр в пул
            long endTime = System.nanoTime();
            long durationMicros = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
            logger.debug("Serialization time: {} microseconds", durationMicros);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> type) {
        long startTime = System.nanoTime();
        Kryo kryo = kryoPool.obtain();
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             Input input = new Input(byteArrayInputStream)) {
            return kryo.readObject(input, type);
        } catch (Exception e) {
            throw new ModuRpcTransportException("Failed to deserialize object", e);
        } finally {
            kryoPool.free(kryo); // Вернем экземпляр в пул
            long endTime = System.nanoTime();
            long durationMicros = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
            logger.debug("Deserialization time: {} microseconds", durationMicros);
        }
    }
}

