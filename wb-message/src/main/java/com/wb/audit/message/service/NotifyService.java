package com.wb.audit.message.service;

import com.wb.audit.message.dto.NotifyDto;

import java.util.Map;

/**
 * 企微通知服务（本期模拟发送：打日志 + 写 notify_log=SENT）
 */
public interface NotifyService {

    Map<String, Object> send(NotifyDto request);
}

