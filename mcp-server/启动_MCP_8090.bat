@echo off
chcp 65001 >nul
title AuditAgent-MCP-8090
echo 启动 MCP 适配器(HTTP) :8090/mcp ...
E:\py3.10\python.exe "%~dp0wb_audit_mcp.py" --http 8090
pause