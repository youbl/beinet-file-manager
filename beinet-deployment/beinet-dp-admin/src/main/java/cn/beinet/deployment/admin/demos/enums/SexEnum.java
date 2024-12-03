package cn.beinet.deployment.admin.demos.enums;

import cn.beinet.core.base.enums.EnumListValid;

/**
 * 定义一个性别枚举，输入不在 男/女 时要抛异常
 */
public enum SexEnum implements EnumListValid<String> {
    BOY("男"),
    GIRL("女");

    private String name;

    SexEnum(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return name;
    }
}
