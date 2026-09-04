# ============================================================
# 一键全流程演示：审核助手 DEMO（场景一~四，真实企微推送）
#   张伟发起 → 陈志强提交材料 → AI 写结论 → 张伟查进度
# 用法：powershell -ExecutionPolicy Bypass -File demo_full_flow.ps1 [-Auto] [-SkipAlign]
#   -Auto        不逐段暂停，一次跑完
#   -SkipAlign   不把任务号改成产品常量 AUD-202607-LX-001
# 前置：后端已启动（默认 http://127.0.0.1:8080），wecom.mock=0（真实推送）
# ============================================================
param(
    [string]$Base = "http://127.0.0.1:8080/api",
    [string]$MaterialFile = "E:\ai\workBuddy-audit-agent\shang-chuan-cai-liao-demo_v0.5.0_backup\assets\模拟系统数据\车间过程FTR问题跟踪管理表.xlsx",
    [switch]$Auto,
    [switch]$SkipAlign
)
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Say($m) { Write-Host "`n==> $m" -ForegroundColor Cyan }
function Step($m) { Write-Host "`n---------- $m ----------" -ForegroundColor Yellow }
function PauseStep {
    if (-not $Auto) {
        $k = Read-Host "按回车继续（q 退出）"
        if ($k -eq "q") { Write-Host "已退出"; exit }
    }
}
function Read-OutFile($f) {
    return [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
}
function Invoke-Api {
    param([string]$Method, [string]$Url, $Body, [string[]]$Headers)
    $bodyFile = Join-Path $env:TEMP ("wbdemo_body_" + [guid]::NewGuid().ToString("N") + ".json")
    $outFile  = Join-Path $env:TEMP ("wbdemo_out_" + [guid]::NewGuid().ToString("N") + ".json")
    if ($null -ne $Body) {
        [System.IO.File]::WriteAllText($bodyFile, ($Body | ConvertTo-Json -Depth 20 -Compress), $utf8NoBom)
    }
    $a = @("-s", "-X", $Method, ($Base + $Url), "-H", "Content-Type: application/json", "-o", $outFile)
    if ($Headers) { foreach ($h in $Headers) { $a += "-H"; $a += $h } }
    if ($null -ne $Body) { $a += "-d"; $a += "@" + $bodyFile }
    & curl.exe @a | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "curl 失败: $Url" }
    Remove-Item $bodyFile -ErrorAction SilentlyContinue
    $txt = Read-OutFile $outFile
    Remove-Item $outFile -ErrorAction SilentlyContinue
    if ([string]::IsNullOrWhiteSpace($txt)) { throw "空响应: $Url" }
    return ($txt | ConvertFrom-Json)
}
function Invoke-Upload {
    param([long]$TaskId, [string]$ClauseId, [string]$File)
    $outFile = Join-Path $env:TEMP ("wbdemo_out_" + [guid]::NewGuid().ToString("N") + ".json")
    $a = @("-s", "-X", "POST", ($Base + "/materials/upload"),
           "-H", "X-User-Id: 4",
           "-F", "taskId=$TaskId", "-F", "clauseId=$ClauseId",
           "-F", ("file=@" + $File),
           "-o", $outFile)
    & curl.exe @a | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "上传失败" }
    $txt = Read-OutFile $outFile
    Remove-Item $outFile -ErrorAction SilentlyContinue
    return ($txt | ConvertFrom-Json)
}

Write-Host "审核助手全流程 DEMO（真实企微推送）" -ForegroundColor Green
Write-Host "  账号：张伟(审核员/DuoLeGeDuo) · 陈志强(被审核人/YangXiuTian)"
Write-Host "  条款：HJ-GC-02 指标监控分析落地（过程（焊接）· 龙兴工厂）"

# ---------- 0. 探活 ----------
Step "0. 探活"
$t0 = Invoke-Api "GET" "/audit/tasks" $null @("X-User-Id: 5")
if ($t0.code -ne 200) { throw "后端不可用，请先启动后端"; }
Write-Host ("后端 OK，当前张伟名下任务 " + @($t0.data.tasks).Count + " 个")
PauseStep

# ---------- 1. 张伟发起审核 ----------
Step "1. 张伟(5) 发起审核 → 创建并下发（真实推送陈志强 YangXiuTian）"
$body1 = @{
    period = @{ type = "MONTH"; start = "2026-07-01"; end = "2026-07-31" }
    factoryId = 1
    regions = @(@{ region = "过程（焊接）"; clauses = @(@{ clauseId = "HJ-GC-02"; assigneeId = 4 }) })
    dispatchTree = @(@{ assigneeId = 4; nodeType = "leaf"; clauseIds = @("HJ-GC-02") })
}
$r1 = Invoke-Api "POST" "/tasks/create" $body1 @("X-User-Id: 5")
if ($r1.code -ne 200) { throw ("创建失败: " + $r1.message) }
$taskId = $r1.data.taskId
$taskNo = $r1.data.taskNo
Write-Host ("创建成功 taskId=$taskId taskNo=$taskNo globalState=" + $r1.data.globalState)
Write-Host "陈志强(YangXiuTian) 应收到「材料待提交」真实企微消息"
PauseStep

# ---------- 任务号对齐产品常量 ----------
if (-not $SkipAlign) {
    Step "1b. 任务号对齐产品包（AUD-202607-LX-001）"
    $mysql = "E:\mysql8\bin\mysql.exe"
    if (Test-Path $mysql) {
        $sql = "UPDATE wb_audit_task SET task_no='AUD-202606-LX-000' WHERE id=100 AND task_no='AUD-202607-LX-001'; UPDATE wb_audit_task SET task_no='AUD-202607-LX-001' WHERE id=$taskId AND task_no='$taskNo';"
        & $mysql --host=127.0.0.1 --port=3307 --user=root --password=root wb_audit -e $sql 2>$null
        Write-Host "已将新任务 task_no 更新为 AUD-202607-LX-001（旧任务100让位）"
        $taskNo = "AUD-202607-LX-001"
    } else {
        Write-Host "未找到 mysql 客户端，跳过任务号对齐（当前 taskNo=$taskNo）"
    }
    PauseStep
}

# ---------- 2. 陈志强提交材料 ----------
Step "2. 陈志强(4) 提交材料（上传 FTR 表 + 确认 + 触发 runTask）"
$p = Invoke-Api "GET" ("/materials/tasks/pending?taskId=" + $taskId) $null @("X-User-Id: 4")
if ($p.code -ne 200) { throw ("查待交失败: " + $p.message) }
Write-Host ("待交条款: " + (@($p.data.clauses).Count) + " 条（HJ-GC-02）")
if (-not (Test-Path $MaterialFile)) { throw ("找不到样例文件: " + $MaterialFile) }
$up = Invoke-Upload $taskId "HJ-GC-02" $MaterialFile
if ($up.code -ne 200) { throw ("上传失败: " + $up.message) }
Write-Host ("上传成功 materialId=" + $up.data.materialId + " 命中标签=" + $up.data.classified)
$cf = Invoke-Api "POST" "/materials/tasks/confirm" @{ taskId = $taskId; materialIds = @([int64]$up.data.materialId) } @("X-User-Id: 4")
if ($cf.code -ne 200) { throw ("确认失败: " + $cf.message) }
Write-Host ("确认成功 allCollected=" + $cf.data.allCollected + " 取数行数=" + $cf.data.pulled[0].rows)
$run = Invoke-Api "POST" "/execute/tasks/run" @{ taskId = $taskId } $null
if ($run.code -ne 200) { throw ("runTask 失败: " + $run.message) }
Write-Host ("runTask 受理 runId=" + $run.data.runId + " state=" + $run.data.state + "（真实流程此处由客户端 WorkBuddy 模型跑 AI 审核）")
PauseStep

# ---------- 3. AI 写结论 ----------
Step "3. AI 审核写结论（模拟 runTask 的 writeConclusion，真实推送张伟 DuoLeGeDuo）"
$body3 = @{
    taskId = $taskId
    clauseId = "HJ-GC-02"
    outcome = "scored"
    notes = @()
    issues = @(
        @{
            ruleId = "FTR连续两月低于目标"
            problemDesc = "审核龙兴工厂焊接2026年7月过程FTR连续两个可审月低于目标"
            evidence = "PULL：焊接 2026-06、2026-07 FTR 与目标对照"
            problemType = "执行类"; score = 6
            refMaterials = @("PULL-月度指标")
            suggestJudgment = "不符合"; confidence = "高"
        },
        @{
            ruleId = "问题管理项目与系统TOP3不一致"
            problemDesc = "FTR不达标触发后，问题管理项目与系统TOP3不一致"
            evidence = "系统TOP3 与 FTR管理表项目对照"
            problemType = "标准类"; score = 6
            refMaterials = @("PULL-系统TOP3", "M-FTR")
            suggestJudgment = "不符合"; confidence = "中"
        }
    )
}
$w = Invoke-Api "POST" "/audit/conclusion" $body3 $null
if ($w.code -ne 200) { throw ("写结论失败: " + $w.message) }
Write-Host ("结论已写入 conclusionId=" + $w.data.conclusionId + "，任务 → HUMAN_REVIEW（人工复审中）")
Write-Host "张伟(DuoLeGeDuo) 应收到「AI审核完成·待人工复核」真实企微消息"
PauseStep

# ---------- 4. 张伟查进度 ----------
Step "4. 张伟(5) 查进度（审核监控 · BIP 问题管理表）"
$tl = Invoke-Api "GET" "/audit/tasks" $null @("X-User-Id: 5")
Write-Host "张伟名下任务："
foreach ($x in @($tl.data.tasks)) {
    Write-Host ("   - " + $x.taskNo + " | " + $x.factoryName + " | " + $x.period + " | " + $x.phase)
}
$pr = Invoke-Api "GET" ("/audit/progress?taskId=" + $taskId) $null @("X-User-Id: 5")
if ($pr.code -ne 200) { throw ("查进度失败: " + $pr.message) }
$d = $pr.data
Write-Host ("`n任务 " + $d.taskNo + " 阶段=" + $d.phase)
foreach ($c in @($d.clauses)) {
    Write-Host ("  条款: " + $c.path)
    Write-Host ("  问题 " + $c.issuesCount + " 条 · 条款分建议 " + $c.suggestedScore + "（" + $c.outcome + "，待人工确认）")
}
Write-Host "`n结果区 BIP 问题管理表（12 列 → 对话区摘要展示关键 4 列）："
foreach ($r in @($d.bipRows)) {
    Write-Host ("   | " + $r."时间" + " | " + $r."制造基地" + " | 序号" + $r."序号" + " | " + $r."区域" + " > " + $r."条款")
    Write-Host ("     " + $r."问题描述" + "（严重度 " + $r."严重度（赋分）" + " · " + $r."问题属性" + "）")
}

Write-Host "`n----------------------------------------" -ForegroundColor Green
Write-Host "全流程跑通 ✅  任务 $taskNo 现处于 HUMAN_REVIEW"
Write-Host "到 WorkBuddy 审核员专家（张伟）说「查进度」即可复现第 4 步对话 + BIP 结果区。"
Write-Host "人工确认可调：POST /api/audit/issues/confirm {taskId, clauseId:'HJ-GC-02', issueIds:[..], confirmed:true}"
Write-Host "----------------------------------------" -ForegroundColor Green