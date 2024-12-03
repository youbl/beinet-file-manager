package cn.beinet.deployment.admin.demos.dto;

import cn.beinet.core.base.annotations.InEnum;
import cn.beinet.deployment.admin.demos.enums.SexEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 新类
 * @author youbl
 * @since 2024/12/3 15:40
 */
@Data
@Accessors(chain = true)
public class DemoDto {
    private String name;

    @InEnum(enumClass = SexEnum.class, type = String.class)
    @NotNull(message = "sex不能为null")
    @NotEmpty(message = "sex不能为空")
    private String sex;

    private Integer age;
}
