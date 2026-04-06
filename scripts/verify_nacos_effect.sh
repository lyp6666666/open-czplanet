set -e
cd "$(dirname "$0")/.."

echo "== Nacos load (gateway) =="
tail -n 200 .logs/ai-tutor-gateway.log 2>/dev/null | grep -E "\\[Nacos Config\\] Load config\\[dataId=|\\[Nacos Config\\] Listening config:" || true
echo

echo "== Nacos load (appointment) =="
tail -n 260 .logs/tutor-appointment-service.log 2>/dev/null | grep -E "\\[Nacos Config\\] Load config\\[dataId=|\\[Nacos Config\\] Listening config:" || true
echo

echo "== Nacos load (im) =="
tail -n 260 .logs/videoCall-IM-service.log 2>/dev/null | grep -E "\\[Nacos Config\\] Load config\\[dataId=|\\[Nacos Config\\] Listening config:" || true
echo

echo "== Nacos load (payment) =="
tail -n 260 .logs/payment-service.log 2>/dev/null | grep -E "\\[Nacos Config\\] Load config\\[dataId=|\\[Nacos Config\\] Listening config:" || true
echo

echo "== Nacos load (admin) =="
tail -n 260 .logs/ai-tutor-admin.log 2>/dev/null | grep -E "\\[Nacos Config\\] Load config\\[dataId=|\\[Nacos Config\\] Listening config:" || true
echo

echo "== Nacos discovery (last logs) =="
for f in .logs/ai-tutor-gateway.log .logs/tutor-appointment-service.log .logs/videoCall-IM-service.log .logs/payment-service.log .logs/ai-tutor-admin.log; do
  if [ ! -f "$f" ]; then
    continue
  fi
  echo "-- $f"
  tail -n 260 "$f" 2>/dev/null | grep -Ei "nacos.*discovery|nacos.*naming|register|registered|registration|namespace|server-addr|nacosexception|connect.*nacos|fail.*nacos" || true
  echo
done

echo "== HTTP smoke =="
curl -s -m 3 -o /dev/null -w "gateway home.config %{http_code}\n" http://127.0.0.1:18080/api/v1/public/home/config || true
curl -s -m 3 -o /dev/null -w "appointment home.config %{http_code}\n" http://127.0.0.1:18081/api/v1/public/home/config || true
curl -s -m 3 -o /dev/null -w "im health %{http_code}\n" http://127.0.0.1:18082/actuator/health || true
curl -s -m 3 -o /dev/null -w "payment health %{http_code}\n" http://127.0.0.1:18083/actuator/health || true
curl -s -m 3 -o /dev/null -w "admin health %{http_code}\n" http://127.0.0.1:18084/actuator/health || true
