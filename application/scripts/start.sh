#!/usr/bin/env bash
# Run from backend folder: ./start.sh [start|stop|restart|status]
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

APP_JAR="$DIR/app.jar"
PID_FILE="$DIR/app.pid"
LOG_FILE="$DIR/app.log"

# /usr/local = tesseract 5 + leptonica 1.87 built by install-tesseract-centos.sh
# /usr/lib64  = fallback for RPM libs
export LD_LIBRARY_PATH="/usr/local/lib:/usr/local/lib64:/usr/lib64:/lib64${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export PATH="/usr/local/bin:${PATH}"

JAVA_OPTS=(
  -Djava.awt.headless=true
)

is_running() {
  [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null
}

start() {
  if is_running; then
    echo "Already running (PID $(cat "$PID_FILE"))"
    exit 0
  fi
  if [[ ! -f "$APP_JAR" ]]; then
    echo "error: app.jar not found in $DIR" >&2
    exit 1
  fi
  if [[ ! -x /usr/local/bin/tesseract ]] && [[ ! -x "${PREFIX:-/usr/local}/bin/tesseract" ]]; then
    echo "warning: /usr/local/bin/tesseract not found — run: sudo ./install-tesseract-centos.sh" >&2
  fi
  echo "Starting app.jar (tess4j 5.x + /usr/local tesseract/leptonica) ..."
  nohup java "${JAVA_OPTS[@]}" -jar "$APP_JAR" >>"$LOG_FILE" 2>&1 &
  echo $! >"$PID_FILE"
  echo "Started PID $(cat "$PID_FILE")"
  echo "Log: tail -f $LOG_FILE"
}

stop() {
  if ! is_running; then
    rm -f "$PID_FILE"
    echo "Not running."
    return 0
  fi
  echo "Stopping PID $(cat "$PID_FILE") ..."
  kill "$(cat "$PID_FILE")" 2>/dev/null || true
  sleep 2
  is_running && kill -9 "$(cat "$PID_FILE")" 2>/dev/null || true
  rm -f "$PID_FILE"
  echo "Stopped."
}

status() {
  if is_running; then
    echo "Running (PID $(cat "$PID_FILE"))"
  else
    echo "Not running."
  fi
}

case "${1:-start}" in
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status) status ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
    ;;
esac
