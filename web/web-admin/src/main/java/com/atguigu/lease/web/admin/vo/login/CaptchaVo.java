package com.atguigu.lease.web.admin.vo.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Schema(description = "图像验证码")
public class CaptchaVo {

    @Schema(description="验证码图片信息")
    private String image;

    @Schema(description="验证码key")
    private String key;

    public CaptchaVo(String image, String key) {
        this.image = image;
        this.key = key;
    }

    public CaptchaVo() {
    }
}
