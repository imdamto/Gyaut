package com.gyaut.app;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Map;
import java.util.HashMap;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import java.util.Map;
import java.util.HashMap;
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

                        String content = editQuestion.getText().toString().trim();

                        Log.d("ButtonTest", "입력된 내용: [" + content + "]");

                        if (content.isEmpty()) {
                            Log.d("ButtonTest", "입력값이 비어 있음");
                            editQuestion.setError("질문을 입력하세요.");
                            return;
                        }

                        Log.d("ButtonTest", "Firebase 저장 시작");


                        Map<String, Object> question = new HashMap<>();
                        question.put("content", content);
                        question.put("author", "student01");
                        question.put("likeCount", 0);
                        // 5. Firestore에 질문 저장
                        db.collection("questions")
                                .add(question)
                                .addOnSuccessListener(documentReference -> {

                                    Log.d("FirebaseTest", "저장 성공");

                                    Toast.makeText(MainActivity.this,
                                            "질문 등록 완료",
                                            Toast.LENGTH_SHORT).show();

                                    editQuestion.setText("");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirebaseTest", "저장 실패", e);
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseTest", "로그인 실패", e);
                });
    }
}