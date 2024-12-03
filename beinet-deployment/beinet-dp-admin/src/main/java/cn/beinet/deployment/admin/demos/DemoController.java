package cn.beinet.deployment.admin.demos;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.demos.dto.DemoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo演示类
 * @author youbl
 * @since 2024/12/3 15:41
 */
@RestController
@Tag(name = "demos", description = "demo演示类")
public class DemoController {
    // 可以在swagger里测试：http://localhost:8080/swagger-ui/index.html#/demos/demoInEnum
    @PostMapping("demo/inenum")
    @Operation(summary = "演示InEnum注解用法", description = "dto.sex值如果不是男/女，会抛MethodArgumentNotValidException异常")
    public ResponseData<String> demoInEnum(@Valid @RequestBody DemoDto dto) {
        return ResponseData.ok(dto.toString());
    }
}
