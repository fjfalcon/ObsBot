package com.fjfalcon.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LambdaUtils {
    private static <T extends Throwable> void throwAsUnchecked(Exception exception) throws T {
        throw (T) exception;
    }

    public static <T, Z, E extends Exception> BiConsumer<T, Z> wrapConsumer(BiConsumerWithException<T,Z,E> consumer) {
        return (t,z) -> {
            try {
                consumer.accept(t, z);
            } catch (Exception exception) {
                throwAsUnchecked(exception);
            }
        };
    }

    @FunctionalInterface
    public interface BiConsumerWithException<T,Z, E extends Exception> {
        void accept(T t,Z z) throws E;
    }


}
