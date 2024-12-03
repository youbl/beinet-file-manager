package cn.beinet.core.base.enums;

import java.util.Arrays;

/**
 * 用于列表校验的枚举接口,
 * 搭配aspect使用
 * @author youbl
 * @since 2024/12/2 20:43
 */
public interface EnumListValid<T> {
    T getValue();

    default boolean matches(T value) {
        return this.getValue().equals(value);
    }

    default boolean exist(T value) {
        return Arrays.stream(this.getClass().getEnumConstants())
                .map(EnumListValid::getValue)
                .anyMatch(v -> v.equals(value));
    }
}
