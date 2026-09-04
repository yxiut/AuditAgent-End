#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""gen_fanwei_peizhi.py — 生成空「审核范围配置.xlsx」（无历史/无匹配时客户端下发）

入参(JSON, argv[1] 或 stdin): {
  "level": "company|factory|workshop",
  "slots": {"factory": "龙兴工厂", "region": "零部件"},   // 抽槽结果，可为空
  "clauseTree": [ {"region":"零部件","projects":[{"project":"质量数据采集","subElements":[{"subElement":"质量信息传递","clauses":[{"clauseId":"LJ-01","clauseName":"问题传递及时性"}]}]}]} ],
  "out": "审核范围配置.xlsx"                                // 输出路径，可选
}
出参：打印输出文件路径（JSON）
"""
import sys, json, hashlib
import sys
try:
    # Windows 下强制 UTF-8，避免 stdin/stdout 按 GBK 解码导致中文乱码
    sys.stdin.reconfigure(encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass
from openpyxl import Workbook
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.styles import Font, PatternFill, Alignment, Protection
from openpyxl.workbook.defined_name import DefinedName

HEADER_TASK = ["周期粒度", "周期起", "周期止", "制造基地"]
HEADER_DETAIL = ["区域", "项目", "子要素", "条款名称", "分派人", "条款实例ID"]


def safe_key(s):
    """生成 Excel 命名区域安全内部键（确定性，跨进程稳定；避免括号等特殊字符进命名区域名）"""
    return "k" + hashlib.md5(s.encode("utf-8")).hexdigest()[:10]


def build_dict(clause_tree):
    """返回 (regions, region_keys, project_map, sub_map, clause_map)
    regions: 区域标准名列表
    region_keys: {region: key}
    project_map: {region_key: [(project, key)]}
    sub_map: {project_key: [(subElement, key)]}
    clause_map: {sub_key: [(clauseId, clauseName)]}
    """
    regions = []
    region_keys = {}
    project_map = {}
    sub_map = {}
    clause_map = {}
    for r in clause_tree:
        regions.append(r["region"])
        rk = safe_key(r["region"])
        region_keys[r["region"]] = rk
        project_map[rk] = []
        for p in r.get("projects", []):
            pk = safe_key(r["region"] + p["project"])
            project_map[rk].append((p["project"], pk))
            sub_map[pk] = []
            for s in p.get("subElements", []):
                sk = safe_key(r["region"] + p["project"] + s["subElement"])
                sub_map[pk].append((s["subElement"], sk))
                clause_map[sk] = []
                for c in s.get("clauses", []):
                    clause_map[sk].append((c["clauseId"], c["clauseName"]))
    return regions, region_keys, project_map, sub_map, clause_map


def main():
    raw = sys.argv[1] if len(sys.argv) > 1 else sys.stdin.read()
    data = json.loads(raw) if raw.strip() else {}
    slots = data.get("slots", {}) or {}
    clause_tree = data.get("clauseTree", [])
    out = data.get("out") or "审核范围配置.xlsx"

    regions, region_keys, project_map, sub_map, clause_map = build_dict(clause_tree)
    # 抽槽区域收窄首级下拉
    if slots.get("region"):
        regions = [r for r in regions if r == slots["region"]] or regions

    wb = Workbook()
    ws = wb.active
    ws.title = "审核范围配置"

    ws.append(HEADER_TASK)
    ws.append(["", "", "", ""])
    ws.append([])
    ws.append(HEADER_DETAIL)
    for _ in range(10):
        ws.append(["", "", "", "", "", ""])

    # 表头样式
    for cell in ws[1] + ws[4]:
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = PatternFill("solid", fgColor="305496")
        cell.alignment = Alignment(horizontal="center")
    # F=条款实例ID（可见·锁定只读·四元组VLOOKUP自动带出），G/H/I=内部键列，隐藏
    for col in ("G", "H", "I", "J"):
        ws.column_dimensions[col].hidden = True

    # ---- 级联字典 sheet（隐藏）----
    d = wb.create_sheet("级联字典")
    d.sheet_state = "hidden"
    d.append(["KEY", "区域", "项目", "子要素", "条款ID", "条款名", "条款名键", "四元组", "条款ID4", "区域|项目", "项目|子要素"])
    region_key_row = {}   # key -> row
    project_key_row = {}
    sub_key_row = {}
    r = 2
    for region in regions:
        rk = region_keys[region]
        region_key_row[rk] = r
        d.cell(r, 1, rk)
        d.cell(r, 2, region)
        r += 1
        for project, pk in project_map[rk]:
            project_key_row[pk] = r
            d.cell(r, 1, pk)
            d.cell(r, 3, project)
            d.cell(r, 10, rk + "|" + project)
            r += 1
            for sub, sk in sub_map[pk]:
                sub_key_row[sk] = r
                d.cell(r, 1, sk)
                d.cell(r, 4, sub)
                d.cell(r, 11, pk + "|" + sub)
                r += 1
                for cid, cname in clause_map[sk]:
                    d.cell(r, 1, "c_" + cid)
                    d.cell(r, 5, cid)
                    d.cell(r, 6, cname)
                    d.cell(r, 7, sk + "|" + cname)   # 子键|条款名（兼容旧F公式）
                    d.cell(r, 8, f"{region}|{project}|{sub}|{cname}")  # 四元组键
                    d.cell(r, 9, cid)                # 四元组→条款ID
                    r += 1
    last = r - 1

    # 命名区域
    def add_name(name, ref):
        wb.defined_names[name] = DefinedName(name, attr_text=ref)

    if regions:
        add_name("regions", f"级联字典!$B$2:$B${region_key_row[region_keys[regions[-1]]]}")
    for rk, row in region_key_row.items():
        proj_rows = project_map[rk]
        if proj_rows:
            add_name("projects_" + rk, f"级联字典!$C${project_key_row[proj_rows[0][1]]}:$C${project_key_row[proj_rows[-1][1]]}")
    for pk, row in project_key_row.items():
        subs = sub_map[pk]
        if subs:
            add_name("subs_" + pk, f"级联字典!$D${sub_key_row[subs[0][1]]}:$D${sub_key_row[subs[-1][1]]}")
    for sk, row in sub_key_row.items():
        cls = clause_map[sk]
        if cls:
            add_name("clauses_" + sk, f"级联字典!$F${row}:$F${row + len(cls) - 1}")

    # ---- 明细行：级联下拉 + 隐藏键列公式 + F 自动带出 ID；A~E 全部可编辑 ----
    for row in range(5, ws.max_row + 1):
        A, B, C, D, E, F = (f"{c}{row}" for c in "ABCDEF")
        G, H, I = f"G{row}", f"H{row}", f"I{row}"
        # 隐藏键列公式：G=区域键, H=项目键, I=子要素键
        ws[G] = f'=IF({A}="","",INDEX(级联字典!$A$2:$A${last},MATCH({A},级联字典!$B$2:$B${last},0)))'
        ws[H] = f'=IF(OR({G}="",{B}=""),"",INDEX(级联字典!$A$2:$A${last},MATCH({G}&"|"&{B},级联字典!$J$2:$J${last},0)))'
        ws[I] = f'=IF(OR({H}="",{C}=""),"",INDEX(级联字典!$A$2:$A${last},MATCH({H}&"|"&{C},级联字典!$K$2:$K${last},0)))'
        # F=条款实例ID：按 子要素键|条款名 反查（消除同名条款跨区域歧义）
        ws[F] = f'=IF({D}="","",IFERROR(VLOOKUP({A}&"|"&{B}&"|"&{C}&"|"&{D},级联字典!$H$2:$I${last},2,0),""))'
        ws[F].protection = Protection(locked=True)
        dvA = DataValidation(type="list", formula1="=regions", allow_blank=True, showDropDown=False)
        dvB = DataValidation(type="list", formula1=f'=INDIRECT("projects_"&{G})', allow_blank=True, showDropDown=False)
        dvC = DataValidation(type="list", formula1=f'=INDIRECT("subs_"&{H})', allow_blank=True, showDropDown=False)
        dvD = DataValidation(type="list", formula1=f'=INDIRECT("clauses_"&{I})', allow_blank=True, showDropDown=False)
        for dv, col in ((dvA, A), (dvB, B), (dvC, C), (dvD, D)):
            ws.add_data_validation(dv)
            dv.add(ws[col])
        # 明细 A~E 可编辑（F 由公式带出，无需手填）
        for col in (A, B, C, D, E):
            ws[col].protection = Protection(locked=False)

    # 任务级填写区 A2:D2（周期粒度/起/止/制造基地）可编辑；表头与隐藏列保持锁定
    for col in ("A2", "B2", "C2", "D2"):
        ws[col].protection = Protection(locked=False)

    # 保护工作表：仅锁表头/隐藏辅助列，所有填写区可编辑
    ws.protection.sheet = True
    ws.protection.password = "wb-audit"
    ws.protection.enable()

    wb.save(out)
    print(json.dumps({"file": out, "baselineId": None, "rows": 10}, ensure_ascii=False))


if __name__ == "__main__":
    main()