package com.gyaut.app.question;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gyaut.app.R;
import java.util.HashMap;
import java.util.Map;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.Toast;
import java.util.ArrayList;
public class Question_StudentActivity extends AppCompatActivity {
    FirebaseFirestore db;
    RecyclerView recyclerQuestions;
    String courseId;
    ArrayList<Question> questionList;
    QuestionAdapter adapter;
    ListenerRegistration listener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 질문 입력 화면 연결
        setContentView(R.layout.activity_question);
        courseId = getIntent().getStringExtra("courseId");
        // 1. 화면의 입력창과 버튼 연결
        EditText editQuestion = findViewById(R.id.editQuestion);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        // 2. Firebase 객체 생성
        FirebaseAuth auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 로그인 전에는 버튼 비활성화
        btnSubmit.setEnabled(false);

        // 3. Firebase 익명 로그인
        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {

                    Log.d("FirebaseTest", "로그인 성공");

                    btnSubmit.setEnabled(true);

                    // 4. 버튼 클릭 이벤트
                    btnSubmit.setOnClickListener(v -> {
                        checkWarningBeforeSubmit();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseTest", "로그인 실패", e);
                });
        recyclerQuestions = findViewById(R.id.recyclerQuestions);

        recyclerQuestions.setLayoutManager(
                new LinearLayoutManager(this));

        questionList = new ArrayList<>();
        adapter = new QuestionAdapter(
                questionList,
                new QuestionAdapter.OnQuestionClickListener() {

                    @Override
                    public void onDeleteClick(Question question) {
                        // 학생용에서는 사용하지 않음
                    }

                    @Override
                    public void onHideClick(Question question) {
                        // 학생용에서는 사용하지 않음
                    }

                    @Override
                    public void onLikeClick(Question question) {
                        toggleLike(question);
                    }
                    @Override
                    public void onAnswerClick(Question question) {

                        Intent intent = new Intent(
                                Question_StudentActivity.this,
                                AnswerViewActivity.class
                        );

                        intent.putExtra("courseId", courseId);
                        intent.putExtra("questionId", question.id);

                        startActivity(intent);
                    }
                },
                false
        );
        recyclerQuestions.setAdapter(adapter);
        loadQuestions();
    }
    private void submitQuestion() {

        EditText editQuestion = findViewById(R.id.editQuestion);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        String content = editQuestion.getText()
                .toString().trim();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> question = new HashMap<>();

        question.put("authorUid", auth.getCurrentUser().getUid());
        question.put("content", content);
        question.put("createdAt", FieldValue.serverTimestamp());
        question.put("likeCount", 0);
        question.put("sessionId", "test-session");
        question.put("status", "visible");
        question.put("topic", "RecyclerView");
        question.put("week", 10);

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
    }
    private void loadQuestions() {

        listener = db.collection("courses")
                .document(courseId)
                .collection("questions")
                .whereEqualTo("status", "visible")
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e("FirebaseTest",
                                "질문 조회 실패", error);
                        return;
                    }

                    if (value == null) {
                        return;
                    }

                    questionList.clear();

                    for (DocumentSnapshot document
                            : value.getDocuments()) {

                        Question question =
                                document.toObject(Question.class);

                        if (question != null) {

                            question.id = document.getId();

                            questionList.add(question);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }
    private void toggleLike(Question question) {

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this,
                    "로그인이 필요합니다.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 공감할 질문의 Firebase 문서
        DocumentReference questionRef = db
                .collection("courses")
                .document(courseId)
                .collection("questions")
                .document(question.id);

        db.runTransaction(transaction -> {

            // 현재 Firebase에 저장된 질문 조회
            DocumentSnapshot snapshot =
                    transaction.get(questionRef);

            // 공감한 사용자 목록
            List<?> likedUsers =
                    (List<?>) snapshot.get("likedUsers");

            // 현재 공감 수
            Long count = snapshot.getLong("likeCount");

            long likeCount = count != null ? count : 0;

            // 이미 공감했는지 확인
            boolean isLiked =
                    likedUsers != null &&
                            likedUsers.contains(uid);

            if (isLiked) {

                // 공감 취소
                transaction.update(questionRef,
                        "likedUsers",
                        FieldValue.arrayRemove(uid));

                transaction.update(questionRef,
                        "likeCount",
                        Math.max(0, likeCount - 1));

            } else {

                // 공감 추가
                transaction.update(questionRef,
                        "likedUsers",
                        FieldValue.arrayUnion(uid));

                transaction.update(questionRef,
                        "likeCount",
                        likeCount + 1);
            }

            return null;

        }).addOnSuccessListener(unused -> {

            Toast.makeText(this,
                    "공감 상태가 변경되었습니다.",
                    Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e -> {

            Toast.makeText(this,
                    "공감 처리 실패",
                    Toast.LENGTH_SHORT).show();

            Log.e("FirebaseTest", "공감 처리 실패", e);
        });
    }
    private void showWarningDialog() {

        View view = getLayoutInflater()
                .inflate(R.layout.dialog_warning, null);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(view)
                        .create();

        Button buttonConfirm =
                view.findViewById(R.id.buttonConfirm);

        buttonConfirm.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }
    private void showBlockedDialog() {
        View view = getLayoutInflater()
                .inflate(R.layout.dialog_blocked, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        Button buttonConfirm =
                view.findViewById(R.id.buttonBlockedConfirm);

        buttonConfirm.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    private void checkWarningBeforeSubmit() {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null || courseId == null) {
            Toast.makeText(this, "사용자 또는 강의 정보를 확인할 수 없습니다.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("courses")
                .document(courseId)
                .collection("members")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(this, "강의 수강 정보를 찾을 수 없습니다.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Long count = document.getLong("warningCount");
                    long warningCount = count == null ? 0 : count;

                    if (warningCount >= 2) {
                        showBlockedDialog();
                        return;
                    }

                    submitQuestion();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "경고 정보를 확인하지 못했습니다.",
                                Toast.LENGTH_SHORT).show());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (listener != null) {
            listener.remove();
        }
    }
}