package com.gyaut.app.course;

import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityCourseDetailBinding;

// 교수: 과목 하나의 상세 화면. 여기서 수업을 시작함
public class CourseDetailActivity extends AppCompatActivity {

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
        binding.btnCopyCode.setOnClickListener(v -> copyCode());
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

    // 과목 문서를 실시간으로 지켜봄 (학생 수, 수업 진행 여부가 바뀌면 바로 반영)
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

        binding.btnSession.setText(course.liveSessionId != null ? "교탁 모드 열기" : "수업 시작");
        binding.btnSession.setEnabled(true);
        binding.btnCopyCode.setEnabled(true);
    }

    // 수업 중이면 교탁 모드로, 아니면 수업 시작
    private void onSessionClick() {
        if (course.liveSessionId != null) {
            SessionStarter.openPodium(this, courseId, course.liveSessionId);
        } else {
            sessionStarter.start(courseId);
        }
    }

    // 참여 코드를 클립보드에 복사 (학생들에게 단톡방 등으로 보낼 때)
    private void copyCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("참여 코드", course.joinCode));
        Toast.makeText(this, "코드를 복사했어요", Toast.LENGTH_SHORT).show();
    }
}