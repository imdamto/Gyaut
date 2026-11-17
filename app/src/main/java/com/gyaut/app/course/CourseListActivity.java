package com.gyaut.app.course;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gyaut.app.auth.LoginActivity;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.UserRole;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityCourseListBinding;

// 내 과목 화면. 학생과 교수가 같이 쓰고, 역할에 따라 아래 버튼이 달라짐
public class CourseListActivity extends AppCompatActivity {

    private ActivityCourseListBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCourseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.btnLogout.setOnClickListener(v -> confirmLogout());
        binding.btnAddCourse.setEnabled(false); // 역할을 알기 전에는 눌리지 않게

        loadUser(auth.getCurrentUser());
    }

    // users/{uid}에서 이름과 역할 읽기
    private void loadUser(FirebaseUser user) {
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.USERS).document(user.getUid())
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

    // 역할에 맞게 이름 표시, 아래 버튼 글자,이동 화면, 빈 목록 안내 정하기
    private void showUser(String name, boolean professor) {
        binding.tvUser.setText(name + " · " + (professor ? "교수" : "학생"));

        binding.btnAddCourse.setText(professor ? "+ 과목 개설" : "+ 과목 등록");
        binding.btnAddCourse.setEnabled(true);
        binding.btnAddCourse.setOnClickListener(v -> startActivity(new Intent(this,
                professor ? CreateCourseActivity.class : JoinCourseActivity.class)));

        binding.textEmpty.setText(professor
                ? "아직 개설한 과목이 없어요\n아래 버튼으로 과목을 만들어 보세요"
                : "아직 등록한 과목이 없어요\n교수님께 받은 코드로 등록해 보세요");
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
        auth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}