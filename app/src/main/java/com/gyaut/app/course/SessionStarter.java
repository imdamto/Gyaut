package com.gyaut.app.course;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.databinding.DialogStartSessionBinding;
import com.gyaut.app.realtime.PodiumActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// 수업 시작: 주차·주제를 묻고, 수업 문서를 만든 뒤 교탁 모드로 이동
// 과목 상세 화면과 내 과목 화면(다음 수업 카드)에서 같이 씀
public class SessionStarter {

    private final AppCompatActivity activity;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private boolean busy; // 주차를 불러오거나 저장하는 중. 버튼을 연달아 눌러도 한 번만 진행

    public SessionStarter(AppCompatActivity activity) {
        this.activity = activity;
    }

    // 마지막 수업을 찾아서 주차를 미리 채운 창을 띄움
    // 마지막 수업이 이번 주에 있었으면 같은 주차, 아니면 다음 주차
    public void start(String courseId) {
        if (busy) {
            return;
        }
        busy = true;
        db.collection(FirestorePaths.COURSES).document(courseId)
                .collection(FirestorePaths.SESSIONS)
                .orderBy("startedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    busy = false;
                    long week = 1;
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot last = snapshot.getDocuments().get(0);
                        long lastWeek = last.getLong("week");
                        week = isThisWeek(last.getDate("startedAt")) ? lastWeek : lastWeek + 1;
                    }
                    askSessionInfo(courseId, week);
                })
                .addOnFailureListener(e -> {
                    busy = false;
                    Toast.makeText(activity, "수업 정보를 불러오지 못했어요", Toast.LENGTH_SHORT).show();
                });
    }

    // 날짜가 이번 주(월요일~일요일) 안인지
    private boolean isThisWeek(Date date) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        now.setFirstDayOfWeek(Calendar.MONDAY);
        then.setFirstDayOfWeek(Calendar.MONDAY);
        then.setTime(date);
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.WEEK_OF_YEAR) == then.get(Calendar.WEEK_OF_YEAR);
    }

    // 주차와 주제를 입력받는 창
    private void askSessionInfo(String courseId, long week) {
        DialogStartSessionBinding dialogBinding = DialogStartSessionBinding.inflate(activity.getLayoutInflater());
        dialogBinding.editWeek.setText(String.valueOf(week));

        new MaterialAlertDialogBuilder(activity)
                .setTitle("수업 시작")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("시작", (dialog, which) ->
                        saveSession(courseId, textOf(dialogBinding.editWeek), textOf(dialogBinding.editTopic)))
                .setNegativeButton("취소", null)
                .show();
    }

    // 새 수업 문서를 만들고, 과목 문서에 진행 중인 수업 ID를 기록 → 교탁 모드로
    private void saveSession(String courseId, String weekText, String topic) {
        if (weekText.isEmpty()) {
            Toast.makeText(activity, "주차를 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference courseRef = db.collection(FirestorePaths.COURSES).document(courseId);
        DocumentReference sessionRef = courseRef.collection(FirestorePaths.SESSIONS).document(); // 새 수업 ID 자동 생성

        Map<String, Object> session = new HashMap<>();
        session.put("week", Long.parseLong(weekText));
        session.put("startedAt", FieldValue.serverTimestamp());
        if (!topic.isEmpty()) {
            session.put("topic", topic);
        }

        WriteBatch batch = db.batch();
        batch.set(sessionRef, session);
        batch.update(courseRef, "liveSessionId", sessionRef.getId());

        busy = true;
        batch.commit()
                .addOnSuccessListener(unused -> {
                    busy = false;
                    openPodium(activity, courseId, sessionRef.getId());
                })
                .addOnFailureListener(e -> {
                    busy = false;
                    Toast.makeText(activity, "수업을 시작하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show();
                });
    }

    // 교탁 모드로 이동. 팀장님 화면이 과목 ID와 수업 ID를 받아서 씀
    public static void openPodium(Context context, String courseId, String sessionId) {
        Intent intent = new Intent(context, PodiumActivity.class);
        intent.putExtra(IntentKeys.COURSE_ID, courseId);
        intent.putExtra(IntentKeys.SESSION_ID, sessionId);
        context.startActivity(intent);
    }

    private String textOf(EditText editText) {
        Editable text = editText.getText();
        return text == null ? "" : text.toString().trim();
    }
}