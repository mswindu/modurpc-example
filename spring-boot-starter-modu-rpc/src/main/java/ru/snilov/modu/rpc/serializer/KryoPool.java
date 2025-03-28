package ru.snilov.modu.rpc.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.util.UUID;

public class KryoPool extends Pool<Kryo> {

    public KryoPool(int poolSize) {
        super(true, true, poolSize);
    }

    @Override
    protected Kryo create() {
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        kryo.setRegistrationRequired(false);
        kryo.register(UUID.class, new DefaultSerializers.UUIDSerializer());

        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
        kryo.register(Throwable.class, new JavaSerializer());
        return kryo;
    }
}
