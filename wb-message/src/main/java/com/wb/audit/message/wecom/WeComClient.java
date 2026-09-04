package com.wb.audit.message.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.message.entity.SysConfig;
import com.wb.audit.message.mapper.SysConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 企微（企业微信）客户端：应用消息推送（文本）。
 * 能力范围：仅推送给【个人】与【多人】——touser 传单个 userid 或 "user1|user2"。
 * 配置读取 wb_sys_config：wecom.corpid / wecom.agentid / wecom.secret / wecom.mock
 * （wecom.mock=1 时走本地模拟，不发真实请求；=0 时发真实企微）。
 */
@Component
public class WeComClient {

    /** 获取 access_token */
    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    /** 发送应用消息 */
    private static final String SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send";
    /** access_token 提前 300s 过期，避免边界失效 */
    private static final long TOKEN_EXPIRE_MARGIN_MS = 300 * 1000L;

    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ReentrantLock tokenLock = new ReentrantLock();

    /** access_token 缓存 */
    private volatile String cachedToken;
    private volatile long cachedTokenExpireAt;

    public WeComClient(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    /**
     * 推送文本消息给一个或多个用户。
     *
     * @param userIds 企微 userid 列表（≥1 个；多个用 | 拼接为一次发送）
     * @param content 文本内容
     * @return 发送结果：{msgId, errcode, errmsg, touser, mock}
     */
    public Map<String, Object> sendText(List<String> userIds, String content) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "推送接收人不能为空");
        }
        Map<String, String> cfg = loadConfig();
        String touser = String.join("|", userIds);
        boolean mock = "1".equals(cfg.get("wecom.mock"));

        if (mock) {
            String msgId = "mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.info("[MOCK企微] 模拟发送成功 touser={} content={} msgId={}", touser, content, msgId);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("msgId", msgId);
            r.put("errcode", 0);
            r.put("errmsg", "ok(mock)");
            r.put("touser", touser);
            r.put("mock", true);
            return r;
        }

        String accessToken = getAccessToken(cfg);
        JsonNode resp = doSend(accessToken, cfg, touser, content);
        int errcode = resp.path("errcode").asInt(-1);
        String errmsg = resp.path("errmsg").asText("");

        // token 失效：40014/42001 → 清缓存重试一次
        if (errcode == 40014 || errcode == 42001) {
            log.warn("企微 token 失效({}), 刷新后重试一次", errcode);
            clearToken();
            resp = doSend(getAccessToken(cfg), cfg, touser, content);
            errcode = resp.path("errcode").asInt(-1);
            errmsg = resp.path("errmsg").asText("");
        }
        if (errcode != 0) {
            log.error("企微推送失败 errcode={} errmsg={} touser={}", errcode, errmsg, touser);
            throw new BizException(ResultCode.NOTIFY_ERROR, "企微推送失败: " + errcode + " " + errmsg);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("msgId", resp.path("msgid").asText(""));
        r.put("errcode", errcode);
        r.put("errmsg", errmsg);
        r.put("touser", touser);
        r.put("mock", false);
        return r;
    }

    /** 发送消息（不抛业务异常，返回响应 JSON） */
    private JsonNode doSend(String accessToken, Map<String, String> cfg, String touser, String content) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("touser", touser);
            body.put("msgtype", "text");
            body.put("agentid", Integer.parseInt(cfg.getOrDefault("wecom.agentid", "1000002")));
            Map<String, String> text = new LinkedHashMap<>();
            text.put("content", content);
            body.put("text", text);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(SEND_URL + "?access_token=" + accessToken))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json;charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.info("企微 send 响应: HTTP {} body={}", resp.statusCode(), resp.body());
            return objectMapper.readTree(resp.body());
        } catch (Exception e) {
            throw new BizException(ResultCode.NOTIFY_ERROR, "企微发送请求异常: " + e.getMessage());
        }
    }

    /** 获取 access_token（带缓存与并发锁） */
    public String getAccessToken(Map<String, String> cfg) {
        long now = System.currentTimeMillis();
        if (cachedToken != null && cachedTokenExpireAt > now) {
            return cachedToken;
        }
        tokenLock.lock();
        try {
            if (cachedToken != null && cachedTokenExpireAt > System.currentTimeMillis()) {
                return cachedToken;
            }
            String corpid = cfg.getOrDefault("wecom.corpid", "");
            String secret = cfg.getOrDefault("wecom.secret", "");
            String url = GET_TOKEN_URL + "?corpid=" + encode(corpid) + "&corpsecret=" + encode(secret);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = objectMapper.readTree(resp.body());
            int errcode = json.path("errcode").asInt(0);
            if (errcode != 0 || !json.hasNonNull("access_token")) {
                throw new BizException(ResultCode.NOTIFY_ERROR,
                        "获取企微 access_token 失败: " + errcode + " " + json.path("errmsg").asText(""));
            }
            String token = json.get("access_token").asText();
            long expiresIn = json.path("expires_in").asLong(7200);
            cachedToken = token;
            cachedTokenExpireAt = System.currentTimeMillis() + (expiresIn * 1000L - TOKEN_EXPIRE_MARGIN_MS);
            log.info("企微 access_token 获取成功, 有效期 {}s", expiresIn);
            return token;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.NOTIFY_ERROR, "获取企微 access_token 异常: " + e.getMessage());
        } finally {
            tokenLock.unlock();
        }
    }

    /** 供 NotifyServiceImpl / PushController 直接取配置并获取 token */
    public String getAccessToken() {
        return getAccessToken(loadConfig());
    }

    private void clearToken() {
        cachedToken = null;
        cachedTokenExpireAt = 0L;
    }

    /** 读取企微相关配置（wb_sys_config） */
    public Map<String, String> loadConfig() {
        List<SysConfig> list = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().likeRight(SysConfig::getConfigKey, "wecom."));
        Map<String, String> cfg = new HashMap<>();
        for (SysConfig c : list) {
            cfg.put(c.getConfigKey(), c.getConfigValue());
        }
        return cfg;
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static final Logger log = LoggerFactory.getLogger(WeComClient.class);
}