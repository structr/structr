#!/bin/bash
# Leave Request REST API test
# Run from command line: bash test-leave-request-rest.sh

BASE="http://localhost:8082/structr/rest"
AUTH="-H X-User:admin -H X-Password:admin"
CT="-H Content-Type:application/json"
CURL="curl -s --max-time 10"
PASSED=0
FAILED=0

assert() {
    local label="$1" actual="$2" expected="$3"
    if [ "$actual" = "$expected" ]; then
        echo "  PASS: $label"
        ((PASSED++))
    else
        echo "  FAIL: $label (expected: $expected, got: $actual)"
        ((FAILED++))
    fi
}

echo "--- REST API: Start process ---"

# 1. Find the process definition
DEF_RESP=$($CURL $AUTH "$BASE/BpmnDefinitions?processName=Employee+Leave+Request")
DEF_ID=$(echo "$DEF_RESP" | jq -r '.result[0].id')
DEF_NAME=$(echo "$DEF_RESP" | jq -r '.result[0].processName')
assert "Found definition" "$DEF_NAME" "Employee Leave Request"

# 2. Start the process
START_RESP=$($CURL $AUTH $CT -X POST "$BASE/BpmnDefinitions/$DEF_ID/startProcess" -d '{}')
INST_ID=$(echo "$START_RESP" | jq -r '.result.id')
INST_STATUS=$(echo "$START_RESP" | jq -r '.result.status')
assert "Process started" "$INST_STATUS" "running"

# 3. Create a LeaveRequest connected to the process instance
LR_RESP=$($CURL $AUTH $CT -X POST "$BASE/LeaveRequest" -d "{
    \"employeeName\": \"Lisa Schmidt\",
    \"leaveType\": \"sick\",
    \"startDate\": \"2026-04-14T00:00:00+0000\",
    \"endDate\": \"2026-04-14T00:00:00+0000\",
    \"days\": 1,
    \"reason\": \"Doctor appointment\",
    \"status\": \"submitted\",
    \"processInstance\": \"$INST_ID\"
}")
# POST returns array of IDs as strings, not objects
LR_ID=$(echo "$LR_RESP" | jq -r '.result[0]')
assert "LeaveRequest created" "$([ -n "$LR_ID" ] && [ "$LR_ID" != "null" ] && echo true)" "true"

echo "--- REST API: Complete submit task ---"

# 4. Find the pending task
TASK_RESP=$($CURL $AUTH "$BASE/TaskInstance?status=created")
TASK_ID=$(echo "$TASK_RESP" | jq -r '.result[0].id')
TASK_NAME=$(echo "$TASK_RESP" | jq -r '.result[0].name')
assert "Found pending task" "$TASK_NAME" "Submit leave request"

# 5. Complete the task (days=1 triggers auto-approve)
COMPLETE_RESP=$($CURL $AUTH $CT -X POST "$BASE/TaskInstance/$TASK_ID/complete" -d '{"days": 1, "leaveType": "sick"}')
COMPLETE_STATUS=$(echo "$COMPLETE_RESP" | jq -r '.result.status')
assert "Task completed" "$COMPLETE_STATUS" "completed"

# 6. Check process status
INST_STATUS2=$($CURL $AUTH "$BASE/ProcessInstance/$INST_ID" | jq -r '.result.status')
assert "Process completed (auto-approved)" "$INST_STATUS2" "completed"

# 7. Update LeaveRequest with approval
$CURL $AUTH $CT -X PUT "$BASE/LeaveRequest/$LR_ID" -d '{"approved": true, "status": "approved"}' > /dev/null

echo "--- REST API: Query results ---"

# 8. Query LeaveRequest via custom view
LR_DETAIL=$($CURL $AUTH "$BASE/LeaveRequest/$LR_ID/custom")
assert "Employee name" "$(echo "$LR_DETAIL" | jq -r '.result.employeeName')" "Lisa Schmidt"
assert "Days" "$(echo "$LR_DETAIL" | jq -r '.result.days')" "1"
assert "Approved" "$(echo "$LR_DETAIL" | jq -r '.result.approved')" "true"
assert "Status" "$(echo "$LR_DETAIL" | jq -r '.result.status')" "approved"

# 9. Query approved requests
APPROVED_COUNT=$($CURL $AUTH "$BASE/LeaveRequest?status=approved" | jq -r '.result | length')
assert "Query approved requests" "$APPROVED_COUNT" "1"

# 10. Graph navigation
PI_REF=$(echo "$LR_DETAIL" | jq -r '.result.processInstance // empty')
assert "Has processInstance ref" "$([ -n "$PI_REF" ] && [ "$PI_REF" != "null" ] && echo true)" "true"

echo ""
echo "===== $PASSED passed, $FAILED failed ====="
