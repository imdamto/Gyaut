package com.gyaut.app.question;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gyaut.app.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AnswerViewActivity extends AppCompatActivity {

    FirebaseFirestore db;

    String courseId;
    String questionId;

    TextView textQuestion;
    TextView textAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_view);

        db = FirebaseFirestore.getInstance();

        courseId = getIntent().getStringExtra("courseId");
        questionId = getIntent().getStringExtra("questionId");

        if (courseId == null || questionId == null) {
            Toast.makeText(this,
                    "질문 정보가 없습니다.",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textQuestion = findViewById(R.id.textQuestion);
        textAnswer = findViewById(R.id.textAnswer);

        findViewById(R.id.buttonBack)
                .setOnClickListener(v -> finish());

        DocumentReference questionRef = db
                .collection("courses")
                .document(courseId)
                .collection("questions")
                .document(questionId);

        // Firebase에서 답변 조회
        questionRef.get().addOnSuccessListener(document -> {

            if (!document.exists()) {
                Toast.makeText(this,
                        "질문이 존재하지 않습니다.",
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String content = document.getString("content");
            String answer = document.getString("answer");

            textQuestion.setText(
                    content != null ? content : ""
            );

            if (answer == null || answer.trim().isEmpty()) {
                textAnswer.setText(
                        "아직 교수님의 답변이 등록되지 않았습니다."
                );
            } else {
                textAnswer.setText(answer);
            }

        }).addOnFailureListener(e -> {

            Toast.makeText(this,
                    "답변 조회 실패",
                    Toast.LENGTH_SHORT).show();
        });
    }
}