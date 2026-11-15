package com.gyaut.app.realtime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.os.CountDownTimer;
import com.gyaut.app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gyaut.app.common.FirestorePaths;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityClassBinding;
import com.gyaut.app.question.QuestionActivity;
import com.gyaut.app.question.QuestionListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ClassActivity extends AppCompatActivity {

    private ActivityClassBinding binding;
    private FirebaseFirestore db;
    private CountDownTimer timer;

    // 일단 테스트 데이터로. 과목 화면에서 넘겨주면 그걸로 바뀜
    private String courseId = "test-course";
    private String sessionId = "test-session";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityClassBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        // 앞 화면에서 과목, 수업 아이디 넘겨줬으면 그걸로 바꾸기
        if (getIntent().getStringExtra(IntentKeys.COURSE_ID) != null) {
            courseId = getIntent().getStringExtra(IntentKeys.COURSE_ID);
            sessionId = getIntent().getStringExtra(IntentKeys.SESSION_ID);
        }

        loadClassInfo();

        // 익명 질문 작성 -> 질문 쓰는 화면
        binding.btnWrite.setOnClickListener(v -> {
            Intent intent = new Intent(ClassActivity.this, QuestionActivity.class);
            intent.putExtra(IntentKeys.COURSE_ID, courseId);
            intent.putExtra(IntentKeys.SESSION_ID, sessionId);
            startActivity(intent);
        });

        // 오늘의 질문 보기 -> 질문 목록
        binding.btnQuestions.setOnClickListener(v -> {
            Intent intent = new Intent(ClassActivity.this, QuestionListActivity.class);
            intent.putExtra(IntentKeys.COURSE_ID, courseId);
            intent.putExtra(IntentKeys.SESSION_ID, sessionId);
            startActivity(intent);
        });

        // 갸웃 원 누르면 저장
        binding.cardGyaut.setOnClickListener(v -> pressGyaut());
    }

    // 과목명, 시간표, 주차, 주제 불러오기
    private void loadClassInfo() {
        // 1. 과목 문서에서 과목명, 시간표
        db.collection(FirestorePaths.COURSES).document(courseId).get()
                .addOnSuccessListener(course -> {
                    binding.tvCourseName.setText(course.getString("name"));

                    // 요일마다 시간이 달라서 줄 여러개로 저장돼 있음
                    List<String> schedule = (List<String>) course.get("schedule");
                    if (schedule != null) {
                        binding.tvSchedule.setText(String.join("\n", schedule));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "수업 정보를 못 불러왔어요", Toast.LENGTH_SHORT).show());

        // 2. 오늘 수업 문서에서 주차, 주제
        db.collection(FirestorePaths.COURSES).document(courseId)
                .collection(FirestorePaths.SESSIONS).document(sessionId).get()
                .addOnSuccessListener(session -> {
                    Long week = session.getLong("week");
                    String topic = session.getString("topic");

                    binding.tvCourseInfo.setText(week + "주차");

                    // 주제가 있을 때만 보여주기 (교수님이 안 쓸 수도 있음)
                    if (topic != null && !topic.isEmpty()) {
                        binding.tvTopic.setText(topic);
                        binding.topicBox.setVisibility(View.VISIBLE);
                    }
                });
    }

    // 갸웃 눌렀을 때 -> confusion/내uid 에 누른 시간 저장
    private void pressGyaut() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "로그인이 필요해요", Toast.LENGTH_SHORT).show();
            return;
        }
        // 누르는 순간 살짝 눌렸다 돌아오는 느낌
        binding.cardGyaut.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80)
                .withEndAction(() ->
                        binding.cardGyaut.animate().scaleX(1f).scaleY(1f).setDuration(120));



        Map<String, Object> data = new HashMap<>();
        data.put("pressedAt", FieldValue.serverTimestamp());

        // 문서 이름이 내 uid라서 다시 누르면 새로 안 생기고 덮어써짐 (1인 1표)
        db.collection(FirestorePaths.COURSES).document(courseId)
                .collection(FirestorePaths.SESSIONS).document(sessionId)
                .collection(FirestorePaths.CONFUSION).document(uid)
                .set(data)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "전달 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
        startTimer();
    }


    // 3분 타이머. 링 두개가 꽉 찼다가 같이 줄어듦
    private void startTimer() {
        // 이미 돌고 있으면 멈추고 처음부터
        if (timer != null) {
            timer.cancel();
        }

        // 누른 상태: 가운데 원만 살짝 주황으로
        binding.gyautFace.setBackgroundResource(R.drawable.bg_gyaut_pressed);

        timer = new CountDownTimer(FirestorePaths.CONFUSION_WINDOW_MS, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 남은 시간을 0~1000으로 바꿔서 링 길이로
                int progress = (int) (millisUntilFinished * 1000 / FirestorePaths.CONFUSION_WINDOW_MS);
                binding.ringTimer.setProgress(progress);
                binding.ringOuter.setProgress(progress);
            }

            @Override
            public void onFinish() {
                // 3분 끝 -> 링 다시 꽉 채우고 원 원래 색
                binding.ringTimer.setProgress(1000);
                binding.ringOuter.setProgress(1000);
                binding.gyautFace.setBackgroundResource(R.drawable.bg_gyaut_normal);
                timer = null;
            }
        };
        timer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 화면 닫히면 타이머도 멈추기
        if (timer != null) {
            timer.cancel();
        }
    }
}