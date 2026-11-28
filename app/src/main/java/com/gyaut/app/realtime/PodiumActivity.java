package com.gyaut.app.realtime;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;   // ← 추가

import com.google.firebase.firestore.FirebaseFirestore;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityPodiumBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FieldValue;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PodiumActivity extends AppCompatActivity {

    private ActivityPodiumBinding binding;
    private FirebaseFirestore db;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ListenerRegistration confusionListener;
    // 학생들이 누른 시간 모아두기
    private List<Long> pressedTimes = new ArrayList<>();
    // 이 과목 전체 학생 수 (비율 계산용)
    private long studentCount = 0;
    // 교탁모드 질문 목록
    private List<PodiumQuestion> questionList = new ArrayList<>();
    private PodiumQuestionAdapter adapter;
    private ListenerRegistration questionListener;
    // 시계가 바뀌었는지 확인용 (분 바뀌면 "몇분 전" 다시 계산)
    private String lastClock = "";

    // 일단 테스트 데이터로. 과목 화면에서 넘겨주면 그걸로 바뀜
    private String courseId = "test-course";
    private String sessionId = "test-session";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPodiumBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        // 앞 화면에서 과목, 수업 아이디 넘겨줬으면 그걸로 바꾸기
        if (getIntent().getStringExtra(IntentKeys.COURSE_ID) != null) {
            courseId = getIntent().getStringExtra(IntentKeys.COURSE_ID);
            sessionId = getIntent().getStringExtra(IntentKeys.SESSION_ID);
        }

        loadClassInfo();

        // 질문 목록 준비
        adapter = new PodiumQuestionAdapter(questionList, question -> markAnswered(question));
        binding.rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvQuestions.setAdapter(adapter);

        // 왼쪽 위 뒤로가기 (수업은 계속 진행 중, 교탁 화면만 닫힘)
        binding.btnBack.setOnClickListener(v -> finish());

        // 수업 종료
        binding.btnEndClass.setOnClickListener(v -> confirmEndClass());

    }

    // 과목명, 주차 불러오기
    private void loadClassInfo() {
        db.collection(FirestorePaths.COURSES).document(courseId).get()
                .addOnSuccessListener(course -> {
                    binding.tvCourseName.setText(course.getString("name"));
                    Long count = course.getLong("studentCount");
                    if (count != null) {
                        studentCount = count;
                    }
                    updateRatio();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "수업 정보를 못 불러왔어요", Toast.LENGTH_SHORT).show());

        db.collection(FirestorePaths.COURSES).document(courseId)
                .collection(FirestorePaths.SESSIONS).document(sessionId).get()
                .addOnSuccessListener(session -> {
                    Long week = session.getLong("week");
                    binding.tvLiveWeek.setText("LIVE · " + week + "주차");
                });
    }

    // confusion 컬렉션 실시간으로 받기
    private void listenConfusion() {
        confusionListener = db.collection(FirestorePaths.COURSES).document(courseId)
                .collection(FirestorePaths.SESSIONS).document(sessionId)
                .collection(FirestorePaths.CONFUSION)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    pressedTimes.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Timestamp pressedAt = doc.getTimestamp("pressedAt",
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        if (pressedAt != null) {
                            pressedTimes.add(pressedAt.toDate().getTime());
                        }
                    }
                    updateRatio();
                });
    }

    // 이 수업 질문 실시간으로 받기
    private void listenQuestions() {
        questionListener = db.collection(FirestorePaths.COURSES).document(courseId)
                .collection(FirestorePaths.QUESTIONS)
                .whereEqualTo("sessionId", sessionId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    questionList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        // 숨긴 질문은 교탁에서도 안 보이게
                        if (!"visible".equals(doc.getString("status"))) {
                            continue;
                        }

                        PodiumQuestion q = new PodiumQuestion();
                        q.id = doc.getId();
                        q.content = doc.getString("content");

                        Long likes = doc.getLong("likeCount");
                        q.likeCount = likes != null ? likes : 0;

                        Timestamp createdAt = doc.getTimestamp("createdAt",
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        q.createdAt = createdAt != null ? createdAt.toDate().getTime() : System.currentTimeMillis();

                        Boolean answered = doc.getBoolean("answered");
                        q.answered = answered != null && answered;

                        questionList.add(q);
                    }

                    // 정렬: 답변 안 한 것 먼저 -> 궁금해요 많은 순 -> 먼저 올라온 순
                    questionList.sort((a, b) -> {
                        if (a.answered != b.answered) {
                            return a.answered ? 1 : -1;
                        }
                        if (a.likeCount != b.likeCount) {
                            return Long.compare(b.likeCount, a.likeCount);
                        }
                        return Long.compare(a.createdAt, b.createdAt);
                    });

                    binding.tvQuestionCount.setText(questionList.size() + "개");
                    adapter.notifyDataSetChanged();
                });
    }

    // 답변 완료 저장 -> 리스너가 다시 받아서 맨 밑으로 정렬됨
    private void markAnswered(PodiumQuestion question) {
        db.collection(FirestorePaths.COURSES).document(courseId)
                .collection(FirestorePaths.QUESTIONS).document(question.id)
                .update("answered", true, "answeredAt", FieldValue.serverTimestamp())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 수업 종료 확인창
    private void confirmEndClass() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("수업을 종료할까요?")
                .setPositiveButton("종료", (dialog, which) -> endClass())
                .setNegativeButton("취소", null)
                .show();
    }

    // 과목 문서에서 진행 중인 수업 ID 지우기 -> 학생 화면도 자동으로 닫힘
    private void endClass() {
        db.collection(FirestorePaths.COURSES).document(courseId)
                .update("liveSessionId", FieldValue.delete())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "수업을 종료했어요", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "수업을 종료하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show());
    }

    // 3분 안에 누른 사람 세서 %, 몇명 중 몇명, 막대 바꾸기
    private void updateRatio() {
        long now = System.currentTimeMillis();
        int count = 0;
        for (long time : pressedTimes) {
            if (now - time < FirestorePaths.CONFUSION_WINDOW_MS) {
                count++;
            }
        }

        int percent = 0;
        if (studentCount > 0) {
            percent = (int) (count * 100 / studentCount);
        }

        binding.tvPercent.setText(percent + "%");
        binding.tvTotal.setText(studentCount + "명 중");
        binding.tvConfusedCount.setText(count + "명");
        binding.barPercent.setProgress(percent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenConfusion();
        listenQuestions();
        handler.post(clock);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (confusionListener != null) {
            confusionListener.remove();
            confusionListener = null;
        }
        if (questionListener != null) {
            questionListener.remove();
            questionListener = null;
        }
        handler.removeCallbacks(clock);
    }

    // 시계 + 비율 다시 세기. 1초마다
    private final Runnable clock = new Runnable() {
        @Override
        public void run() {
            String now = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date());
            binding.tvClock.setText(now);
            updateRatio(); // 3분 지난 사람 빠지게 같이 다시 세기
            // 분이 바뀌면 "몇분 전"도 다시 계산하게 목록 새로 그리기
            if (!now.equals(lastClock)) {
                lastClock = now;
                adapter.notifyDataSetChanged();
            }
            handler.postDelayed(this, 1000);
        }
    };
}