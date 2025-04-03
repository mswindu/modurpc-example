package ru.snilov.modu.rpc.task;

import org.springframework.core.task.TaskDecorator;
import ru.snilov.modu.rpc.context.ModuRpcContext;

/**
 * Декоратор для передачи контекста в асинхронные задачи
 * Использовать при конфигурации AsyncConfigurer, например,
 *     public TaskExecutor getAsyncExecutor() {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setTaskDecorator(new ContextAwareTaskDecorator());
 *         executor.initialize();
 *         return executor;
 *     }
 */
public class ModuRpcContextAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        ModuRpcContext originalModuContext = ModuRpcContext.getCurrent();
        boolean hasModuContext = (originalModuContext.getRequestChainId() != null);

        // Если нет контекстов, просто возвращаем оригинальную задачу
        if (!hasModuContext) {
            return runnable;
        }

        return () -> {
            try {
                ModuRpcContext.setContext(originalModuContext.copy());

                runnable.run();
            } finally {
                ModuRpcContext.clearContext();
            }
        };
    }
}
