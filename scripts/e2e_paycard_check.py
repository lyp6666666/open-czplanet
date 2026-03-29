import json
import os
import re
import subprocess
import sys
import time


ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def sh(cmd: str) -> str:
    return subprocess.check_output(cmd, shell=True, cwd=ROOT, text=True)


def curl_json(cmd: str):
    out = sh(cmd)
    return json.loads(out)


def wait_http(url: str, want: str = "200", tries: int = 60) -> None:
    for _ in range(tries):
        try:
            code = sh(f"curl -s -o /dev/null -m 1 -w '%{{http_code}}' {url}").strip()
            if code == want:
                return
        except Exception:
            pass
        time.sleep(1)
    raise RuntimeError(url)


def last_sms_code(phone: str) -> str:
    log_path = os.path.join(ROOT, ".logs", "tutor-appointment-service.log")
    txt = sh(f"tail -n 500 {log_path}")
    for line in txt.splitlines()[::-1]:
        if "SMS SEND SUCCESS" in line and f"phone: {phone}" in line:
            m = re.search(r"code: (\\d+)", line)
            if m:
                return m.group(1)
    raise RuntimeError(phone)


def login(phone: str, role: str):
    sh(
        "curl -s -H 'Content-Type: application/json' "
        + f"-d '{{\"phone\":\"{phone}\"}}' http://localhost:18080/user/sendcode >/dev/null"
    )
    code = last_sms_code(phone)
    res = curl_json(
        "curl -s -H 'Content-Type: application/json' "
        + f"-d '{json.dumps({'phone': phone, 'code': code, 'userRoleEnum': role})}' "
        + "http://localhost:18080/user/loginOrRegister"
    )
    data = res.get("data") or {}
    token = data.get("token") or data.get("jwt")
    user = data.get("user") or {}
    uid = user.get("id")
    if not token or not uid:
        raise RuntimeError(res)
    return int(uid), token


def main() -> int:
    wait_http("http://localhost:18081/actuator/health")
    wait_http("http://localhost:18082/actuator/health")
    wait_http("http://localhost:18080/api/v1/public/home/config")

    student_uid, student_token = login("13800000021", "STUDENT")
    teacher_uid, teacher_token = login("13800000022", "TEACHER")

    demand_payload = {
        "subjectName": "数学",
        "subjectOther": False,
        "title": "测试需求-支付卡片",
        "description": "用于测试申请通过后教师侧出现支付卡片",
        "studentGender": "male",
        "teacherGenderPreference": "both",
        "teacherRequirementDetail": "耐心",
        "classMode": "online",
        "frequencyPerWeek": 2,
        "publisherIdentity": "PARENT",
        "budgetMin": 80,
        "budgetMax": 120,
        "stageCode": "PRIMARY",
        "educationRequirement": "UNLIMITED",
        "schedule": "[\"Tue 19-21\"]",
    }
    res = curl_json(
        "curl -s -H 'Content-Type: application/json' "
        + f"-H 'Authorization: Bearer {student_token}' "
        + f"-d '{json.dumps(demand_payload)}' "
        + "http://localhost:18080/api/v1/parent/jobs"
    )
    demand_id = res.get("data")
    if not demand_id:
        raise RuntimeError(res)

    app_payload = {
        "receiverUid": student_uid,
        "contextType": "DEMAND",
        "contextId": int(demand_id),
        "content": "你好，我想沟通一下需求",
        "clientRequestId": "test-apply-pay-card",
    }
    res = curl_json(
        "curl -s -H 'Content-Type: application/json' "
        + f"-H 'Authorization: Bearer {teacher_token}' "
        + f"-d '{json.dumps(app_payload)}' "
        + "http://localhost:18080/chat/application/start-chat"
    )
    msg = ((res.get("data") or {}).get("message") or {})
    room_id = msg.get("roomId")
    app_id = ((msg.get("body") or {}).get("applicationId"))
    if not room_id or not app_id:
        raise RuntimeError(res)

    res = curl_json(
        "curl -s -H 'Content-Type: application/json' "
        + f"-H 'Authorization: Bearer {student_token}' "
        + "-d '{\"action\":\"ACCEPT\"}' "
        + f"http://localhost:18080/chat/application/{app_id}/decision-message"
    )
    if res.get("code") != 0:
        raise RuntimeError(res)

    page = curl_json(
        "curl -s "
        + f"-H 'Authorization: Bearer {teacher_token}' "
        + f"'http://localhost:18080/chat/public/msg/page?roomId={room_id}&pageSize=50'"
    )
    items = ((page.get("data") or {}).get("list") or [])
    found_pay = False
    for it in items:
        body = ((it.get("message") or {}).get("body"))
        if isinstance(body, dict) and body.get("type") == "brokerage_required":
            found_pay = True
            break
    if not found_pay:
        raise RuntimeError({"roomId": room_id, "applicationId": app_id, "tail": items[-5:]})

    sys.stdout.write(
        json.dumps(
            {
                "teacherUid": teacher_uid,
                "studentUid": student_uid,
                "roomId": room_id,
                "applicationId": app_id,
                "brokerageRequired": True,
            },
            ensure_ascii=False,
        )
        + "\n"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as e:
        sys.stderr.write(str(e) + "\n")
        raise

