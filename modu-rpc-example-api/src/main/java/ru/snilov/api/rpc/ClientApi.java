package ru.snilov.api.rpc;

import ru.snilov.modu.rpc.api.ModuRpcApi;
import ru.snilov.modu.rpc.api.dto.PrimitiveDTO;

import java.util.List;

@ModuRpcApi
public interface ClientApi {
    String ping();

    List<String> getArgList(String... args);

    PrimitiveDTO usePrimitive(int a1, long a2, boolean a3, char a4, double a5);

    void thrownRuntimeException();

    void thrownUserException();
}
