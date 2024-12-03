package cn.beinet.deployment.admin.stores.enums;

import cn.beinet.core.base.consts.ErrorCodeConst;
import cn.beinet.core.base.exceptions.BaseErrorEnums;
import lombok.Getter;

@Getter
public enum StoreErrorCode implements BaseErrorEnums {
    STORE_ERR_NO_BASE_DIR_CONFIG("yml目录配置不存在", 1),
    STORE_ERR_NO_PERMISSION("不允许访问的目录", 2),
    STORE_ERR_FILE_EXISTS("文件已存在", 3),
    STORE_ERR_FILE_NOT_EXISTS("文件不存在", 4),
    ;
    private final int errorCode;
    private final String errorMsg;

    StoreErrorCode(String errorMsg, int errorCode) {
        this.errorCode = ErrorCodeConst.STORE_START + errorCode;
        this.errorMsg = errorMsg;
    }
}
