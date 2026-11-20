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
        adapter = new PodiumQuestionAdapter(questionList);
        binding.rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvQuestions.setAdapter(adapter);

        addTestQuestions(); // 화면 확인용. 다음 단계에서 지움
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

    // 카드 잘 나오는지 보려고 가짜 질문 넣기 (다음 단계에서 지움)
    private void addTestQuestions() {
        long now = System.currentTimeMillis();

        PodiumQuestion q1 = new PodiumQuestion();
        q1.content = "사용자 인터뷰에서 답변을 유도하지 않으려면 질문을 어떻게 구성해야 하나요?";
        q1.createdAt = now - 3 * 60000;
        q1.likeCount = 18;
        questionList.add(q1);

        PodiumQuestion q2 = new PodiumQuestion();
        q2.content = "페르소나는 실제 인터뷰 결과만으로 만들어야 하나요?";
        q2.createdAt = now - 18 * 60000;
        q2.likeCount = 12;
        questionList.add(q2);

        PodiumQuestion q3 = new PodiumQuestion();
        q3.content = "정성조사와 정량조사 중 무엇을 먼저 해야 할까요?";
        q3.createdAt = now - 29 * 60000;
        q3.likeCount = 9;
        q3.answered = true;
        questionList.add(q3);

        binding.tvQuestionCount.setText(questionList.size() + "개");
        adapter.notifyDataSetChanged();
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
        handler.post(clock);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (confusionListener != null) {
            confusionListener.remove();
            confusionListener = null;
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
            handler.postDelayed(this, 1000);
        }
    };
}