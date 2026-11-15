package com.gyaut.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gyaut.app.R;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.UserRole;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.course.CourseListActivity;
import com.gyaut.app.databinding.ActivityLoginBinding;
import com.gyaut.app.databinding.DialogSelectRoleBinding;
import com.gyaut.app.question.QuestionListActivity;
import com.gyaut.app.realtime.ClassActivity;
import com.gyaut.app.realtime.PodiumActivity;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

        // 로그인 버튼
        binding.btnLogin.setOnClickListener(v -> tryLogin());

        // 구글 로그인 버튼
        binding.btnGoogle.setOnClickListener(v -> signInWithGoogle());

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
                .addOnSuccessListener(result -> goCourseList())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, errorMessage(e), Toast.LENGTH_LONG).show();
                });
    }

    // 폰에 있는 구글 계정 중 하나를 고르는 창 띄우기
    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // 폰에 있는 모든 구글 계정 보여 주기
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        setLoading(true);
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredential(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        setLoading(false);
                        if (e instanceof GetCredentialCancellationException) {
                            return; // 사용자가 창을 닫은 것
                        }
                        String message = (e instanceof NoCredentialException)
                                ? "이 기기에 로그인된 구글 계정이 없어요"
                                : "구글 로그인에 실패했어요";
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // 고른 계정의 토큰으로 Firebase 로그인
    private void handleGoogleCredential(Credential credential) {
        GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(credential.getData());
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.getIdToken(), null);

        auth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener(result -> checkUserDoc(result.getUser()))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String message = (e instanceof FirebaseNetworkException)
                            ? "인터넷 연결을 확인해 주세요"
                            : "구글 로그인에 실패했어요";
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
    }

    // users/{uid} 문서가 있으면 과목 목록으로, 없으면(처음 온 사람) 역할 묻기
    private void checkUserDoc(FirebaseUser user) {
        db.collection(FirestorePaths.USERS).document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        goCourseList();
                    } else {
                        askRole(user);
                    }
                })
                .addOnFailureListener(e -> {
                    undoGoogleLogin();
                    Toast.makeText(this, "구글 로그인에 실패했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show();
                });
    }

    // 처음 온 사람에게 학생/교수 묻기
    private void askRole(FirebaseUser user) {
        DialogSelectRoleBinding roleBinding = DialogSelectRoleBinding.inflate(getLayoutInflater());

        new MaterialAlertDialogBuilder(this)
                .setTitle("반가워요, " + user.getDisplayName() + " 님")
                .setMessage("학생인지 교수인지 골라 주세요")
                .setView(roleBinding.getRoot())
                .setPositiveButton("시작하기", (dialog, which) -> {
                    String role = roleBinding.toggleRole.getCheckedButtonId() == R.id.btnRoleProfessor
                            ? UserRole.PROFESSOR : UserRole.STUDENT;
                    saveUser(user, role);
                })
                .setNegativeButton("취소", (dialog, which) -> undoGoogleLogin())
                .setCancelable(false)
                .show();
    }

    // users/{uid}에 구글 이름·이메일·역할 저장
    private void saveUser(FirebaseUser user, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", user.getDisplayName());
        data.put("email", user.getEmail());
        data.put("role", role);

        db.collection(FirestorePaths.USERS).document(user.getUid())
                .set(data)
                .addOnSuccessListener(unused -> goCourseList())
                .addOnFailureListener(e -> {
                    undoGoogleLogin();
                    Toast.makeText(this, "정보 저장에 실패했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show();
                });
    }

    // 역할 저장까지 못 끝냈으면 로그인 상태를 되돌림
    private void undoGoogleLogin() {
        auth.signOut();
        setLoading(false);
    }

    // 과목 목록으로 이동하고 로그인 화면은 닫기 (뒤로 가기로 돌아오지 않게)
    private void goCourseList() {
        startActivity(new Intent(this, CourseListActivity.class));
        finish();
    }

    // 로그인 중에는 버튼을 막아서 여러 번 눌리지 않게
    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogle.setEnabled(!loading);
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