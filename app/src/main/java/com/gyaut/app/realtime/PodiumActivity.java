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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PodiumActivity extends AppCompatActivity {

    private ActivityPodiumBinding binding;
    private FirebaseFirestore db;
    private Handler handler = new Handler(Looper.getMainLooper());

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

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(clock);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(clock);
    }

    // 시계. 1초마다 지금 시간 다시 넣기 (분 바뀌면 바로 보이게)
    private final Runnable clock = new Runnable() {
        @Override
        public void run() {
            String now = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date());
            binding.tvClock.setText(now);
            handler.postDelayed(this, 1000);
        }
    };
}