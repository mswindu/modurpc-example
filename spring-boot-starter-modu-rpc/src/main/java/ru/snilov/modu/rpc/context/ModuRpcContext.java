package ru.snilov.modu.rpc.context;

public class ModuRpcContext {
    private static final ThreadLocal<ModuRpcContext> CONTEXT = ThreadLocal.withInitial(ModuRpcContext::new);

    private String requestChainId;
    private int depth;

    // Получение текущего контекста
    public static ModuRpcContext getCurrent() {
        return CONTEXT.get();
    }

    // Установка контекста (копирование значений из другого контекста)
    public static void setContext(ModuRpcContext context) {
        if (context == null) {
            clearContext();
            return;
        }

        ModuRpcContext current = CONTEXT.get();
        current.requestChainId = context.requestChainId;
        current.depth = context.depth;
    }

    // Очистка контекста
    public static void clearContext() {
        ModuRpcContext current = CONTEXT.get();
        current.requestChainId = null;
        current.depth = 0;
    }

    // Полная очистка (удаление ThreadLocal)
    public static void clear() {
        CONTEXT.remove();
    }

    // Геттеры и сеттеры
    public String getRequestChainId() {
        return requestChainId;
    }

    public void setRequestChainId(String requestChainId) {
        this.requestChainId = requestChainId;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    // Создание копии контекста
    public ModuRpcContext copy() {
        ModuRpcContext copy = new ModuRpcContext();
        copy.requestChainId = this.requestChainId;
        copy.depth = this.depth;
        return copy;
    }

    @Override
    public String toString() {
        return "ModuRpcContext{" +
                "requestChainId='" + requestChainId + '\'' +
                ", depth=" + depth +
                '}';
    }
}
