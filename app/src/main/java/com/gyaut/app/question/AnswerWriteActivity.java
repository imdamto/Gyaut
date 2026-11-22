package com.gyaut.app.question;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gyaut.app.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AnswerWriteActivity extends AppCompatActivity {

    FirebaseFirestore db;

    String courseId;
    String questionId;

    TextView textQuestion;
    EditText editAnswer;
    Button buttonSaveAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_write);

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
        editAnswer = findViewById(R.id.editAnswer);
        buttonSaveAnswer = findViewById(R.id.buttonSaveAnswer);

        DocumentReference questionRef = db
                .collection("courses")
                .document(courseId)
                .collection("questions")
                .document(questionId);

        // 질문 및 기존 답변 불러오기
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

            // 이미 작성된 답변이 있다면 수정 가능
            if (answer != null) {
                editAnswer.setText(answer);
            }

        }).addOnFailureListener(e -> {
            Toast.makeText(this,
                    "질문 조회 실패",
                    Toast.LENGTH_SHORT).show();
        });

        // 답변 저장 버튼
        buttonSaveAnswer.setOnClickListener(v -> {

            String answer = editAnswer.getText()
                    .toString()
                    .trim();

            if (answer.isEmpty()) {
                Toast.makeText(this,
                        "답변을 입력하세요.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            questionRef.update("answer", answer)
                    .addOnSuccessListener(unused -> {

                        Toast.makeText(this,
                                "답변이 저장되었습니다.",
                                Toast.LENGTH_SHORT).show();

                        finish();
                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(this,
                                "답변 저장 실패",
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }
}