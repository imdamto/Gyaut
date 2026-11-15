package com.gyaut.app.realtime;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityClassBinding;
import com.gyaut.app.question.QuestionActivity;
import com.gyaut.app.question.QuestionListActivity;

public class ClassActivity extends AppCompatActivity {

    private ActivityClassBinding binding;

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

        // 앞 화면에서 과목, 수업 아이디 넘겨줬으면 그걸로 바꾸기
        if (getIntent().getStringExtra(IntentKeys.COURSE_ID) != null) {
            courseId = getIntent().getStringExtra(IntentKeys.COURSE_ID);
            sessionId = getIntent().getStringExtra(IntentKeys.SESSION_ID);
        }

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
}