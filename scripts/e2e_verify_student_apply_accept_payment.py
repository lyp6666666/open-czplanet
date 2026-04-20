import json
import os
import random
import re
import subprocess
import time
from pathlib import Path

from playwright.sync_api import sync_playwright


ROOT = Path(__file__).resolve().parents[1]
BASE_API = os.environ.get("E2E_API_BASE", "http://localhost:18080").rstrip("/")
BASE_WEB = os.environ.get("E2E_WEB_BASE", "http://localhost:5173").rstrip("/")
REMOTE_APP = "root@111.228.20.88"
REMOTE_LOG = "/opt/ai-platform/.logs/tutor-appointment-service.log"


def run(cmd: str, cwd: Path | None = None) -> str:
    return subprocess.check_output(cmd, shell=True, cwd=str(cwd or ROOT), text=True)


def curl_json(method: str, path: str, token: str | None = None, body: dict | None = None, query: dict | None = None):
    url = f"{BASE_API}{path}"
    if query:
        from urllib.parse import urlencode
        url = f"{url}?{urlencode(query)}"
    headers = ["-H", "Content-Type: application/json"]
    if token:
        headers += ["-H", f"Authorization: Bearer {token}"]
    data_part = []
    if body is not None:
        data_part = ["--data-raw", json.dumps(body, ensure_ascii=False)]
    cmd = ["curl", "-sS", "-X", method, url, *headers, *data_part]
    out = subprocess.check_output(cmd, text=True)
    res = json.loads(out)
    if res.get("code") != 0:
        raise RuntimeError(f"API failed {path}: {res}")
    return res["data"]


def rand_phone(prefix: str) -> str:
    return f"{prefix}{random.randint(0, 99999999):08d}"[:11]


def last_sms_code(phone: str) -> str:
    txt = run(f"ssh {REMOTE_APP} \"tail -n 800 {REMOTE_LOG}\"")
    for line in reversed(txt.splitlines()):
      if phone not in line:
        continue
      match = re.search(r"code[:=]\\s*(\\d{4,6})", line)
      if match:
        return match.group(1)
    raise RuntimeError(f"SMS code not found for {phone}")


def login(role: str, phone: str) -> dict:
    curl_json("POST", "/user/sendcode", body={"phone": phone})
    time.sleep(0.5)
    code = last_sms_code(phone)
    return curl_json("POST", "/user/loginOrRegister", body={"phone": phone, "code": code, "userRoleEnum": role})


def prepare_flow() -> dict:
    student = login("STUDENT", rand_phone("186"))
    teacher = login("TEACHER", rand_phone("188"))

    student_token = student["token"]
    teacher_token = teacher["token"]
    student_uid = student["id"]
    teacher_uid = teacher["id"]

    subject_tree = curl_json("GET", "/api/v1/public/subjects/tree")
    subject_id = 200
    stack = list(subject_tree or [])
    while stack:
        node = stack.pop(0)
        children = node.get("children") or []
        if not children and isinstance(node.get("id"), int):
            subject_id = node["id"]
            break
        stack = list(children) + stack

    demand_id = curl_json(
        "POST",
        "/api/v1/parent/jobs",
        token=student_token,
        body={
            "subjectId": subject_id,
            "title": "E2E 测试需求",
            "description": "验证学生发起家教申请后教师首次通过与支付提示链路。",
            "classMode": "online",
            "frequencyPerWeek": 2,
            "stageCode": "PRIMARY",
            "educationRequirement": "UNLIMITED",
            "publisherIdentity": "PARENT",
        },
    )

    room_id = curl_json("POST", "/chat/room", token=student_token, body={"targetUid": teacher_uid})
    start_msg = curl_json(
        "POST",
        "/chat/application/start-chat",
        token=student_token,
        body={
            "receiverUid": teacher_uid,
            "contextType": "TUTOR",
            "contextId": teacher_uid,
            "content": "您好，我想申请长期补课，方便沟通吗？",
            "teachingMode": "ONLINE",
            "clientRequestId": f"e2e-student-apply-{int(time.time())}",
        },
    )
    application_id = ((start_msg or {}).get("message") or {}).get("body", {}).get("applicationId")
    if not application_id:
        raise RuntimeError(f"application id missing: {start_msg}")

    return {
        "student": student,
        "teacher": teacher,
        "student_token": student_token,
        "teacher_token": teacher_token,
        "student_uid": student_uid,
        "teacher_uid": teacher_uid,
        "room_id": room_id,
        "application_id": application_id,
        "demand_id": demand_id,
    }


def verify_browser(flow: dict) -> dict:
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="chrome", headless=True)
        page = browser.new_page()
        page.add_init_script(
            """(payload) => {
              localStorage.setItem('ai_tutor_token', payload.token);
              localStorage.setItem('ai_tutor_user', JSON.stringify(payload.user));
            }""",
            {
                "token": flow["teacher_token"],
                "user": {
                    "id": flow["teacher"]["id"],
                    "name": flow["teacher"].get("name") or f"用户{flow['teacher']['id']}",
                    "phone": flow["teacher"]["phone"],
                    "avatar": flow["teacher"].get("avatar") or "",
                    "sex": flow["teacher"].get("sex"),
                    "userType": flow["teacher"].get("userType"),
                    "token": flow["teacher_token"],
                },
            },
        )
        errors: list[str] = []
        requests: list[str] = []
        page.on("pageerror", lambda exc: errors.append(str(exc)))
        page.on("requestfinished", lambda req: requests.append(req.url))

        page.goto(f"{BASE_WEB}/#/chat/{flow['room_id']}?otherUid={flow['student_uid']}", wait_until="networkidle")
        page.locator("button", has_text="通过").click()
        page.wait_for_timeout(2500)

        current_url = page.url
        page_text = page.locator("body").inner_text()
        browser.close()
        return {
            "current_url": current_url,
            "page_text": page_text,
            "errors": errors,
            "requests": requests,
        }


def verify_api(flow: dict) -> dict:
    messages = curl_json(
        "GET",
        "/chat/public/msg/page",
        token=flow["teacher_token"],
        query={"roomId": flow["room_id"], "pageSize": 50},
    )
    items = messages.get("list") or []
    has_status = False
    brokerage = None
    for item in items:
        body = ((item or {}).get("message") or {}).get("body")
        if isinstance(body, dict) and body.get("type") == "tutor_application_status" and body.get("applicationId") == flow["application_id"]:
            has_status = True
        if isinstance(body, dict) and body.get("type") == "brokerage_required":
            brokerage = body
    enter = curl_json("POST", f"/chat/application/{flow['application_id']}/enter-chat", token=flow["teacher_token"], body={})
    detail = curl_json("GET", f"/chat/application/{flow['application_id']}", token=flow["teacher_token"])
    return {
        "has_status": has_status,
        "brokerage": brokerage,
        "enter": enter,
        "detail": detail,
        "messages_count": len(items),
    }


def main():
    flow = prepare_flow()
    ui = verify_browser(flow)
    api = verify_api(flow)
    result = {"flow": flow, "ui": ui, "api": api}
    print(json.dumps(result, ensure_ascii=False))

    if "系统内部异常" in ui["page_text"]:
        raise SystemExit("browser still shows 系统内部异常")
    if "/pay/cashier" not in ui["current_url"]:
        raise SystemExit(f"browser did not navigate to cashier: {ui['current_url']}")
    if not api["has_status"]:
        raise SystemExit("missing tutor application status message")
    if not api["brokerage"]:
        raise SystemExit("missing brokerage required message")
    if not api["enter"].get("paymentRequired"):
        raise SystemExit(f"enterChat not gated for payment: {api['enter']}")


if __name__ == "__main__":
    main()
