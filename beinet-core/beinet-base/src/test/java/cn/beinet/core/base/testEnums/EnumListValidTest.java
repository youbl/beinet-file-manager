package cn.beinet.core.base.testEnums;

import cn.beinet.core.base.UtilsTestApplication;
import cn.beinet.core.base.enums.BoolEnum;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = UtilsTestApplication.class)
public class EnumListValidTest {
    @Test
    public void testDemo() {
        var boolTrue = BoolEnum.TRUE;
        var existTrue = boolTrue.exist(1);
        Assert.assertTrue(existTrue);

        var existFalse = boolTrue.exist(0);
        Assert.assertTrue(existFalse);
    }
}