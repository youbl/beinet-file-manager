package cn.beinet.deployment.admin.stores.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文件或目录对象
 *
 * @author youbl
 * @since 2024/3/25 13:48
 */
@Data
@Accessors(chain = true)
@Schema(description = "文件或目录对象")
public class StoreInfo {
    /**
     * 文件或目录名
     */
    @Schema(description = "文件或目录名")
    private String name;
    /**
     * 文件或目录名
     */
    @Schema(description = "文件或目录完整路径")
    private String path;
    /**
     * 是否目录
     */
    @Schema(description = "是否目录")
    private boolean dir;
    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private long size;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private long created;
    /**
     * 最后修改时间
     */
    @Schema(description = "最后修改时间")
    private long modified;

    @Schema(description = "是否只读")
    private boolean readonly = true;
}
