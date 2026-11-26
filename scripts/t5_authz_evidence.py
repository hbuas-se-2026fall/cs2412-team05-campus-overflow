#!/usr/bin/env python3
"""T5 接口级越权响应码取证：在编排冒烟同源的全栈环境上，用真实 HTTP 请求
验证「学生 / 教师(课管) / 管理员」多角色在边界接口上的实际响应码。

只读代码核对不算证据，本脚本产生的每一行都来自真实请求与真实响应。
越权用例若返回 2xx 视为安全缺陷（工作流变红）；5xx 同样判失败。
"""
import json
import os
import re
import sys
import time
import urllib.request
import urllib.error
import http.cookiejar

BASE = os.environ.get("T5_BASE_URL", "http://localhost:8080")
PASSWORD = "Campus2026"
RUN_URL = f"https://github.com/{os.environ.get('GITHUB_REPOSITORY','')}/actions/runs/{os.environ.get('GITHUB_RUN_ID','')}"
SHA = os.environ.get("GITHUB_SHA", "")


class Client:
    """一个账号一个会话：SESSION + XSRF-TOKEN 双 Cookie（ADR-003 / QS-07）。"""

    def __init__(self, name):
        self.name = name
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))

    def _xsrf(self):
        for c in self.jar:
            if c.name == "XSRF-TOKEN":
                return c.value
        return None

    def call(self, method, path, body=None):
        headers = {"Accept": "application/json", "User-Agent": "t5-authz-evidence"}
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        xsrf = self._xsrf()
        if xsrf:
            headers["X-XSRF-TOKEN"] = xsrf
        req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
        try:
            with self.opener.open(req, timeout=30) as r:
                return r.status, r.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode("utf-8", "replace")

    def login(self, username):
        self.call("GET", "/api/v1/auth/csrf")
        status, text = self.call("POST", "/api/v1/auth/login",
                                 {"username": username, "password": PASSWORD})
        if status != 200:
            raise RuntimeError(f"{self.name}({username}) 登录失败: {status} {text[:200]}")
        print(f"  登录成功: {self.name}({username})")


def biz_code(text):
    m = re.search(r'"code"\s*:\s*"([^"]+)"', text)
    return m.group(1) if m else "—"


def main():
    os.makedirs("evidence", exist_ok=True)
    os.makedirs("docs/verification", exist_ok=True)

    for i in range(60):
        try:
            with urllib.request.urlopen(BASE + "/actuator/health/readiness", timeout=5) as r:
                if r.status == 200:
                    break
        except Exception:
            pass
        time.sleep(2)
    else:
        print("后端 readiness 超时")
        sys.exit(1)

    anon = Client("未登录")
    gang, hong, ming = Client("小刚/学生"), Client("小红/学生"), Client("小明/学生")
    teacher, ta, admin = Client("王老师/课管"), Client("李助教/助教"), Client("系统管理员")
    gang.login("2023003")
    hong.login("2023002")
    ming.login("2023001")
    teacher.login("T2001")
    ta.login("TA3001")
    admin.login("admin01")

    # demo 数据终态（DemoDataSeeder）：q1=1 小明提问，a1=1 小红回答（已被课管认证、已被小明采纳），
    # 课程 id=1（T2001 为课管，TA3001 为助教），小明 userId=4。
    # (用例编号, 边界, 客户端, 方法, 路径, body, 越权应拒绝, 说明)
    cases = [
        ("0",  "未登录对照", anon, "PUT", "/api/v1/answers/1", {"body": "x"}, True,
         "无会话访问受保护接口，预期 401 AUTH-401"),
        ("1a", "采纳·学生越权", gang, "PUT", "/api/v1/questions/1/accepted-answer", {"answerId": 1}, True,
         "小刚既非提问者也非课管，预期 403"),
        ("1b", "采纳·提问者对照", ming, "PUT", "/api/v1/questions/1/accepted-answer", {"answerId": 1}, False,
         "小明重复采纳同一回答，记录实际行为（幂等或冲突）"),
        ("2a", "编辑回答·非作者", gang, "PUT", "/api/v1/answers/1", {"body": "越权编辑，不应生效"}, True,
         "小刚非作者非课管，预期 403"),
        ("2b", "编辑回答·作者对照", hong, "PUT", "/api/v1/answers/1",
         {"body": "关键在于**黑高**这个不变量：叔红变色、叔黑旋转。\n\n（T5 接口级验收时的作者编辑，CI 一次性环境）"}, False,
         "作者本人编辑，预期 204"),
        ("3a", "教师认证·学生越权", ming, "POST", "/api/v1/answers/1/endorsement", None, True,
         "学生调用课管专属认证，预期 403"),
        ("3b", "教师认证·课管重复", teacher, "POST", "/api/v1/answers/1/endorsement", None, False,
         "demo 数据已认证过 a1，预期 QA-2002 重复认证"),
        ("3c", "教师认证·助教对照", ta, "POST", "/api/v1/answers/1/endorsement", None, False,
         "助教同属课程管理侧，预期同为 QA-2002 而非 403"),
        ("4a", "投票·不可自投", hong, "PUT", "/api/v1/votes", {"targetType": "ANSWER", "targetId": 1, "value": 1}, True,
         "小红投自己的回答 a1，预期 QA-1015"),
        ("4b", "投票·正常幂等对照", gang, "PUT", "/api/v1/votes", {"targetType": "ANSWER", "targetId": 1, "value": 1}, False,
         "小刚 demo 中已投 a1=1，幂等 PUT 预期 200"),
        ("5a", "管理员对账·学生", gang, "GET", "/api/v1/admin/reputation/reconciliation", None, True,
         "URL 级 hasRole(ADMIN)，学生预期 403 AUTH-403"),
        ("5b", "管理员对账·教师", teacher, "GET", "/api/v1/admin/reputation/reconciliation", None, True,
         "教师同样非 ADMIN，预期 403 AUTH-403"),
        ("5c", "管理员对账·管理员", admin, "GET", "/api/v1/admin/reputation/reconciliation", None, False,
         "ADMIN 本人，预期 200"),
        ("6a", "审核选课·学生越权", gang, "PUT", "/api/v1/courses/1/applications/4", {"approve": True}, True,
         "预期 ID-3004 仅课程教师/助教/管理员可审核"),
        ("6b", "调整角色·学生越权", gang, "PUT", "/api/v1/courses/1/members/4", {"role": "TA"}, True,
         "预期 ID-3003 仅课程教师或管理员可调整"),
    ]

    rows, failures = [], []
    for cid, boundary, client, method, path, body, must_deny, note in cases:
        status, text = client.call(method, path, body)
        code = biz_code(text)
        verdict = "记录"
        if must_deny:
            if 200 <= status < 300:
                verdict = "FAIL：越权请求成功"
                failures.append(f"{cid} {boundary} 返回 {status}（越权未被拒绝）")
            elif status >= 500:
                verdict = "FAIL：服务端错误"
                failures.append(f"{cid} {boundary} 返回 {status}（应受控拒绝而非 500）")
            else:
                verdict = "PASS：受控拒绝"
        rows.append({"id": cid, "boundary": boundary, "actor": client.name,
                     "request": f"{method} {path}", "status": status,
                     "code": code, "verdict": verdict, "note": note})
        print(f"  {cid} {boundary}: HTTP {status} {code} -> {verdict}")

    with open("evidence/t5-results.json", "w", encoding="utf-8") as f:
        json.dump({"run_url": RUN_URL, "sha": SHA, "cases": rows}, f, ensure_ascii=False, indent=2)

    lines = [
        "# T5 接口级越权响应码证据（自动采集，真实 HTTP 请求）",
        "",
        f"> 由 `.github/workflows/t5-authz-evidence.yml` 生成，运行 [{os.environ.get('GITHUB_RUN_ID','')}]({RUN_URL})。",
        f"> 提交：`{SHA}`；环境：GitHub runner 上 `deploy/docker-compose.yml` 全栈（demo 数据）。",
        "> 每条记录 = 一次真实请求的实际响应；原始数据见 artifact `t5-authz-evidence` 的 `t5-results.json`。",
        "",
        "| # | 边界 | 角色 | 请求 | HTTP | 业务码 | 判定 | 说明 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for r in rows:
        lines.append(f"| {r['id']} | {r['boundary']} | {r['actor']} | `{r['request']}` | {r['status']} | `{r['code']}` | {r['verdict']} | {r['note']} |")
    lines += ["", "## 结论", ""]
    if failures:
        lines += ["**存在未通过项：**"] + [f"- {x}" for x in failures]
    else:
        lines += ["全部越权用例均被受控拒绝（非 2xx、非 5xx），响应码与业务码见上表。"]
    with open("docs/verification/qa-t5-interface-authz-latest.md", "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

    if failures:
        sys.exit(1)
    print("T5 接口级取证完成，全部越权用例受控拒绝。")


if __name__ == "__main__":
    main()