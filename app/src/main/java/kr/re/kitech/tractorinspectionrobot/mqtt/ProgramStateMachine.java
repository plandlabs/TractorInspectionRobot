package kr.re.kitech.tractorinspectionrobot.mqtt;

import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;

public class ProgramStateMachine {

    private static final String TAG = "ProgramStateMachine";

    // 상태 상수
    public static final int PHASE_IDLE      = 0;
    public static final int PHASE_LIFTING   = 1; // Z→0
    public static final int PHASE_MOVING_XY = 2; // XY→타겟, Z=0
    public static final int PHASE_MOVING_Z  = 3; // Z→타겟 Z
    public static final int PHASE_WAITING   = 4; // intervalSecond 대기

    // 위치 허용 오차
    private static final int POS_TOL = 10;
    private static final String FP = "pc-controller";

    public interface Callback {
        // MQTT로 publish 하고 싶을 때 호출됨
        void publish(String topic, String payload, int qos, boolean retain);

        // "Program 1/5 (Z→0)" 같은 상태 텍스트 변경 시
        void onProgramStatusLabelChanged(String label);

        // 진행 상황 브로드캐스트용
        void onProgramProgressChanged(boolean running, int index, int total, int phase);

        // 현재 pose를 UI 쪽(MonitProgram 등)에 알려줄 때
        void onProgramPose(RobotState pose);
    }

    private final Handler handler;
    private final String baseTopic;
    private final String reqTopic;
    private final int rollbackTargetStep;
    private final Callback callback;

    private boolean programRunning = false;
    private int programPhase = PHASE_IDLE;
    private int programIndex = -1;
    private boolean programStepScheduled = false;
    private int programIntervalSecond = 10;

    private final List<RobotState> programList = new ArrayList<>();
    private RobotState currentProgramTarget = null;
    private RobotState liftTarget = null;
    private RobotState xyTarget = null;
    private RobotState finalTarget = null;

    private RobotState lastStaState = null;

    private int opidCounter = 1;

    public ProgramStateMachine(Handler handler,
                               String baseTopic,
                               String reqTopic,
                               int rollbackTargetStep,
                               Callback callback) {
        this.handler = handler;
        this.baseTopic = baseTopic;
        this.reqTopic = reqTopic;
        this.rollbackTargetStep = rollbackTargetStep;
        this.callback = callback;
    }

    // 외부에서 STA JSON이 들어왔을 때 호출
    public void onStaPayload(String payload) {
        if (!programRunning) return;

        try {
            JSONObject root = new JSONObject(payload);
            JSONObject ct = root.optJSONObject("ct");
            if (ct == null) return;

            int x  = (lastStaState != null) ? lastStaState.x  : 0;
            int y  = (lastStaState != null) ? lastStaState.y  : 0;
            int z  = (lastStaState != null) ? lastStaState.z  : 0;
            int s1 = (lastStaState != null) ? lastStaState.s1 : 0;
            int s2 = (lastStaState != null) ? lastStaState.s2 : 0;
            int s3 = (lastStaState != null) ? lastStaState.s3 : 0;

            JSONObject motion = ct.optJSONObject("motion");
            if (motion != null) {
                JSONObject pos = motion.optJSONObject("pos");
                if (pos != null) {
                    x = pos.optInt("x", x);
                    y = pos.optInt("y", y);
                    z = pos.optInt("z", z);
                }
            }

            JSONObject servoContainer = ct.optJSONObject("servo");
            if (servoContainer != null) {
                JSONObject servo = servoContainer.optJSONObject("angles");
                if (servo == null && servoContainer.has("s1")) {
                    servo = servoContainer;
                }
                if (servo != null) {
                    s1 = servo.optInt("s1", s1);
                    s2 = servo.optInt("s2", s2);
                    s3 = servo.optInt("s3", s3);
                }
            }

            long ts = System.currentTimeMillis();
            lastStaState = new RobotState(x, y, z, s1, s2, s3, ts);

            onProgramStateUpdatedBySta();
        } catch (Exception e) {
            Log.e(TAG, "onStaPayload parse error", e);
        }
    }

    public void startProgram(String programJson, int intervalSec) {
        stopProgram(); // 기존 것 정리

        if (programJson == null || programJson.isEmpty()) {
            Log.w(TAG, "startProgram: programJson is empty");
            return;
        }

        try {
            JSONArray arr = new JSONArray(programJson);
            programList.clear();
            for (int j = 0; j < arr.length(); j++) {
                JSONObject obj = arr.getJSONObject(j);
                RobotState rs = new RobotState(obj);
                programList.add(rs);
            }
        } catch (Exception e) {
            Log.e(TAG, "startProgram: invalid program json", e);
            programList.clear();
            return;
        }

        if (programList.isEmpty()) {
            Log.w(TAG, "startProgram: programList is empty");
            return;
        }

        programIntervalSecond = Math.max(1, intervalSec);

        programRunning = true;
        programIndex = 0;
        programPhase = PHASE_LIFTING;
        programStepScheduled = false;
        currentProgramTarget = programList.get(0);
        liftTarget = xyTarget = finalTarget = null;

        prepareProgramPhaseTargets();

        if (liftTarget != null) {
            sendMoveAndServo(liftTarget);
        } else if (finalTarget != null) {
            sendMoveAndServo(finalTarget);
        }

        Log.i(TAG, "Program started, total items=" + programList.size());

        updateProgramStatusLabel();
        broadcastProgramProgress();
    }

    public void stopProgram() {
        if (!programRunning && programPhase == PHASE_IDLE) {
            programList.clear();
            // 상태 라벨 초기화
            if (callback != null) {
                callback.onProgramStatusLabelChanged("");
                callback.onProgramProgressChanged(false, -1, 0, PHASE_IDLE);
            }
            return;
        }

        programRunning = false;
        handler.removeCallbacksAndMessages(null);
        programPhase = PHASE_IDLE;
        programIndex = -1;
        programStepScheduled = false;
        currentProgramTarget = null;
        liftTarget = xyTarget = finalTarget = null;

        Log.i(TAG, "Program stopped");

        updateProgramStatusLabel();
        broadcastProgramProgress();
    }

    public boolean isProgramRunning() {
        return programRunning;
    }

    private void onProgramStateUpdatedBySta() {
        if (!programRunning || currentProgramTarget == null) return;

        switch (programPhase) {
            case PHASE_LIFTING:
                if (reachedLiftHeight(lastStaState, liftTarget, POS_TOL)) {
                    programPhase = PHASE_MOVING_XY;
                    if (xyTarget != null) {
                        sendMoveAndServo(xyTarget);
                    }
                    updateProgramStatusLabel();
                    broadcastProgramProgress();
                }
                break;

            case PHASE_MOVING_XY:
                if (sameXYZ(lastStaState, xyTarget)) {
                    programPhase = PHASE_MOVING_Z;
                    if (finalTarget != null) {
                        sendMoveAndServo(finalTarget);
                    }
                    updateProgramStatusLabel();
                    broadcastProgramProgress();
                }
                break;

            case PHASE_MOVING_Z:
                if (sameXYZ(lastStaState, finalTarget) && !programStepScheduled) {
                    programPhase = PHASE_WAITING;
                    updateProgramStatusLabel();
                    broadcastProgramProgress();
                    scheduleNextProgramStep();
                }
                break;

            case PHASE_WAITING:
            case PHASE_IDLE:
            default:
                break;
        }
    }

    private void scheduleNextProgramStep() {
        if (!programRunning) return;

        programStepScheduled = true;

        handler.postDelayed(() -> {
            if (!programRunning) return;

            programIndex++;

            if (programIndex >= programList.size()) {
                Log.i(TAG, "Program finished");
                stopProgram();
                return;
            }

            currentProgramTarget = programList.get(programIndex);
            programPhase = PHASE_LIFTING;
            programStepScheduled = false;
            liftTarget = xyTarget = finalTarget = null;

            prepareProgramPhaseTargets();

            if (liftTarget != null) {
                sendMoveAndServo(liftTarget);
            } else if (finalTarget != null) {
                sendMoveAndServo(finalTarget);
            }

            Log.i(TAG, "Program step: " + (programIndex + 1) + " / " + programList.size());

            updateProgramStatusLabel();
            broadcastProgramProgress();
        }, programIntervalSecond * 1000L);
    }

    private void prepareProgramPhaseTargets() {
        if (currentProgramTarget == null) return;

        RobotState pose = (lastStaState != null) ? lastStaState : currentProgramTarget;

        int startX = pose.x;
        int startY = pose.y;
        int startZ = pose.z;

        long ts = System.currentTimeMillis();

        // 1단계: 현재 위치에서 Z를 0까지 올리기
        liftTarget = new RobotState(
                startX,
                startY,
                0,
                currentProgramTarget.s1,
                currentProgramTarget.s2,
                currentProgramTarget.s3,
                ts
        );

        // 2단계: Z=0 유지하면서 XY 이동
        xyTarget = new RobotState(
                currentProgramTarget.x,
                currentProgramTarget.y,
                0,
                currentProgramTarget.s1,
                currentProgramTarget.s2,
                currentProgramTarget.s3,
                ts
        );

        // 3단계: 최종 Z까지 이동
        finalTarget = new RobotState(
                currentProgramTarget.x,
                currentProgramTarget.y,
                currentProgramTarget.z,
                currentProgramTarget.s1,
                currentProgramTarget.s2,
                currentProgramTarget.s3,
                ts
        );

        if (startZ >= 0 &&
                startZ <= rollbackTargetStep &&
                startX == currentProgramTarget.x &&
                startY == currentProgramTarget.y) {
            liftTarget = null;
            xyTarget = null;
            programPhase = PHASE_MOVING_Z;
        }
    }

    private void sendMoveAndServo(RobotState s) {
        sendMoveAbs(s);
        sendServoAbs(s);
        if (callback != null) {
            callback.onProgramPose(s);
        }
    }

    private void sendMoveAbs(RobotState s) {
        try {
            JSONObject root = new JSONObject();
            root.put("mt", "req");
            root.put("tm", nowIso());
            root.put("fp", FP);

            JSONObject ct = new JSONObject();
            ct.put("tg", baseTopic);
            ct.put("cmd", 2001);
            ct.put("opid", opidCounter++);

            JSONObject p = new JSONObject();
            p.put("mode", "abs");
            p.put("x", s.x);
            p.put("y", s.y);
            p.put("z", s.z);
            p.put("scurve", true);

            ct.put("param", p);
            root.put("ct", ct);

            if (callback != null) {
                callback.publish(reqTopic, root.toString(), 1, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "sendMoveAbs error", e);
        }
    }

    private void sendServoAbs(RobotState s) {
        try {
            JSONObject root = new JSONObject();
            root.put("mt", "req");
            root.put("tm", nowIso());
            root.put("fp", FP);

            JSONObject ct = new JSONObject();
            ct.put("tg", baseTopic);
            ct.put("cmd", 2003);
            ct.put("opid", opidCounter++);

            JSONObject p = new JSONObject();
            p.put("mode", "abs");
            p.put("s1", s.s1);
            p.put("s2", s.s2);
            p.put("s3", s.s3);

            ct.put("param", p);
            root.put("ct", ct);

            if (callback != null) {
                callback.publish(reqTopic, root.toString(), 1, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "sendServoAbs error", e);
        }
    }

    private boolean sameXYZ(RobotState a, RobotState b) {
        if (a == null || b == null) return false;
        return a.x == b.x &&
                a.y == b.y &&
                a.z == b.z;
    }

    private boolean reachedLiftHeight(RobotState cur, RobotState target, int tol) {
        if (cur == null || target == null) return false;
        return Math.abs(cur.x - target.x) <= tol &&
                Math.abs(cur.y - target.y) <= tol &&
                Math.abs(cur.z - target.z) <= tol;
    }

    private static String nowIso() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private String phaseToLabel(int phase) {
        switch (phase) {
            case PHASE_LIFTING:   return "Z→ 0";
            case PHASE_MOVING_XY: return "XY 이동";
            case PHASE_MOVING_Z:  return "Z 이동";
            case PHASE_WAITING:   return "대기";
            case PHASE_IDLE:
            default:              return "";
        }
    }

    private void updateProgramStatusLabel() {
        String label;
        if (!programRunning || programList.isEmpty() || programIndex < 0 || programIndex >= programList.size()) {
            label = "";
        } else {
            String phaseStr = phaseToLabel(programPhase);
            String stepStr  = (programIndex + 1) + "/" + programList.size();
            if (phaseStr.isEmpty()) {
                label = "Program " + stepStr;
            } else {
                label = "Program " + stepStr + " (" + phaseStr + ")";
            }
        }
        if (callback != null) {
            callback.onProgramStatusLabelChanged(label);
        }
    }

    private void broadcastProgramProgress() {
        if (callback != null) {
            int total = programList.size();
            callback.onProgramProgressChanged(programRunning, programIndex, total, programPhase);
        }
    }
}
