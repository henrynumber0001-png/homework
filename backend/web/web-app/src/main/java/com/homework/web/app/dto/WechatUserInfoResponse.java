package com.homework.web.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WechatUserInfoResponse {

    private String openid;

    private String nickname;

    private Integer sex;

    private String province;

    private String city;

    private String country;

    @JsonProperty("headimgurl")
    private String headImgUrl;

    private List<String> privilege;

    private String unionid;

    private Integer errcode;

    private String errmsg;
}
/*
WechatUserInfoResponse 是用来接收微信“获取用户信息接口”的返回结果的。
微信登录比 Google/Apple 多一个 Response 类，是因为它的接口流程多一步。
Google/Apple 通常是：
authCode
  ↓
换 token
  ↓
拿到 id_token
  ↓
校验 id_token
  ↓
直接从 id_token 里取 sub / email / name / avatar
所以 Google/Apple 只需要类似：GoogleTokenResponse

因为用户信息已经在 id_token 里了。
*/

/*
微信是：
authCode
  ↓
换 access_token
  ↓
拿到 access_token + openid + unionid (WechatTokenResponse)
  ↓
再调用 (微信的)userinfo 接口
  ↓
拿 nickname / headimgurl / province / city 等用户资料 (WechatUserInfoResponse)

所以微信通常需要两个 Response：
WechatTokenResponse 用来接收 code 换 token 的结果：
{
  "access_token": "...",
  "expires_in": 7200,
  "refresh_token": "...",
  "openid": "...",
  "scope": "...",
  "unionid": "..."
}
以及：WechatUserInfoResponse 用来接收用户资料：
这里主要是需要 nickname 和 headimgurl 信息 （如果要拿unionid 和 openid, 优先去WechatTokenResponse中拿，因为WechatTokenResponse中的openid 和 unionid 本来就是基于 WechatTokenResponse 查出来的)
{
  "openid": "...",
  "nickname": "Henry",
  "sex": 1,
  "province": "Guangdong",
  "city": "Shenzhen",
  "country": "CN",
  "headimgurl": "https://...",
  "privilege": [],
  "unionid": "..."
}
 */
