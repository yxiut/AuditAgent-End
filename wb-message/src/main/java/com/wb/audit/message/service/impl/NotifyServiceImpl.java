package com.wb.audit.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.message.dto.NotifyDto;
import com.wb.audit.message.entity.NotifyLog;
import com.wb.audit.message.entity.NotifyTemplate;
import com.wb.audit.message.mapper.NotifyLogMapper;
import com.wb.audit.message.mapper.NotifyTemplateMapper;
import com.wb.audit.message.service.NotifyService;
import com.wb.audit.message.wecom.WeComClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 企微通知实现：渲染模板 → 真实企微推送（单人/多人）→ 写 notify_log。
 * 接口签名按 /notify/send；企微推送失败抛异常 → 调用方事务整体回滚。
 */
@Service
public class NotifyServiceImpl implements NotifyService {

    private final NotifyTemplateMapper templateMapper;
    private final NotifyLogMapper logMapper;
    private final WeComClient weComClient;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Map<String, Object> send(NotifyDto request) {
        NotifyTemplate tpl = templateMapper.selectOne(
                new LambdaQueryWrapper<NotifyTemplate>().eq(NotifyTemplate::getCode, request.getTemplateCode()));
        if (tpl == null) {
            throw new BizException(ResultCode.NOT_FOUND, "通知模板不存在: " + request.getTemplateCode());
        }
        Map<String, Object> vars = request.getVars() == null ? Map.of() : request.getVars();
        String content = render(tpl.getContent(), vars);

        // 真实企微推送：单人 or 多人（touser 用 | 拼接，一次发送）
        Map<String, Object> result = weComClient.sendText(request.getToUsers(), content);
        String msgId = (String) result.getOrDefault("msgId", "");
        int errcode = (int) result.getOrDefault("errcode", -1);
        String status = errcode == 0 ? "SENT" : "FAILED";

        List<Map<String, Object>> sent = new ArrayList<>();
        for (String wecomUserid : request.getToUsers()) {
            NotifyLog nl = new NotifyLog();
            nl.setTaskId(request.getTaskId());
            nl.setTemplateId(tpl.getId());
            nl.setWecomUserid(wecomUserid);
            nl.setNotifyType(tpl.getCode());
            nl.setContent(content);
            nl.setMsgId(msgId);
            nl.setChannel("WECOM");
            nl.setStatus(status);
            nl.setSendResult(result.toString());
            nl.setSentAt(LocalDateTime.now());
            logMapper.insert(nl);
            sent.add(Map.of("wecomUserid", wecomUserid, "msgId", msgId, "status", status));
        }
        return Map.of("sent", sent, "wecom", result);
    }

    /** 占位符渲染 {taskNo} → 值 */
    private String render(String content, Map<String, Object> vars) {
        String out = content;
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            String v = e.getValue() == null ? "" : e.getValue().toString();
            out = out.replace("{" + e.getKey() + "}", v);
        }
        return out;
    }

    private static final Logger log = LoggerFactory.getLogger(NotifyServiceImpl.class);

    public NotifyServiceImpl(NotifyTemplateMapper templateMapper, NotifyLogMapper logMapper, WeComClient weComClient) {
        this.templateMapper = templateMapper;
        this.logMapper = logMapper;
        this.weComClient = weComClient;
    }
}