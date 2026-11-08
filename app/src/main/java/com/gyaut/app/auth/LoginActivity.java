package com.gyaut.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.gyaut.app.databinding.ActivityLoginBinding;
import com.gyaut.app.question.QuestionListActivity;
import com.gyaut.app.realtime.ClassActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // TODO(계정·과목 담당): 로그인 화면 다 만들면 임시 버튼 두 개와 testLoginThenGo() 삭제
        binding.btnGoClass.setOnClickListener(v -> testLoginThenGo(ClassActivity.class));
        binding.btnGoQuestion.setOnClickListener(v -> testLoginThenGo(QuestionListActivity.class));
    }

    // 테스트 학생 계정으로 로그인한 다음 그 화면으로 이동
    private void testLoginThenGo(Class<?> target) {
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("student1@gyaut.test", "test1234")
                .addOnSuccessListener(r -> startActivity(new Intent(this, target)))
                .addOnFailureListener(e -> Toast.makeText(this,
                        "테스트 로그인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}