package com.gyaut.app.realtime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.util.List;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gyaut.app.common.FirestorePaths;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityClassBinding;
import com.gyaut.app.question.QuestionActivity;
import com.gyaut.app.question.QuestionListActivity;

public class ClassActivity extends AppCompatActivity {

    private ActivityClassBinding binding;
    private FirebaseFirestore db;

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
}