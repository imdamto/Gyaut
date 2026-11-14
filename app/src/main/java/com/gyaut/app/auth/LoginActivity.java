package com.gyaut.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.course.CourseListActivity;
import com.gyaut.app.databinding.ActivityLoginBinding;
import com.gyaut.app.question.QuestionListActivity;
import com.gyaut.app.realtime.ClassActivity;
import com.gyaut.app.realtime.PodiumActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // 로그인 버튼
        binding.btnLogin.setOnClickListener(v -> tryLogin());

        // "회원가입" 글자 → 회원가입 화면으로
        binding.textGoSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        // TODO: 과목 → 수업 화면 연결되면 임시 버튼 세 개와 testLoginThenGo() 삭제 예정
        binding.btnGoClass.setOnClickListener(v -> testLoginThenGo("student1@gyaut.test", ClassActivity.class));
        binding.btnGoQuestion.setOnClickListener(v -> testLoginThenGo("student1@gyaut.test", QuestionListActivity.class));
        binding.btnGoPodium.setOnClickListener(v -> testLoginThenGo("prof1@gyaut.test", PodiumActivity.class));
    }

    // 입력값 검사 후 Firebase 로그인
    private void tryLogin() {
        String email = textOf(binding.editEmail).trim();
        String password = textOf(binding.editPassword);

        binding.inputEmail.setError(null);
        binding.inputPassword.setError(null);

        if (email.isEmpty()) {
            binding.inputEmail.setError("이메일을 입력해 주세요");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.setError("이메일 형식이 아니에요");
            return;
        }
        if (password.isEmpty()) {
            binding.inputPassword.setError("비밀번호를 입력해 주세요");
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    startActivity(new Intent(this, CourseListActivity.class));
                    finish(); // 뒤로 가기로 로그인 화면에 돌아오지 않게
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, errorMessage(e), Toast.LENGTH_LONG).show();
                });
    }

    // 로그인 중에는 버튼을 막아서 여러 번 눌리지 않게
    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? "로그인 중..." : "로그인");
    }

    // Firebase 에러를 사용자가 알아들을 수 있는 문장으로
    private String errorMessage(Exception e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException) {
            return "이메일 또는 비밀번호가 맞지 않아요";
        }
        if (e instanceof FirebaseNetworkException) {
            return "인터넷 연결을 확인해 주세요";
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return "시도가 너무 많아요. 잠시 후 다시 해 주세요";
        }
        return "로그인에 실패했어요: " + e.getMessage();
    }

    // 입력창 글자 꺼내기 (비어 있으면 "")
    private String textOf(EditText editText) {
        Editable text = editText.getText();
        return text == null ? "" : text.toString();
    }

    // 테스트 계정으로 로그인한 다음 그 화면으로 이동
    private void testLoginThenGo(String email, Class<?> target) {
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, "test1234")
                .addOnSuccessListener(r -> startActivity(new Intent(this, target)))
                .addOnFailureListener(e -> Toast.makeText(this,
                        "테스트 로그인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}