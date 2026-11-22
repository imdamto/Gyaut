package com.gyaut.app.course;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.UserRole;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityJoinCourseBinding;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// 학생: 교수님께 받은 참여 코드로 과목을 찾아 등록하는 화면
public class JoinCourseActivity extends AppCompatActivity {

    private static final int CODE_LENGTH = 6;

    private ActivityJoinCourseBinding binding;
    private FirebaseFirestore db;
    private String uid;

    private Course found; // 코드로 찾은 과목. 카드가 숨겨져 있으면 null

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityJoinCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        binding.btnFind.setOnClickListener(v -> findCourse());
        binding.btnJoin.setOnClickListener(v -> tryJoin());
    }

    // joinCode가 입력한 코드와 같은 과목 찾기
    private void findCourse() {
        // 붙여 넣기 등으로 소문자가 들어와도 찾을 수 있게 대문자로 바꿈
        String code = textOf(binding.editCode).trim().toUpperCase(Locale.ROOT);

        binding.inputCode.setError(null);
        showFound(null);

        if (code.length() != CODE_LENGTH) {
            binding.inputCode.setError("코드 6자리를 입력해 주세요");
            return;
        }

        setFinding(true);
        db.collection(FirestorePaths.COURSES)
                .whereEqualTo("joinCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    setFinding(false);
                    if (snapshot.isEmpty()) {
                        binding.inputCode.setError("이 코드의 과목이 없어요. 코드를 다시 확인해 주세요");
                        return;
                    }
                    showFound(Course.from(snapshot.getDocuments().get(0)));
                })
                .addOnFailureListener(e -> {
                    setFinding(false);
                    Toast.makeText(this, "과목을 찾지 못했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show();
                });
    }

    // 찾은 과목을 카드로 보여 줌. null이면 카드를 숨김
    private void showFound(Course course) {
        found = course;
        if (course == null) {
            binding.cardFound.setVisibility(View.GONE);
            return;
        }
        binding.tvFoundName.setText(course.titleText());
        binding.tvFoundInfo.setText(course.infoText());
        binding.cardFound.setVisibility(View.VISIBLE);
    }

    // 이미 등록한 과목인지 먼저 확인
    private void tryJoin() {
        Course course = found;
        DocumentReference myCourseRef = db.collection(FirestorePaths.USERS).document(uid)
                .collection(FirestorePaths.MY_COURSES).document(course.id);

        setJoining(true);
        myCourseRef.get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        setJoining(false);
                        Toast.makeText(this, "이미 등록한 과목이에요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveJoin(course, myCourseRef);
                })
                .addOnFailureListener(e -> failJoin());
    }

    // 내 이름을 먼저 읽음. 교수 상세 화면의 학생 목록에 보여 줄 이름
    private void saveJoin(Course course, DocumentReference myCourseRef) {
        db.collection(FirestorePaths.USERS).document(uid)
                .get()
                .addOnSuccessListener(userDoc -> commitJoin(course, myCourseRef, userDoc.getString("name")))
                .addOnFailureListener(e -> failJoin());
    }

    // 멤버 추가, 내 과목 추가, 학생 수 +1을 한 번에 저장
    private void commitJoin(Course course, DocumentReference myCourseRef, String name) {
        DocumentReference courseRef = db.collection(FirestorePaths.COURSES).document(course.id);

        Map<String, Object> member = new HashMap<>();
        member.put("role", UserRole.STUDENT);
        member.put("name", name);
        member.put("warningCount", 0);

        WriteBatch batch = db.batch();
        batch.set(courseRef.collection(FirestorePaths.MEMBERS).document(uid), member);
        batch.set(myCourseRef, new HashMap<>()); // 문서 ID(과목 ID)만 있으면 됨
        batch.update(courseRef, "studentCount", FieldValue.increment(1));

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, course.titleText() + " 과목을 등록했어요", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> failJoin());
    }

    private void failJoin() {
        setJoining(false);
        Toast.makeText(this, "등록하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show();
    }

    private void setFinding(boolean finding) {
        binding.btnFind.setEnabled(!finding);
        binding.btnFind.setText(finding ? "찾는 중..." : "과목 찾기");
    }

    private void setJoining(boolean joining) {
        binding.btnJoin.setEnabled(!joining);
        binding.btnFind.setEnabled(!joining);
        binding.btnJoin.setText(joining ? "등록하는 중..." : "등록하기");
    }

    private String textOf(EditText editText) {
        Editable text = editText.getText();
        return text == null ? "" : text.toString();
    }
}