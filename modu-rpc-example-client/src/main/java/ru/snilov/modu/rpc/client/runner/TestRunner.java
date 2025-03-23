package ru.snilov.modu.rpc.client.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.snilov.api.rpc.ClientApi;
import ru.snilov.api.rpc.exception.CustomException;
import ru.snilov.modu.rpc.api.dto.PrimitiveDTO;

import java.util.List;

@Component
@Slf4j
public class TestRunner implements ApplicationRunner {
    private final ClientApi clientApi;

    @Autowired
    public TestRunner(ClientApi clientApi) {
        this.clientApi = clientApi;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String ping = clientApi.ping();
        log.info(ping);

        List<String> argList = clientApi.getArgList("Arg1", "Arg2", "Arg3");
        log.info(argList.toString());

        PrimitiveDTO a = clientApi.usePrimitive(1, 1L, false, 'a', 3.3);
        log.info(a.toString());

        try {
            clientApi.thrownRuntimeException();
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }

        try {
            clientApi.thrownUserException();
        } catch (CustomException e) {
            log.error(e.getMessage());
        }
    }
}
