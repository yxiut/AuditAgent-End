package com.wb.audit.controller;

import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.message.wecom.WeComClient;
import com.wb.audit.message.wecom.WeComCrypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企微接收消息服务器（回调）：
 *  GET  /api/wecom/callback —— URL 验证（echostr 解密后原样返回明文）
 *  POST /api/wecom/callback —— 接收消息/事件（解密记录，demo 返回 success）
 * 配置：wb_sys_config 的 wecom.token / wecom.aes_key（与企微后台填写一致）
 */
@RestController
@RequestMapping("/api/wecom")
public class WeComCallbackController {

    private static final Pattern ENCRYPT_PATTERN =
            Pattern.compile("<Encrypt><!\\[CDATA\\[(.*?)\\]\\]></Encrypt>", Pattern.DOTALL);

    private final WeComClient weComClient;

    /**
     * URL 验证：msg_signature 校验 → 解密 echostr → 1 秒内原样返回明文（不带引号/BOM/换行）
     */
    @GetMapping("/callback")
    public String verify(@RequestParam("msg_signature") String msgSignature,
                         @RequestParam String timestamp,
                         @RequestParam String nonce,
                         @RequestParam String echostr) throws Exception {
        Map<String, String> cfg = weComClient.loadConfig();
        String token = cfg.getOrDefault("wecom.token", "");
        String aesKey = cfg.getOrDefault("wecom.aes_key", "");
        if (!WeComCrypto.verifySignature(token, timestamp, nonce, echostr, msgSignature)) {
            log.warn("企微回调 URL 验证签名失败 timestamp={}", timestamp);
            throw new BizException(ResultCode.PARAM_ERROR, "签名校验失败");
        }
        WeComCrypto.DecryptResult r = WeComCrypto.decrypt(aesKey, echostr);
        log.info("企微回调 URL 验证成功 receiveId={}", r.getReceiveId());
        return r.getMessage();
    }

    /**
     * 接收消息/事件：解析 Encrypt → 验签 → 解密记录；demo 不做被动回复，返回 success
     */
    @PostMapping(value = "/callback", produces = "text/plain;charset=utf-8")
    public String receive(@RequestParam("msg_signature") String msgSignature,
                          @RequestParam String timestamp,
                          @RequestParam String nonce,
                          @RequestBody String body) throws Exception {
        Map<String, String> cfg = weComClient.loadConfig();
        String token = cfg.getOrDefault("wecom.token", "");
        String aesKey = cfg.getOrDefault("wecom.aes_key", "");
        Matcher m = ENCRYPT_PATTERN.matcher(body);
        if (!m.find()) {
            throw new BizException(ResultCode.PARAM_ERROR, "回调包缺少 Encrypt 节点");
        }
        String encrypt = m.group(1);
        if (!WeComCrypto.verifySignature(token, timestamp, nonce, encrypt, msgSignature)) {
            log.warn("企微回调消息签名失败 timestamp={}", timestamp);
            throw new BizException(ResultCode.PARAM_ERROR, "签名校验失败");
        }
        WeComCrypto.DecryptResult r = WeComCrypto.decrypt(aesKey, encrypt);
        log.info("收到企微回调消息 receiveId={} msg={}", r.getReceiveId(), r.getMessage());
        return "success";
    }

    private static final Logger log = LoggerFactory.getLogger(WeComCallbackController.class);

    public WeComCallbackController(WeComClient weComClient) {
        this.weComClient = weComClient;
    }
}