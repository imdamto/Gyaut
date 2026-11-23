package com.gyaut.app.course;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityCourseDetailBinding;

// 교수: 과목 하나의 상세 화면
public class CourseDetailActivity extends AppCompatActivity {

    private ActivityCourseDetailBinding binding;
    private FirebaseFirestore db;
    private String courseId;
    private ListenerRegistration courseListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        courseId = getIntent().getStringExtra(IntentKeys.COURSE_ID);
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

    // 과목 문서를 실시간으로 지켜봄 (학생이 등록하면 학생 수가 바로 바뀜)
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
        binding.tvTitle.setText(course.titleText());
        binding.tvInfo.setText(course.infoText());
        binding.tvJoinCode.setText(course.joinCode);
    }
}