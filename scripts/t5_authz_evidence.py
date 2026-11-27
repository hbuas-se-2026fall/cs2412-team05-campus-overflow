#!/usr/bin/env python3
"""T5 接口级越权响应码取证：在编排冒烟同源的全栈环境上，用真实 HTTP 请求
验证「学生 / 教师(课管) / 管理员」多角色在边界接口上的实际响应码。

只读代码核对不算证据，本脚本产生的每一行都来自真实请求与真实响应。

判定口径（2026-11-26 评审后收紧）：
  * 每个用例都写明**期望 HTTP 状态码**与**期望业务码**，实际值逐项比对；
  * 越权用例既不能 2xx（越权成功），也不能是 404（接口根本不存在，用例无效），更不能 5xx；
  * 正向对照同样必须命中期望值——只记录不判定，等于没测；
  * 任何一项不符即 FAIL，脚本退出码非 0，工作流转红。
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
PASSWORD = "Campus2026"  # demo profile 固定口令，非生产凭据
RUN_URL = f"https://github.com/{os.environ.get('GITHUB_REPOSITORY', '')}/actions/runs/{os.environ.get('GITHUB_RUN_ID', '')}"
SHA = os.environ.get("GITHUB_SHA", "")

DENY = "越权拒绝"
ALLOW = "正向对照"


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


def judge(case, status, code):
    """返回 (判定文本, 失败原因或 None)。逐项比对期望，不做「不是 2xx 就算过」的宽松判定。"""
    expect_status, expect_code = case["expect_status"], case["expect_code"]
    if status >= 500:
        return "FAIL：服务端错误", f"{case['id']} {case['boundary']} 返回 {status}（应受控拒绝或正常返回，不是 5xx）"
    if status == 404:
        return "FAIL：接口不存在", f"{case['id']} {case['boundary']} 返回 404（路径不存在，用例本身无效）"
    if status != expect_status:
        return (f"FAIL：期望 {expect_status}，实际 {status}",
                f"{case['id']} {case['boundary']} 期望 HTTP {expect_status}，实际 {status} {code}")
    if expect_code and code != expect_code:
        return (f"FAIL：期望业务码 {expect_code}，实际 {code}",
                f"{case['id']} {case['boundary']} 期望业务码 {expect_code}，实际 {code}")
    if case["kind"] == DENY and 200 <= status < 300:
        return "FAIL：越权请求成功", f"{case['id']} {case['boundary']} 越权请求返回 {status}"
    return "PASS", None


def main():
    os.makedirs("evidence", exist_ok=True)
    os.makedirs("docs/verification", exist_ok=True)

    for _ in range(60):
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
    anon.call("GET", "/api/v1/auth/csrf")  # 先把 CSRF 令牌拿在手上，0b 才是检认证层而不是检 CSRF 层
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
    # expect_* 取自本脚本对 dev,demo 实例的实测基线；未命中即 FAIL。
    cases = [
        dict(id="0a", boundary="未登录·受保护读接口", client=anon, method="GET",
             path="/api/v1/notifications", body=None, kind=DENY,
             expect_status=401, expect_code="AUTH-401",
             note="无会话读通知，预期 401 AUTH-401"),
        dict(id="0b", boundary="未登录·带 CSRF 令牌的写接口", client=anon, method="PUT",
             path="/api/v1/answers/1", body={"body": "x"}, kind=DENY,
             expect_status=401, expect_code="AUTH-401",
             note="已持有 XSRF-TOKEN，越过 CSRF 层后由认证层拒绝（401），证明 CSRF 校验没有替认证兜底"),
        dict(id="1a", boundary="采纳·学生越权", client=gang, method="PUT",
             path="/api/v1/questions/1/accepted-answer", body={"answerId": 1}, kind=DENY,
             expect_status=403, expect_code="QA-3001",
             note="小刚非提问者、非课管，预期 403 QA-3001"),
        dict(id="1b", boundary="采纳·提问者重复采纳", client=ming, method="PUT",
             path="/api/v1/questions/1/accepted-answer", body={"answerId": 1}, kind=ALLOW,
             expect_status=409, expect_code="QA-2002",
             note="小明已采纳过该回答，重复采纳应被业务规则拒绝而非静默成功"),
        dict(id="2a", boundary="编辑回答·非作者", client=gang, method="PUT",
             path="/api/v1/answers/1", body={"body": "越权编辑，不应生效"}, kind=DENY,
             expect_status=403, expect_code="QA-3001",
             note="小刚非作者、非课管，预期 403"),
        dict(id="2b", boundary="编辑回答·作者本人", client=hong, method="PUT",
             path="/api/v1/answers/1",
             body={"body": "关键在于**黑高**这个不变量：叔红变色、叔黑旋转。\n\n（T5 接口级验收时的作者编辑，CI 一次性环境）"},
             kind=ALLOW, expect_status=204, expect_code=None,
             note="作者本人编辑，预期 204 无正文"),
        dict(id="3a", boundary="教师认证·学生越权", client=ming, method="POST",
             path="/api/v1/answers/1/endorsement", body=None, kind=DENY,
             expect_status=403, expect_code="QA-3001",
             note="学生调用课管专属认证，预期 403"),
        dict(id="3b", boundary="教师认证·课管重复认证", client=teacher, method="POST",
             path="/api/v1/answers/1/endorsement", body=None, kind=ALLOW,
             expect_status=409, expect_code="QA-2002",
             note="demo 数据已认证过 a1，重复认证是业务冲突而不是静默成功"),
        dict(id="3c", boundary="教师认证·助教重复认证", client=ta, method="POST",
             path="/api/v1/answers/1/endorsement", body=None, kind=ALLOW,
             expect_status=409, expect_code="QA-2002",
             note="助教同属课程管理侧，应走到同一业务规则而非 403"),
        dict(id="4a", boundary="投票·不可自投", client=hong, method="PUT",
             path="/api/v1/votes", body={"targetType": "ANSWER", "targetId": 1, "value": 1}, kind=DENY,
             expect_status=422, expect_code="QA-1015",
             note="小红投自己的回答 a1，预期 422 QA-1015"),
        dict(id="4b", boundary="投票·重复同值", client=gang, method="PUT",
             path="/api/v1/votes", body={"targetType": "ANSWER", "targetId": 1, "value": 1}, kind=ALLOW,
             expect_status=200, expect_code=None,
             note="小刚 demo 中已投 a1=1，幂等 PUT 预期 200"),
        dict(id="5a", boundary="管理员对账·学生", client=gang, method="GET",
             path="/api/v1/admin/reputation/reconciliation", body=None, kind=DENY,
             expect_status=403, expect_code="AUTH-403",
             note="URL 级 hasRole(ADMIN)，学生预期 403 AUTH-403"),
        dict(id="5b", boundary="管理员对账·教师", client=teacher, method="GET",
             path="/api/v1/admin/reputation/reconciliation", body=None, kind=DENY,
             expect_status=403, expect_code="AUTH-403",
             note="教师同样非 ADMIN，预期 403 AUTH-403"),
        dict(id="5c", boundary="管理员对账·管理员", client=admin, method="GET",
             path="/api/v1/admin/reputation/reconciliation", body=None, kind=ALLOW,
             expect_status=200, expect_code=None,
             note="ADMIN 本人，预期 200"),
        dict(id="6a", boundary="审核选课·学生越权", client=gang, method="PUT",
             path="/api/v1/courses/1/applications/4", body={"approve": True}, kind=DENY,
             expect_status=403, expect_code="ID-3004",
             note="仅课程教师/助教/管理员可审核，预期 403 ID-3004"),
        dict(id="6b", boundary="调整课程角色·学生越权", client=gang, method="PUT",
             path="/api/v1/courses/1/members/4", body={"role": "TA"}, kind=DENY,
             expect_status=403, expect_code="ID-3003",
             note="仅课程教师或管理员可调整，预期 403 ID-3003"),
    ]

    rows, failures = [], []
    for case in cases:
        status, text = case["client"].call(case["method"], case["path"], case["body"])
        code = biz_code(text)
        verdict, failure = judge(case, status, code)
        if failure:
            failures.append(failure)
        expect = f"{case['expect_status']}" + (f" / {case['expect_code']}" if case["expect_code"] else "")
        rows.append({
            "id": case["id"], "boundary": case["boundary"], "kind": case["kind"],
            "actor": case["client"].name, "request": f"{case['method']} {case['path']}",
            "expect": expect, "status": status, "code": code,
            "verdict": verdict, "note": case["note"],
        })
        print(f"  {case['id']} {case['boundary']}: 期望 {expect} / 实际 HTTP {status} {code} -> {verdict}")

    with open("evidence/t5-results.json", "w", encoding="utf-8") as f:
        json.dump({"run_url": RUN_URL, "sha": SHA, "cases": rows}, f, ensure_ascii=False, indent=2)

    lines = [
        "# T5 接口级越权响应码证据（自动采集，真实 HTTP 请求）",
        "",
        f"> 由 `.github/workflows/t5-authz-evidence.yml` 生成，运行 [{os.environ.get('GITHUB_RUN_ID', '')}]({RUN_URL})。",
        f"> 提交：`{SHA}`；环境：GitHub runner 上 `deploy/docker-compose.yml` 全栈（demo 数据）。",
        "> 每条记录 = 一次真实请求的实际响应；原始数据见 artifact `t5-authz-evidence` 的 `t5-results.json`。",
        "> 判定口径：实际响应必须同时命中**期望 HTTP 状态码**与**期望业务码**；404（接口不存在）与 5xx 一律判失败。",
        "",
        "| # | 类型 | 边界 | 角色 | 请求 | 期望 | 实际 HTTP | 业务码 | 判定 | 说明 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for r in rows:
        lines.append(f"| {r['id']} | {r['kind']} | {r['boundary']} | {r['actor']} | `{r['request']}` | "
                     f"`{r['expect']}` | {r['status']} | `{r['code']}` | {r['verdict']} | {r['note']} |")
    deny = sum(1 for r in rows if r["kind"] == DENY)
    allow = len(rows) - deny
    lines += ["", "## 结论", ""]
    if failures:
        lines += [f"**存在 {len(failures)} 项未通过：**"] + [f"- {x}" for x in failures]
    else:
        lines += [f"{deny} 条越权用例全部按预期被拒绝（命中期望状态码与业务码），"
                  f"{allow} 条正向对照全部命中期望响应，无 404 无效路径、无 5xx。"]
    with open("docs/verification/qa-t5-interface-authz-latest.md", "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

    if failures:
        sys.exit(1)
    print("T5 接口级取证完成：越权用例与正向对照全部命中期望响应。")


if __name__ == "__main__":
    main()
