package com.gyaut.app.question;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gyaut.app.R;

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
                        // 현재 로그인한 사용자 확인
                        if (auth.getCurrentUser() == null) {
                            Toast.makeText(this, "로그인이 필요합니다.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

// 질문 데이터 생성
                        Map<String, Object> question = new HashMap<>();

                        question.put("authorUid", auth.getCurrentUser().getUid());
                        question.put("content", content);
                        question.put("createdAt", FieldValue.serverTimestamp());
                        question.put("likeCount", 0);
                        question.put("sessionId", "test-session");
                        question.put("status", "visible");
                        question.put("topic", "RecyclerView");
                        question.put("week", 10);

// Firebase에 질문 저장
                        String courseId = "test-course";
                        db.collection("courses")
                                .document(courseId)
                                .collection("questions")
                                .add(question)
                                .addOnSuccessListener(documentReference -> {

                                    Toast.makeText(this, "질문이 등록되었습니다.",
                                            Toast.LENGTH_SHORT).show();

                                    editQuestion.setText("");

                                    Log.d("FirebaseTest",
                                            "문서 ID: " + documentReference.getId());

                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(this, "질문 등록 실패",
                                            Toast.LENGTH_SHORT).show();

                                    Log.e("FirebaseTest", "질문 저장 실패", e);

                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseTest", "로그인 실패", e);
                });
    }
}