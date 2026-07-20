package com.homework.web.app.vo;

import com.homework.model.enums.MembershipOrderPayType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建会员订单后返回给前端的支付参数。
 *
 * <p>当前只接入微信 Native 支付，因此 mode 固定为 NATIVE，codeUrl 用于生成二维码。
 * 这里不返回商户私钥、APIv3 密钥等任何服务端凭据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPayloadVO {

    private MembershipOrderPayType payType;

    private String mode;

    private String codeUrl;
}
