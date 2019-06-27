package ru.craftlogic.towns.utils;

@FunctionalInterface
public interface FloatFunction<R> {
    R apply(float value);
}
