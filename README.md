# AuditAgent-End · 发起审核场景后端

WorkBuddy AI 审核助手「发起审核（fa-qi-shen-he）」后端实现。**目标：流程跑通**。

## 技术栈

- Java 17（本机 `E:\jdk17\jdk-17.0.1`）
- Spring Boot 3.5.16 + MyBatis-Plus 3.5.17 + MySQL 8.0（127.0.0.1:3307 root/root，库 `wb_audit`）；**无 Lombok**（原生 getter/setter + Logger + 构造器，任意 IDE 直接可编译）
- Maven 多 Module：`wb-common / wb-auth / wb-dict / wb-task / wb-audit(预留) / wb-message / wb-app`
- **对外接口（Controller）统一放在 `wb-app` 模块**；领域模块只含 Entity/Mapper/Service
- **Mapper XML 按业务放在各领域模块** `src/main/resources/mapper/`（wb-dict / wb-task / wb-message），`mapper-locations: classpath*:mapper/*.xml` 全量扫描
- **所有表含 `create_time`/`update_time`**（MyBatis-Plus 自动填充：insert 双填、update 填 update_time）
- **表名统一 `wb_` 前缀**（如 `wb_audit_task`），列名保持常规（`task_no`/`region`/`create_time`…）；控制器路径**不含 /v1**（如 `/api/tasks/create`）
- 实体字段均有 Javadoc 注释；方法命名简单直白（createAndDispatch / normalize / send 等）；DTO 命名：请求 `XxxDto`、响应 `XxxVo`

## 快速启动

```powershell
# 1. 建库 + 建表 + 种子（已在 wb_audit 执行过）
mysql -h127.0.0.1 -P3307 -uroot -proot < sql\schema.sql
mysql -h127.0.0.1 -P3307 -uroot -proot < sql\seed.sql

# 2. 打包（需 JDK17；mvnw wrapper 自动下载 Maven 3.9）
$env:JAVA_HOME="E:\jdk17\jdk-17.0.1"
.\mvnw.cmd -DskipTests package

# 3. 启动
java -jar wb-app\target\wb-app-0.0.1-SNAPSHOT.jar
# 服务端口 8080
```

## 身份传递（本期写死用户，不建用户表）

| 方式 | 说明 |
|---|---|
| `X-User-Id: 1/2/3` | 1=张三(审核人·可发布) 2=李四(陪审员) 3=王五(被审核人) —— **联调推荐** |
| `X-User-Name: 张三` | 中文需 URL 编码，如 `%E5%BC%A0%E4%B8%89` |

## 接口（7 个）

| 接口 | 说明 |
|---|---|
| `POST /api/auth/check-publish` | 发布权预检 |
| `POST /api/dict/normalize` | 口语归一（`{"text":"龙兴"}`→龙兴工厂） |
| `GET /api/dict/factories` | 可选工厂 |
| `GET /api/dict/clause-tree` | 四级条款树（区域→项目→子要素→条款） |
| `GET /api/users/search?q=` | 人员搜索（写死候选） |
| `POST /api/tasks/create` | **创建并下发（单事务）**：权限→区域隔离条款校验→nodeType 后端判定→taskNo→落库5表→企微通知聚合→回执；任一失败整体回滚 |
| `POST /api/notify/send` | 企微通用发送（**本期模拟**：日志+notify_log=SENT） |

### /tasks/create 入参示例（区域→条款分组，区域隔离）

```json
{
  "period": { "type": "MONTH", "start": "2026-08-01", "end": "2026-08-31" },
  "factoryId": 1,
  "regions": [
    { "region": "零部件", "clauses": [ { "clauseId": "LJ-01", "assigneeId": 3 } ] },
    { "region": "过程（焊接）", "clauses": [ { "clauseId": "HJ-GC-01", "assigneeId": 2 } ] }
  ],
  "dispatchTree": [
    { "assigneeId": 3, "nodeType": "leaf", "clauseIds": ["LJ-01"] },
    { "assigneeId": 2, "nodeType": "juror", "clauseIds": ["HJ-GC-01"] }
  ]
}
```

curl 示例：

```bash
curl -X POST http://127.0.0.1:8080/api/tasks/create \
  -H "Content-Type: application/json; charset=utf-8" \
  -H "X-User-Id: 1" \
  -d '{"period":{"type":"MONTH","start":"2026-08-01","end":"2026-08-31"},"factoryId":1,"regions":[{"region":"零部件","clauses":[{"clauseId":"LJ-01","assigneeId":3}]}],"dispatchTree":[{"assigneeId":3,"nodeType":"leaf","clauseIds":["LJ-01"]}]}'
```

## 数据表（9 张，见 sql/：schema.sql 建表 / seed.sql 种子 / crud.sql 简单增删改查示例）

`dict_clause_tree` `dict_entity_alias` `audit_task` `task_clause` `task_assignment` `task_node_log` `notify_template` `notify_log` `sys_config`

- 全局态存 `audit_task.global_state`（本期创建即 COLLECTING，无 DRAFT）
- 条款态存 `task_clause.state/material_state/audit_state/review_state`（创建瞬间 TO_COLLECT）
- 流转留痕存 `task_node_log`

## PY 脚本（scripts/，Python3 + openpyxl）

支持 WorkBuddy 客户端生成/解析 Excel：

| 脚本 | 作用 |
|---|---|
| `gen_fanwei_peizhi.py` | 生成空「审核范围配置.xlsx」（四级级联下拉 + 隐藏级联字典） |
| `parse_fanwei_peizhi.py` | 解析配置表 → 周期/基地/条款列表 + 校验问题 |
| `gen_xiugai_moban.py` | 生成「审核修改模板.xlsx」（已有行锁定 + 追加行级联 + 隐藏「基线」sheet，打印 baselineId） |
| `parse_xiugai_moban.py` | 对隐藏「基线」diff → remove/add/assignChanges/invalid/empty |

用法：入参 JSON 走 `argv[1]` 或 stdin，出参 JSON 打印 stdout。

```bash
echo '{"level":"company","slots":{},"clauseTree":[...],"out":"x.xlsx"}' | python scripts/gen_fanwei_peizhi.py
python scripts/parse_fanwei_peizhi.py x.xlsx --tree tree.json
python scripts/parse_xiugai_moban.py tpl.xlsx
```

## 本期约定

- 无历史/复制/基线后端接口（「上次配置」由 WorkBuddy 提示词+会话上下文承担）
- 无用户/组织表（鉴权写死映射；预留后续接真实组织）
- 无草稿态（创建并下发直达 COLLECTING）
- 企微 = 模拟发送（`wecom.mock=1`；替换 NotifyServiceImpl 即可接真实企微）





