package com.gyaut.app.course;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.gyaut.app.auth.LoginActivity;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.UserRole;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.databinding.ActivityCourseListBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 내 과목 화면. 학생과 교수가 같이 쓰고, 역할에 따라 아래 버튼이 달라짐
public class CourseListActivity extends AppCompatActivity {

    private ActivityCourseListBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CourseAdapter adapter;
    private boolean professor; // 카드를 눌렀을 때 어디로 갈지 정할 때 씀

    // 실시간 구독. 화면이 안 보이게 되면 remove()로 끊어야 함
    private ListenerRegistration myCoursesListener;                                    // 내 과목 ID 목록
    private final Map<String, ListenerRegistration> courseListeners = new HashMap<>(); // 과목 ID → 그 과목 문서 구독
    private final Map<String, Course> courses = new HashMap<>();                       // 과목 ID → 최신 과목 정보

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCourseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        adapter = new CourseAdapter(course -> openCourse(course));
        binding.listCourses.setAdapter(adapter);

        binding.btnLogout.setOnClickListener(v -> confirmLogout());
        binding.btnAddCourse.setEnabled(false); // 역할을 알기 전에는 눌리지 않게

        loadUser(auth.getCurrentUser());
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenMyCourses();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListening();
    }

    // users/{uid}에서 이름과 역할 읽기
    private void loadUser(FirebaseUser user) {
        db.collection(FirestorePaths.USERS).document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    boolean professor = UserRole.PROFESSOR.equals(doc.getString("role"));
                    showUser(name != null ? name : user.getEmail(), professor);
                })
                .addOnFailureListener(e -> {
                    binding.tvUser.setText(user.getEmail());
                    Toast.makeText(this, "내 정보를 불러오지 못했어요", Toast.LENGTH_SHORT).show();
                });
    }

    // 역할에 맞게 이름 표시, 아래 버튼 글자·이동 화면, 빈 목록 안내, 참여 코드 표시 정하기
    private void showUser(String name, boolean professor) {
        this.professor = professor;
        binding.tvUser.setText(name + " · " + (professor ? "교수" : "학생"));

        binding.btnAddCourse.setText(professor ? "+ 과목 개설" : "+ 과목 등록");
        binding.btnAddCourse.setEnabled(true);
        binding.btnAddCourse.setOnClickListener(v -> startActivity(new Intent(this,
                professor ? CreateCourseActivity.class : JoinCourseActivity.class)));

        binding.textEmpty.setText(professor
                ? "아직 개설한 과목이 없어요\n아래 버튼으로 과목을 만들어 보세요"
                : "아직 등록한 과목이 없어요\n교수님께 받은 코드로 등록해 보세요");

        adapter.setProfessor(professor);
    }

    // 과목 카드를 눌렀을 때. 교수는 과목 상세 화면으로
    private void openCourse(Course course) {
        if (professor) {
            Intent intent = new Intent(this, CourseDetailActivity.class);
            intent.putExtra(IntentKeys.COURSE_ID, course.id);
            startActivity(intent);
        }
    }

    // users/{uid}/myCourses를 실시간으로 지켜봄 → 과목을 개설·등록하면 목록에 바로 나타남
    private void listenMyCourses() {
        myCoursesListener = db.collection(FirestorePaths.USERS).document(auth.getCurrentUser().getUid())
                .collection(FirestorePaths.MY_COURSES)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "과목 목록을 불러오지 못했어요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 문서 ID가 곧 과목 ID. 처음 보는 과목만 구독 시작
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String courseId = doc.getId();
                        if (!courseListeners.containsKey(courseId)) {
                            courseListeners.put(courseId, listenCourse(courseId));
                        }
                    }
                    showCourses();
                });
    }

    // courses/{courseId} 문서 하나를 실시간으로 지켜봄 (학생 수 등이 바뀌면 바로 반영)
    private ListenerRegistration listenCourse(String courseId) {
        return db.collection(FirestorePaths.COURSES).document(courseId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || !doc.exists()) {
                        return;
                    }
                    courses.put(courseId, Course.from(doc));
                    showCourses();
                });
    }

    // 지금까지 받은 과목들을 이름순으로 정렬해서 표시
    private void showCourses() {
        List<Course> list = new ArrayList<>(courses.values());
        Collections.sort(list, (a, b) -> a.name.compareTo(b.name));
        adapter.setCourses(list);
        binding.textEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // 실시간 구독을 모두 끊기
    private void stopListening() {
        if (myCoursesListener != null) {
            myCoursesListener.remove();
            myCoursesListener = null;
        }
        for (ListenerRegistration listener : courseListeners.values()) {
            listener.remove();
        }
        courseListeners.clear();
        courses.clear();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("로그아웃할까요?")
                .setPositiveButton("로그아웃", (dialog, which) -> logout())
                .setNegativeButton("취소", null)
                .show();
    }

    // 로그아웃하고 로그인 화면으로. 지금까지 열린 화면은 모두 닫음
    private void logout() {
        stopListening(); // 로그아웃 뒤에 구독이 남아 있으면 권한 오류가 남
        auth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}