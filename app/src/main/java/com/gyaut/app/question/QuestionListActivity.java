package com.gyaut.app.question;
import com.gyaut.app.R;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class QuestionListActivity extends AppCompatActivity {

    FirebaseFirestore db;

    RecyclerView recyclerQuestions;

    ArrayList<Question> questionList;

    QuestionAdapter adapter;

    ListenerRegistration listener;

    String courseId = "test-course";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_list);

        // 1. Firebase 초기화
        db = FirebaseFirestore.getInstance();

        // 2. RecyclerView 연결
        recyclerQuestions = findViewById(R.id.recyclerQuestions);

        recyclerQuestions.setLayoutManager(
                new LinearLayoutManager(this));

        // 3. 질문 목록 생성
        questionList = new ArrayList<>();

        // 4. Adapter 연결
        adapter = new QuestionAdapter(questionList);

        recyclerQuestions.setAdapter(adapter);

        // 5. Firebase 질문 불러오기
        loadQuestions();
    }

    private void loadQuestions() {

        listener = db.collection("courses")
                .document(courseId)
                .collection("questions")
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e("FirebaseTest",
                                "질문 조회 실패", error);

                        Toast.makeText(this,
                                "질문 조회 실패",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null) {
                        return;
                    }

                    // 기존 목록 초기화
                    questionList.clear();

                    // Firebase 질문 하나씩 가져오기
                    for (DocumentSnapshot document
                            : value.getDocuments()) {

                        Question question =
                                document.toObject(Question.class);

                        if (question != null) {

                            // 질문 문서 ID 저장
                            question.id = document.getId();

                            // 질문 목록에 추가
                            questionList.add(question);
                        }
                    }

                    // 화면 갱신
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (listener != null) {
            listener.remove();
        }
    }
}