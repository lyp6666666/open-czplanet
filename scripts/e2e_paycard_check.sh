set -e
cd "$(dirname "$0")/.."

for i in $(seq 1 60); do
  code="$(curl -s -o /dev/null -m 1 -w '%{http_code}' http://localhost:18080/api/v1/public/home/config || true)"
  if [ "$code" = "200" ]; then
    break
  fi
  sleep 1
done

student_phone="13800000031"
teacher_phone="13800000032"

curl -s -H 'Content-Type: application/json' -d "{\"phone\":\"$student_phone\"}" http://localhost:18080/user/sendcode >/dev/null
code_s="$(grep 'SMS SEND SUCCESS' .logs/tutor-appointment-service.log | grep "phone: $student_phone" | tail -n 1 | sed -E 's/.*code: ([0-9]+).*/\1/')"
resp_s="$(curl -s -H 'Content-Type: application/json' -d "{\"phone\":\"$student_phone\",\"code\":\"$code_s\",\"userRoleEnum\":\"STUDENT\"}" http://localhost:18080/user/loginOrRegister)"
student_token="$(echo "$resp_s" | grep -oE '\"token\":\"[^\"]+\"' | head -n 1 | sed -E 's/\"token\":\"([^\"]+)\"/\1/')"
student_uid="$(echo "$resp_s" | grep -oE '\"user\":\\{\"id\":[0-9]+' | head -n 1 | sed -E 's/.*\"id\":([0-9]+).*/\1/')"

curl -s -H 'Content-Type: application/json' -d "{\"phone\":\"$teacher_phone\"}" http://localhost:18080/user/sendcode >/dev/null
code_t="$(grep 'SMS SEND SUCCESS' .logs/tutor-appointment-service.log | grep "phone: $teacher_phone" | tail -n 1 | sed -E 's/.*code: ([0-9]+).*/\1/')"
resp_t="$(curl -s -H 'Content-Type: application/json' -d "{\"phone\":\"$teacher_phone\",\"code\":\"$code_t\",\"userRoleEnum\":\"TEACHER\"}" http://localhost:18080/user/loginOrRegister)"
teacher_token="$(echo "$resp_t" | grep -oE '\"token\":\"[^\"]+\"' | head -n 1 | sed -E 's/\"token\":\"([^\"]+)\"/\1/')"
teacher_uid="$(echo "$resp_t" | grep -oE '\"user\":\\{\"id\":[0-9]+' | head -n 1 | sed -E 's/.*\"id\":([0-9]+).*/\1/')"

payload='{"subjectName":"数学","subjectOther":false,"title":"测试需求-支付卡片","description":"用于测试申请通过后教师侧出现支付卡片","studentGender":"male","teacherGenderPreference":"both","teacherRequirementDetail":"耐心","classMode":"online","frequencyPerWeek":2,"publisherIdentity":"PARENT","budgetMin":80,"budgetMax":120,"stageCode":"PRIMARY","educationRequirement":"UNLIMITED","schedule":"[\\"Tue 19-21\\"]"}'
resp_d="$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer $student_token" -d "$payload" http://localhost:18080/api/v1/parent/jobs)"
demand_id="$(echo "$resp_d" | grep -oE '\"data\":[0-9]+' | head -n 1 | sed -E 's/\"data\":([0-9]+)/\1/')"

app_payload="{\"receiverUid\":$student_uid,\"contextType\":\"DEMAND\",\"contextId\":$demand_id,\"content\":\"你好，我想沟通一下需求\",\"clientRequestId\":\"e2e-paycard\"}"
resp_a="$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer $teacher_token" -d "$app_payload" http://localhost:18080/chat/application/start-chat)"
room_id="$(echo "$resp_a" | grep -oE '\"roomId\":[0-9]+' | head -n 1 | sed -E 's/\"roomId\":([0-9]+)/\1/')"
app_id="$(echo "$resp_a" | grep -oE '\"applicationId\":[0-9]+' | head -n 1 | sed -E 's/\"applicationId\":([0-9]+)/\1/')"

curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer $student_token" -d '{"action":"ACCEPT"}' "http://localhost:18080/chat/application/$app_id/decision-message" >/dev/null

msgs="$(curl -s -H "Authorization: Bearer $teacher_token" "http://localhost:18080/chat/public/msg/page?roomId=$room_id&pageSize=50")"
echo "$msgs" | grep -q '\"type\":\"brokerage_required\"'

echo "OK teacherUid=$teacher_uid studentUid=$student_uid roomId=$room_id applicationId=$app_id brokerage_required=present"

