package com.gyaut.app.course;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityCourseDetailBinding;

// 교수: 과목 하나의 상세 화면. 여기서 수업을 시작하고 끝내고, 과목을 수정·삭제함
public class CourseDetailActivity extends AppCompatActivity {

    // ⋮ 메뉴 항목 번호
    private static final int MENU_EDIT = 1;
    private static final int MENU_DELETE = 2;

    private ActivityCourseDetailBinding binding;
    private FirebaseFirestore db;
    private String courseId;
    private ListenerRegistration courseListener;
    private SessionStarter sessionStarter;

    private Course course; // 지금 보고 있는 과목. 과목 문서가 바뀔 때마다 갱신

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        courseId = getIntent().getStringExtra(IntentKeys.COURSE_ID);
        sessionStarter = new SessionStarter(this);

        binding.btnSession.setOnClickListener(v -> onSessionClick());
        binding.btnEndSession.setOnClickListener(v -> confirmEndSession());
        binding.btnCopyCode.setOnClickListener(v -> copyCode());
        binding.btnMore.setOnClickListener(v -> showMenu());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenCourse();
    }

    @Override
    protected void onStop() {
        super.onStop();
        courseListener.remove();
    }

    // 과목 문서를 실시간으로 지켜봄 (학생 수, 수업 진행 여부, 수정한 내용이 바로 반영)
    private void listenCourse() {
        courseListener = db.collection(FirestorePaths.COURSES).document(courseId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || !doc.exists()) {
                        return;
                    }
                    showCourse(Course.from(doc));
                });
    }

    private void showCourse(Course course) {
        this.course = course;
        binding.tvTitle.setText(course.titleText());
        binding.tvInfo.setText(course.infoText());
        binding.tvJoinCode.setText(course.joinCode);

        boolean live = course.liveSessionId != null;
        binding.btnSession.setText(live ? "교탁 모드 열기" : "수업 시작");
        binding.btnSession.setEnabled(true);
        binding.btnEndSession.setVisibility(live ? View.VISIBLE : View.GONE);
        binding.btnCopyCode.setEnabled(true);
    }

    // ⋮ 메뉴: 과목 수정, 과목 삭제
    private void showMenu() {
        PopupMenu menu = new PopupMenu(this, binding.btnMore);
        menu.getMenu().add(0, MENU_EDIT, 0, "수정");
        menu.getMenu().add(0, MENU_DELETE, 1, "삭제");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_EDIT) {
                Intent intent = new Intent(this, CreateCourseActivity.class);
                intent.putExtra(IntentKeys.COURSE_ID, courseId); // 과목 ID를 넘기면 수정 모드
                startActivity(intent);
            } else {
                confirmDelete();
            }
            return true;
        });
        menu.show();
    }

    // 수업 중이면 교탁 모드로, 아니면 수업 시작
    private void onSessionClick() {
        if (course.liveSessionId != null) {
            SessionStarter.openPodium(this, courseId, course.liveSessionId);
        } else {
            sessionStarter.start(courseId);
        }
    }

    private void confirmEndSession() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("수업을 종료할까요?")
                .setPositiveButton("종료", (dialog, which) -> endSession())
                .setNegativeButton("취소", null)
                .show();
    }

    // 과목 문서에서 진행 중인 수업 ID를 지움 → 모든 화면에서 LIVE가 사라짐
    private void endSession() {
        db.collection(FirestorePaths.COURSES).document(courseId)
                .update("liveSessionId", FieldValue.delete())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "수업을 종료하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show());
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("과목을 삭제할까요?")
                .setPositiveButton("삭제", (dialog, which) -> deleteCourse())
                .setNegativeButton("취소", null)
                .show();
    }

    // 과목 문서, 내 멤버 문서, 내 과목 목록 문서를 한 번에 삭제
    private void deleteCourse() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference courseRef = db.collection(FirestorePaths.COURSES).document(courseId);

        WriteBatch batch = db.batch();
        batch.delete(courseRef);
        batch.delete(courseRef.collection(FirestorePaths.MEMBERS).document(uid));
        batch.delete(db.collection(FirestorePaths.USERS).document(uid)
                .collection(FirestorePaths.MY_COURSES).document(courseId));

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "과목을 삭제했어요", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "과목을 삭제하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show());
    }

    // 참여 코드를 클립보드에 복사 (학생들에게 단톡방 등으로 보낼 때)
    private void copyCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("참여 코드", course.joinCode));
        Toast.makeText(this, "코드를 복사했어요", Toast.LENGTH_SHORT).show();
    }
}