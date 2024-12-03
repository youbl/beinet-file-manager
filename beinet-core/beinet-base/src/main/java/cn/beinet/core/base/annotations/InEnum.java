package cn.beinet.core.base.annotations;

import cn.beinet.core.base.aspect.InEnumConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在属性上添加该注解，搭配@Valid进行判断：属性值是否在枚举列表里
 * @author youbl
 * @since 2024/12/3 15:29
 */
@Constraint(validatedBy = InEnumConstraintValidator.class)
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InEnum {

    String message() default "Invalid value. This is not permitted.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // enum class
    Class<? extends Enum<?>> enumClass();

    // value type: int or string
    Class<?> type();

}
