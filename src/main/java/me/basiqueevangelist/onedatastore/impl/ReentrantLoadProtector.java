package me.basiqueevangelist.onedatastore.impl;

import java.util.function.Supplier;

public class ReentrantLoadProtector {
    private final ThreadLocal<Boolean> IS_IN_METHOD = ThreadLocal.withInitial(() -> false);
    private final Supplier<RuntimeException> exceptionFactory;

    public ReentrantLoadProtector(Supplier<RuntimeException> exceptionFactory) {
        this.exceptionFactory = exceptionFactory;
    }

    public Scope enter() {
        if (IS_IN_METHOD.get()) {
            throw exceptionFactory.get();
        }

        IS_IN_METHOD.set(true);

        return new Scope();
    }

    public class Scope implements AutoCloseable {
        private Scope() { }

        @Override
        public void close() {
            IS_IN_METHOD.set(false);
        }
    }
}
