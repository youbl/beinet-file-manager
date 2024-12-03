package cn.beinet.core.base.enums;

import lombok.Getter;

@Getter
public enum BoolEnum implements EnumListValid<Integer> {

    /**
     * 状态，0：否； 1：是；
     */
    FALSE(0),
    TRUE(1),
    ;

    private final Integer value;

    BoolEnum(Integer value) {
        this.value = value;
    }
}
