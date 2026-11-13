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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gyaut.app.R;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.UserRole;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.course.CourseListActivity;
import com.gyaut.app.databinding.ActivitySignUpBinding;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6; // Firebase 최소 길이

    private ActivitySignUpBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnSignUp.setOnClickListener(v -> trySignUp());
        binding.textGoLogin.setOnClickListener(v -> finish()); // 로그인 화면으로 돌아가기
    }

    // 입력값 검사 → 계정 생성 → users/{uid} 저장
    private void trySignUp() {
        String name = textOf(binding.editName).trim();
        String email = textOf(binding.editEmail).trim();
        String password = textOf(binding.editPassword);
        String confirm = textOf(binding.editPasswordConfirm);

        binding.inputName.setError(null);
        binding.inputEmail.setError(null);
        binding.inputPassword.setError(null);
        binding.inputPasswordConfirm.setError(null);

        if (name.isEmpty()) {
            binding.inputName.setError("이름을 입력해 주세요");
            return;
        }
        if (email.isEmpty()) {
            binding.inputEmail.setError("이메일을 입력해 주세요");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.setError("이메일 형식이 아니에요");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            binding.inputPassword.setError("비밀번호를 6자 이상 입력해 주세요");
            return;
        }
        if (!password.equals(confirm)) {
            binding.inputPasswordConfirm.setError("비밀번호가 일치하지 않아요");
            return;
        }

        // 선택된 버튼으로 역할 결정
        String role = binding.toggleRole.getCheckedButtonId() == R.id.btnRoleProfessor
                ? UserRole.PROFESSOR : UserRole.STUDENT;

        setLoading(true);

        // 계정은 만들어졌는데 정보 저장만 실패해서 다시 누른 경우 → 저장만 다시 시도
        FirebaseUser current = auth.getCurrentUser();
        if (current != null && email.equals(current.getEmail())) {
            saveUser(current.getUid(), name, email, role);
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result ->
                        saveUser(result.getUser().getUid(), name, email, role))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        binding.inputEmail.setError(errorMessage(e));
                    } else {
                        Toast.makeText(this, errorMessage(e), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // users/{uid} 문서에 이름·이메일·역할 저장
    private void saveUser(String uid, String name, String email, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("role", role);

        db.collection(FirestorePaths.USERS).document(uid)
                .set(user)
                .addOnSuccessListener(unused -> goCourseList())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "정보 저장에 실패했어요. 가입하기를 다시 눌러 주세요",
                            Toast.LENGTH_LONG).show();
                });
    }

    // 과목 목록으로 이동하면서 로그인·회원가입 화면은 모두 닫음
    private void goCourseList() {
        Intent intent = new Intent(this, CourseListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        binding.btnSignUp.setEnabled(!loading);
        binding.btnSignUp.setText(loading ? "가입하는 중..." : "가입하기");
    }

    private String errorMessage(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "이미 가입된 이메일이에요";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "이메일 형식을 확인해 주세요";
        }
        if (e instanceof FirebaseNetworkException) {
            return "인터넷 연결을 확인해 주세요";
        }
        return "가입에 실패했어요: " + e.getMessage();
    }

    private String textOf(EditText editText) {
        Editable text = editText.getText();
        return text == null ? "" : text.toString();
    }
}