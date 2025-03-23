package ru.snilov.modu.rpc.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.snilov.api.rpc.ClientApi;
import ru.snilov.api.rpc.exception.CustomException;
import ru.snilov.modu.rpc.api.dto.PrimitiveDTO;

import java.util.List;

@Service
@Slf4j
public class ClientApiHandler implements ClientApi {
    @Override
    public String ping() {
        log.info("call method [ping]");
        return "pong";
    }

    @Override
    public List<String> getArgList(String... args) {
        log.info("call method [getArgList] args [{}]", List.of(args));
        return List.of(args);
    }

    @Override
    public PrimitiveDTO usePrimitive(int a1, long a2, boolean a3, char a4, double a5) {
        log.info("call method [usePrimitive]");
        return new PrimitiveDTO(a1, a2, a3, a4, a5);
    }

    @Override
    public void thrownRuntimeException() {
        throw new RuntimeException("runtime exception");
    }

    @Override
    public void thrownUserException() {
        try {
            throw new Exception("user exception");
        } catch (Exception e) {
            throw new CustomException("user exception", e);
        }
    }
}
