package com.gyaut.app.question;
import com.gyaut.app.R;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;

public class Question_ProfessorActivity extends AppCompatActivity {

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
        adapter = new QuestionAdapter(
                questionList,
                new QuestionAdapter.OnQuestionClickListener() {

                    @Override
                    public void onDeleteClick(Question question) {
                        showDeleteDialog(question);
                    }
                    @Override
                    public void onHideClick(Question question) {
                        toggleQuestionStatus(question);
                    }
                    @Override
                    public void onWarningClick(Question question) {
                        giveWarning(question.authorUid);
                    }
                    @Override
                    public void onAnswerClick(Question question) {

                        Intent intent = new Intent(
                                Question_ProfessorActivity.this,
                                AnswerWriteActivity.class
                        );

                        intent.putExtra("courseId", courseId);
                        intent.putExtra("questionId", question.id);

                        startActivity(intent);
                    }
                },
                true
        );
        recyclerQuestions.setAdapter(adapter);

        // 5. Firebase 질문 불러오기
        loadQuestions();
    }
    private void giveWarning(String studentUid) {

        if (studentUid == null || studentUid.isEmpty()) {
            Toast.makeText(this, "학생 정보를 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        DocumentReference userRef = db.collection("courses")
                .document(courseId)
                .collection("members")
                .document(studentUid);
        db.runTransaction(transaction -> {

            DocumentSnapshot snapshot = transaction.get(userRef);

            if (!snapshot.exists()) {
                throw new IllegalStateException("학생 문서가 없습니다.");
            }

            Long count = snapshot.getLong("warning");

            long currentCount = count != null ? count : 0;
            long newCount = Math.min(currentCount + 1, 2);

            transaction.update(userRef, "warning", newCount);

            return newCount;

        }).addOnSuccessListener(count -> {

            Toast.makeText(this,
                    "경고 " + count + "회 부여 완료",
                    Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e -> {

            Toast.makeText(this,
                    "경고 처리 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();

        });
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
    private void showDeleteDialog(Question question) {

        new AlertDialog.Builder(this)
                .setTitle("질문 삭제")
                .setMessage("해당 질문을 삭제하시겠습니까?\n\n"
                        + question.content)

                .setNegativeButton("취소", (dialog, which) -> {
                    dialog.dismiss();
                })

                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteQuestion(question.id);
                })

                .show();
    }
    private void deleteQuestion(String questionId) {

        db.collection("courses")
                .document(courseId)
                .collection("questions")
                .document(questionId)
                .delete()

                .addOnSuccessListener(unused -> {

                    Toast.makeText(this,
                            "질문이 삭제되었습니다.",
                            Toast.LENGTH_SHORT).show();
                })

                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "질문 삭제 실패",
                            Toast.LENGTH_SHORT).show();

                    Log.e("FirebaseTest", "삭제 실패", e);
                });
    }
    private void toggleQuestionStatus(Question question) {

        String newStatus;

        // 현재 공개 상태라면 숨김으로 변경
        if ("visible".equals(question.status)) {
            newStatus = "hidden";
        } else {
            newStatus = "visible";
        }

        db.collection("courses")
                .document(courseId)
                .collection("questions")
                .document(question.id)

                .update("status", newStatus)

                .addOnSuccessListener(unused -> {

                    if ("hidden".equals(newStatus)) {

                        Toast.makeText(this,
                                "질문이 숨김 처리되었습니다.",
                                Toast.LENGTH_SHORT).show();

                    } else {

                        Toast.makeText(this,
                                "질문이 다시 공개되었습니다.",
                                Toast.LENGTH_SHORT).show();
                    }
                })

                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "상태 변경 실패",
                            Toast.LENGTH_SHORT).show();

                    Log.e("FirebaseTest", "상태 변경 실패", e);
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