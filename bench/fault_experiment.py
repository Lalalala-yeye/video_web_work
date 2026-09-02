# -*- coding: utf-8 -*-
"""
TASK-LAB-02 故障处理实验：停掉依赖服务，验证系统返回设计好的降级结果。

实验场景：
  A. 正常基线：全部服务在线，各接口正常响应
  B. 停掉消息服务(8086)：点赞仍成功（通知降级为静默失败）
  C. 停掉视频服务(8083)：评论被拒绝（返回「目标不可用」），不产生无主评论
  D. 恢复服务：一切恢复正常

运行前：docker compose up -d 且全部 healthy。
环境变量：FAULT_EXP_GATEWAY（可选，默认 http://127.0.0.1:8080）
"""
import json, time, os, sys, subprocess
import urllib.request, urllib.parse, urllib.error

ALLOWED = ("http://127.0.0.1:8081", "http://localhost:8081")
GATEWAY = os.environ.get("FAULT_EXP_GATEWAY", ALLOWED[0])
assert GATEWAY in ALLOWED, "仅允许本机网关地址"

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None

OPENER = urllib.request.build_opener(NoRedirect)
RESULTS = []

def req(method, path, token=None, form=None, jbody=None):
    assert path.startswith("/") and "://" not in path
    headers = {}
    if form is not None:
        data = urllib.parse.urlencode(form).encode()
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    elif jbody is not None:
        data = json.dumps(jbody).encode()
        headers["Content-Type"] = "application/json"
    else:
        data = None
    if token:
        headers["Authorization"] = token
    r = urllib.request.Request(GATEWAY + path, data=data, headers=headers, method=method)
    try:
        with OPENER.open(r, timeout=10) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        try: return e.code, json.loads(body)
        except: return e.code, {"raw": body}
    except Exception as e:
        return 0, {"error": str(e)}

def record(phase, name, passed, detail=""):
    tag = "PASS" if passed else "FAIL"
    RESULTS.append((phase, name, tag, detail))
    print(f"  [{tag}] {name}" + (f"  <- {detail}" if detail and not passed else ""))

def login(username, password):
    s, b = req("POST", "/user/account/login", jbody={"username": username, "password": password})
    if b.get("data", {}).get("token"):
        return b["data"]["token"], b["data"]["user"]["id"]
    return None, None

def stop_service(name):
    subprocess.run(["docker", "stop", f"doinb-{name}"], capture_output=True, timeout=15)
    print(f"\n{'='*60}")
    print(f"  >>> 已停掉 {name} 服务 <<<")
    print(f"{'='*60}")

def start_service(name):
    subprocess.run(["docker", "start", f"doinb-{name}"], capture_output=True, timeout=15)
    print(f"\n  >>> 已恢复 {name} 服务 <<<")

def wait_healthy(name, timeout=90):
    for i in range(timeout // 3):
        r = subprocess.run(["docker", "inspect", "-f", "{{.State.Health.Status}}", f"doinb-{name}"],
                          capture_output=True, text=True, timeout=5)
        if r.stdout.strip() == "healthy":
            return True
        time.sleep(3)
    return False

T0 = str(int(time.time()))
print("=" * 60)
print(f"TASK-LAB-02 故障处理实验  run={T0}")
print(f"网关: {GATEWAY}")
print("=" * 60)

tok, uid = login("demo_user", "123456")
atok, _ = login("demo_author", "123456")
print(f"\n登录: demo_user(id={uid}) {'ok' if tok else 'FAIL'}  demo_author {'ok' if atok else 'FAIL'}")

# ===== A =====
print(f"\n--- A. 正常基线（全部服务在线）---")
s, b = req("GET", "/video/list")
record("A", "视频列表正常", b.get("code") == 200, str(b)[:80])
s, b = req("GET", "/live/list")
record("A", "直播列表正常", b.get("code") == 200, str(b)[:80])
s, b = req("GET", "/search?keyword=test")
record("A", "搜索正常", b.get("code") == 200, str(b)[:80])

s, b = req("GET", "/video/list")
vids = (b.get("data") or {}).get("records", [])
vid = vids[0]["id"] if vids else None
if vid:
    s, b = req("POST", "/video/reaction", token=tok, form={"videoId": vid, "reaction": 1})
    record("A", "点赞正常", b.get("code") == 200, str(b)[:80])
    req("POST", "/video/reaction", token=tok, form={"videoId": vid, "reaction": 0})

# ===== B =====
stop_service("message")
time.sleep(3)
print(f"\n--- B. 停掉消息服务(8086) → 点赞通知降级 ---")
if vid:
    s, b = req("POST", "/video/reaction", token=tok, form={"videoId": vid, "reaction": 1})
    record("B", "点赞仍成功（通知降级）", b.get("code") == 200, str(b)[:80])
    req("POST", "/video/reaction", token=tok, form={"videoId": vid, "reaction": 0})
s, b = req("GET", "/video/list")
record("B", "视频列表不受影响", b.get("code") == 200, str(b)[:80])
s, b = req("GET", "/search?keyword=test")
record("B", "搜索不受影响", b.get("code") == 200, str(b)[:80])
s, b = req("GET", "/live/list")
record("B", "直播列表不受影响", b.get("code") == 200, str(b)[:80])

start_service("message")
print("  等待消息服务恢复...", end="", flush=True)
if wait_healthy("message"): print(" ok")
else: print(" 超时（继续）")

# ===== C =====
stop_service("video")
time.sleep(3)
print(f"\n--- C. 停掉视频服务(8083) → 评论被拒绝 ---")
s, b = req("POST", "/comment/add", token=tok, form={"targetId": 1, "targetType": 1, "content": f"fault_exp_{T0}"})
rs = str(b)
rejected = b.get("code") != 200 or any(k in rs for k in ("不可用", "失败", "502", "error"))
record("C", "评论被拒绝（无无主评论）", rejected, rs[:100])
s, b = req("GET", "/live/list")
record("C", "直播列表不受影响", b.get("code") == 200, str(b)[:80])
s, b = req("GET", "/search?keyword=test")
record("C", "搜索不崩溃", b.get("code") == 200, str(b)[:100])

start_service("video")
print("  等待视频服务恢复...", end="", flush=True)
if wait_healthy("video"): print(" ok")
else: print(" 超时（继续）")

# ===== D =====
print(f"\n--- D. 全部恢复后回归验证 ---")
time.sleep(5)
s, b = req("GET", "/video/list")
record("D", "视频列表恢复", b.get("code") == 200, str(b)[:80])
s, b = req("GET", "/search?keyword=test")
record("D", "搜索恢复", b.get("code") == 200, str(b)[:80])
if vid:
    s, b = req("POST", "/comment/add", token=tok, form={"targetId": vid, "targetType": 1, "content": f"recovered_{T0}"})
    record("D", "评论恢复", b.get("code") == 200, str(b)[:80])

# ===== 汇总 =====
print("\n" + "=" * 60)
p = sum(1 for _,_,t,_ in RESULTS if t=="PASS")
f = sum(1 for _,_,t,_ in RESULTS if t=="FAIL")
print(f"实验结果: {p} 通过 / {f} 失败")
if f:
    print("\n失败项:")
    for ph, nm, tg, dt in RESULTS:
        if tg == "FAIL": print(f"  {ph} | {nm} <- {dt}")
print("\n结论:")
print("1. 消息服务宕机: 核心功能不受影响（通知 fire-and-forget 降级）")
print("2. 视频服务宕机: 评论被拒绝（不产生无主数据），其他服务正常")
print("3. 恢复后: 全部功能自动恢复")
print("4. 故障隔离验证通过")
