#!/usr/bin/env bash
set -euo pipefail
set +x

TARGET="${TARGET:-}"
MODE="${MODE:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
PREFLIGHT="${SCRIPT_DIR}/real-model-readiness-preflight.sh"

usage() {
  cat <<'EOF'
Usage:
  scripts/smoke/real-model-controlled-smoke.sh
  TARGET=<target> MODE=plan scripts/smoke/real-model-controlled-smoke.sh
  TARGET=<target> MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
  ALLOW_REAL_MODEL_SMOKE=1 TARGET=<target> MODE=execute scripts/smoke/real-model-controlled-smoke.sh

Targets:
  yunwu-suno          Public-network Suno smoke path; not the production replacement for DreamMaker.
  dreammaker-suno     Production-target Suno path through DreamMaker.
  dreammaker-minimax  Production-target MiniMax path through DreamMaker.
  deepseek            Real lyrics model path; script: deepseek-real-lyrics-smoke.sh
  wellapi-image2      Public-network Image 2 smoke path; not the production replacement for DreamMaker.
  dreammaker-image2   Production-target Image 2 path through DreamMaker.

Modes:
  list       Print target matrix only.
  plan       Print target-specific commands and runbook.
  preflight  Run strict read-only readiness preflight for the target.
  execute    Delegate to an existing real smoke script when available.

Safety:
  execute requires ALLOW_REAL_MODEL_SMOKE=1 and still relies on each delegated script's own ALLOW_* gate.
EOF
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

print_matrix() {
  cat <<'EOF'
Real model controlled smoke targets

TARGET              TYPE                         EXECUTION
yunwu-suno         public-network smoke          script: yunwu-suno-real-music-stack-smoke.sh
dreammaker-suno    production-target music       script: dreammaker-real-music-stack-smoke.sh REAL_PROVIDER=suno
dreammaker-minimax production-target music       script: dreammaker-real-music-stack-smoke.sh REAL_PROVIDER=minimax
deepseek           real lyrics model             script: deepseek-real-lyrics-smoke.sh
wellapi-image2     public-network smoke          script: wellapi-image2-real-cover-stack-smoke.sh
dreammaker-image2  production-target image2      script: dreammaker-image2-real-cover-stack-smoke.sh

DreamMaker music and DreamMaker Image 2 must remain the production-target paths.
Yunwu and WellAPI are current public-network controlled smoke paths only.
EOF
}

runbook_for() {
  case "$1" in
    yunwu-suno) printf '%s\n' "docs/runbook/yunwu-suno-controlled-real-integration.md" ;;
    dreammaker-suno|dreammaker-minimax) printf '%s\n' "docs/runbook/dreammaker-controlled-real-integration.md" ;;
    deepseek) printf '%s\n' "docs/runbook/deepseek-controlled-real-integration.md" ;;
    wellapi-image2|dreammaker-image2) printf '%s\n' "docs/runbook/image2-controlled-real-integration.md" ;;
    *) fail "Unsupported target: $1" ;;
  esac
}

target_note() {
  case "$1" in
    yunwu-suno)
      printf '%s\n' "Yunwu Suno is for current public-network controlled smoke. It does not replace DreamMaker production."
      ;;
    dreammaker-suno)
      printf '%s\n' "DreamMaker Suno is a production-target path. Use company intranet/permissioned credentials when required."
      ;;
    dreammaker-minimax)
      printf '%s\n' "DreamMaker MiniMax is a production-target path. Use company intranet/permissioned credentials when required."
      ;;
    deepseek)
      printf '%s\n' "DeepSeek is the real lyrics model path. Open only the LLM path for first smoke; keep music/image/company systems mocked."
      ;;
    wellapi-image2)
      printf '%s\n' "WellAPI Image 2 is for current public-network controlled smoke. It does not replace DreamMaker Image 2 production."
      ;;
    dreammaker-image2)
      printf '%s\n' "DreamMaker Image 2 is a production-target path. Open only the Image 2 path for first smoke; keep music/DeepSeek/company systems mocked."
      ;;
    *)
      fail "Unsupported target: $1"
      ;;
  esac
}

print_plan() {
  local target="$1"
  local runbook
  runbook="$(runbook_for "$target")"

  printf 'Target: %s\n' "$target"
  printf 'Role: '
  target_note "$target"
  printf 'Runbook: %s\n' "$runbook"
  printf 'Preflight: TARGET=%s STRICT=true scripts/smoke/real-model-readiness-preflight.sh\n' "$target"

  case "$target" in
    yunwu-suno)
      printf 'Execute: ALLOW_REAL_MODEL_SMOKE=1 ALLOW_YUNWU_REAL_SMOKE=1 TARGET=yunwu-suno MODE=execute scripts/smoke/real-model-controlled-smoke.sh\n'
      ;;
    dreammaker-suno)
      printf 'Execute: ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_REAL_SMOKE=1 TARGET=dreammaker-suno MODE=execute scripts/smoke/real-model-controlled-smoke.sh\n'
      ;;
    dreammaker-minimax)
      printf 'Execute: ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_REAL_SMOKE=1 TARGET=dreammaker-minimax MODE=execute scripts/smoke/real-model-controlled-smoke.sh\n'
      ;;
    deepseek)
      printf 'Execute: ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DEEPSEEK_REAL_SMOKE=1 TARGET=deepseek MODE=execute scripts/smoke/real-model-controlled-smoke.sh\n'
      ;;
    wellapi-image2)
      printf 'Execute: ALLOW_REAL_MODEL_SMOKE=1 ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1 TARGET=wellapi-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh\n'
      ;;
    dreammaker-image2)
      printf 'Execute: ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 TARGET=dreammaker-image2 MODE=execute scripts/smoke/real-model-controlled-smoke.sh\n'
      ;;
    *)
      fail "Unsupported target: $target"
      ;;
  esac
}

run_preflight() {
  local target="$1"
  TARGET="$target" STRICT=true "$PREFLIGHT"
}

require_execute_gate() {
  if [[ "${ALLOW_REAL_MODEL_SMOKE:-}" != "1" ]]; then
    fail "MODE=execute requires ALLOW_REAL_MODEL_SMOKE=1. Run MODE=plan and MODE=preflight first."
  fi
}

execute_target() {
  local target="$1"
  require_execute_gate
  run_preflight "$target"

  case "$target" in
    yunwu-suno)
      ALLOW_YUNWU_REAL_SMOKE="${ALLOW_YUNWU_REAL_SMOKE:-}" "${SCRIPT_DIR}/yunwu-suno-real-music-stack-smoke.sh"
      ;;
    dreammaker-suno)
      REAL_PROVIDER=suno ALLOW_DREAMMAKER_REAL_SMOKE="${ALLOW_DREAMMAKER_REAL_SMOKE:-}" "${SCRIPT_DIR}/dreammaker-real-music-stack-smoke.sh"
      ;;
    dreammaker-minimax)
      REAL_PROVIDER=minimax ALLOW_DREAMMAKER_REAL_SMOKE="${ALLOW_DREAMMAKER_REAL_SMOKE:-}" "${SCRIPT_DIR}/dreammaker-real-music-stack-smoke.sh"
      ;;
    wellapi-image2)
      ALLOW_WELLAPI_IMAGE2_REAL_SMOKE="${ALLOW_WELLAPI_IMAGE2_REAL_SMOKE:-}" "${SCRIPT_DIR}/wellapi-image2-real-cover-stack-smoke.sh"
      ;;
    deepseek)
      ALLOW_DEEPSEEK_REAL_SMOKE="${ALLOW_DEEPSEEK_REAL_SMOKE:-}" "${SCRIPT_DIR}/deepseek-real-lyrics-smoke.sh"
      ;;
    dreammaker-image2)
      ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE="${ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE:-}" "${SCRIPT_DIR}/dreammaker-image2-real-cover-stack-smoke.sh"
      ;;
    *)
      fail "Unsupported target: $target"
      ;;
  esac
}

cd "$REPO_ROOT"

if [[ -z "$TARGET" ]]; then
  MODE="${MODE:-list}"
else
  MODE="${MODE:-plan}"
fi

case "$MODE" in
  list)
    print_matrix
    ;;
  plan)
    [[ -n "$TARGET" ]] || fail "MODE=plan requires TARGET."
    print_plan "$TARGET"
    ;;
  preflight)
    [[ -n "$TARGET" ]] || fail "MODE=preflight requires TARGET."
    run_preflight "$TARGET"
    ;;
  execute)
    [[ -n "$TARGET" ]] || fail "MODE=execute requires TARGET."
    execute_target "$TARGET"
    ;;
  *)
    usage >&2
    fail "Unsupported MODE: $MODE"
    ;;
esac
