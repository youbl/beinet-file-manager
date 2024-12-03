package cn.beinet.core.base.aspect;

import cn.beinet.core.base.annotations.InEnum;
import cn.beinet.core.base.enums.EnumListValid;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 搭配InEnum注解使用的校验类
 * @author youbl
 * @since 2024/12/3 15:30
 */
public class InEnumConstraintValidator implements ConstraintValidator<InEnum, Object> {
    private Class<? extends Enum> enumClass;

    @Override
    public void initialize(InEnum constraintAnnotation) {
        enumClass = constraintAnnotation.enumClass();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        // 如果值为null，直接返回true，表示校验通过
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            return true;
        }
        Enum[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            return true;
        }
        Enum enumConstant = enumConstants[0];
        boolean isImpl = enumConstant instanceof EnumListValid;
        if (!isImpl) {
            //为实现的不校验
            return true;
        }
        EnumListValid enumListValidService = (EnumListValid) enumConstant;
        return enumListValidService.exist(value);
    }
}
