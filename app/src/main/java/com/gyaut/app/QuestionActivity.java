package com.gyaut.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class QuestionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 질문 입력 화면 연결
        setContentView(R.layout.activity_question);

        // 1. 화면의 입력창과 버튼 연결
        EditText editQuestion = findViewById(R.id.editQuestion);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        // 2. Firebase 객체 생성
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 로그인 전에는 버튼 비활성화
        btnSubmit.setEnabled(false);

        // 3. Firebase 익명 로그인
        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {

                    Log.d("FirebaseTest", "로그인 성공");

                    btnSubmit.setEnabled(true);

                    // 4. 버튼 클릭 이벤트
                    btnSubmit.setOnClickListener(v -> {

                        Log.d("ButtonTest", "버튼 클릭됨");

                        String content = editQuestion.getText()
                                .toString().trim();

                        Log.d("ButtonTest", "입력된 내용: [" + content + "]");

                        // 입력값 검사
                        if (content.isEmpty()) {
                            Log.d("ButtonTest", "입력값이 비어 있음");
                            editQuestion.setError("질문을 입력하세요.");
                            return;
                        }

                        Log.d("ButtonTest", "Firebase 저장 시작");

                        // 5. 질문 데이터 생성
                        Map<String, Object> question = new HashMap<>();

                        question.put("content", content);
                        question.put("author", "student01");
                        question.put("likeCount", 0);

                        // 6. Firestore에 질문 저장
                        db.collection("questions")
                                .add(question)
                                .addOnSuccessListener(documentReference -> {

                                    Log.d("FirebaseTest", "질문 저장 성공");
                                    Log.d("FirebaseTest", "문서 ID: "
                                            + documentReference.getId());

                                    Toast.makeText(QuestionActivity.this,
                                            "질문이 등록되었습니다.",
                                            Toast.LENGTH_SHORT).show();

                                    // 입력창 초기화
                                    editQuestion.setText("");
                                })
                                .addOnFailureListener(e -> {

                                    Log.e("FirebaseTest", "질문 저장 실패", e);

                                    Toast.makeText(QuestionActivity.this,
                                            "질문 등록에 실패했습니다.",
                                            Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseTest", "로그인 실패", e);
                });
    }
}