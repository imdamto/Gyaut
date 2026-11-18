package com.gyaut.app.realtime;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

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