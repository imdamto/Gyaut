package com.gyaut.app.course;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.gyaut.app.R;
import com.gyaut.app.auth.LoginActivity;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.UserRole;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityCourseListBinding;
import com.gyaut.app.realtime.ClassActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// 내 과목 화면. 학생과 교수가 같이 쓰고, 역할에 따라 + 버튼이 여는 화면이 달라짐
public class CourseListActivity extends AppCompatActivity {

    private ActivityCourseListBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CourseAdapter adapter;
    private SessionStarter sessionStarter;
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
        sessionStarter = new SessionStarter(this);

        adapter = new CourseAdapter(course -> openCourse(course), course -> onCourseLongClick(course));
        binding.listCourses.setAdapter(adapter);

        binding.btnMore.setOnClickListener(v -> showMenu());
        binding.btnAdd.setEnabled(false); // 역할을 알기 전에는 눌리지 않게

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

    // 역할에 맞게 이름 표시, + 버튼이 열 화면, 빈 목록 안내, 분반 표시, 다음 수업 카드 버튼 정하기
    private void showUser(String name, boolean professor) {
        this.professor = professor;
        binding.tvUser.setText(name + " · " + (professor ? "교수" : "학생"));

        binding.btnAdd.setEnabled(true);
        binding.btnAdd.setContentDescription(professor ? "과목 개설" : "과목 등록");
        binding.btnAdd.setOnClickListener(v -> startActivity(new Intent(this,
                professor ? CreateCourseActivity.class : JoinCourseActivity.class)));

        binding.textEmpty.setText(professor
                ? "아직 개설한 과목이 없어요\n오른쪽 위 + 로 과목을 만들어 보세요"
                : "아직 등록한 과목이 없어요\n오른쪽 위 + 로 교수님께 받은 코드를 입력해 보세요");

        adapter.setShowSection(professor);
        showNextClass(); // 역할을 알게 됐으니 카드 버튼을 다시 정함
    }

    // 교수는 분반까지(여러 분반을 가르칠 수 있으니까), 학생은 과목명만
    private String courseTitle(Course course) {
        return professor ? course.titleText() : course.name;
    }

    // ⋮ 메뉴. 지금은 로그아웃 하나
    private void showMenu() {
        PopupMenu menu = new PopupMenu(this, binding.btnMore);
        menu.getMenu().add("로그아웃");
        menu.setOnMenuItemClickListener(item -> {
            confirmLogout();
            return true;
        });
        menu.show();
    }

    // 과목 카드를 눌렀을 때. 교수는 과목 상세 화면, 학생은 진행 중인 수업으로
    private void openCourse(Course course) {
        if (professor) {
            Intent intent = new Intent(this, CourseDetailActivity.class);
            intent.putExtra(IntentKeys.COURSE_ID, course.id);
            startActivity(intent);
        } else if (course.liveSessionId != null) {
            Intent intent = new Intent(this, ClassActivity.class);
            intent.putExtra(IntentKeys.COURSE_ID, course.id);
            intent.putExtra(IntentKeys.SESSION_ID, course.liveSessionId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "지금 진행 중인 수업이 없어요", Toast.LENGTH_SHORT).show();
        }
    }

    // 과목 카드를 길게 눌렀을 때. 학생만 수강 취소 (교수는 상세 화면 ⋮에서 삭제)
    private void onCourseLongClick(Course course) {
        if (professor) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setMessage(course.name + " 수강을 취소할까요?")
                .setPositiveButton("수강 취소", (dialog, which) -> leaveCourse(course))
                .setNegativeButton("아니요", null)
                .show();
    }

    // 등록의 반대: 내 멤버 문서, 내 과목 목록 문서를 지우고 학생 수 -1
    private void leaveCourse(Course course) {
        String uid = auth.getCurrentUser().getUid();
        DocumentReference courseRef = db.collection(FirestorePaths.COURSES).document(course.id);

        WriteBatch batch = db.batch();
        batch.delete(courseRef.collection(FirestorePaths.MEMBERS).document(uid));
        batch.delete(db.collection(FirestorePaths.USERS).document(uid)
                .collection(FirestorePaths.MY_COURSES).document(course.id));
        batch.update(courseRef, "studentCount", FieldValue.increment(-1));

        batch.commit()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "수강을 취소했어요", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "수강을 취소하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_LONG).show());
    }

    // users/{uid}/myCourses를 실시간으로 지켜봄 → 과목을 개설·등록·삭제·수강 취소하면 목록에 바로 반영
    private void listenMyCourses() {
        myCoursesListener = db.collection(FirestorePaths.USERS).document(auth.getCurrentUser().getUid())
                .collection(FirestorePaths.MY_COURSES)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "과목 목록을 불러오지 못했어요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 문서 ID가 곧 과목 ID. 처음 보는 과목만 구독 시작
                    Set<String> ids = new HashSet<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String courseId = doc.getId();
                        ids.add(courseId);
                        if (!courseListeners.containsKey(courseId)) {
                            courseListeners.put(courseId, listenCourse(courseId));
                        }
                    }

                    // 내 과목 목록에서 빠진 과목은 구독을 끊고 화면에서도 지움
                    List<String> removed = new ArrayList<>();
                    for (String courseId : courseListeners.keySet()) {
                        if (!ids.contains(courseId)) {
                            removed.add(courseId);
                        }
                    }
                    for (String courseId : removed) {
                        courseListeners.get(courseId).remove();
                        courseListeners.remove(courseId);
                        courses.remove(courseId);
                    }
                    showCourses();
                });
    }

    // courses/{courseId} 문서 하나를 실시간으로 지켜봄 (학생 수, 수업 진행 여부가 바뀌면 바로 반영)
    private ListenerRegistration listenCourse(String courseId) {
        return db.collection(FirestorePaths.COURSES).document(courseId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (!doc.exists()) {
                        courses.remove(courseId); // 교수가 과목을 삭제함 → 카드에서 지움
                        showCourses();
                        return;
                    }
                    Course updated = Course.from(doc);
                    Course before = courses.put(courseId, updated); // put은 그 자리에 있던 이전 값을 돌려줌

                    // 보고 있는 동안 "수업 중 아님 → 수업 중"으로 바뀌었으면 학생에게 알림
                    boolean started = before != null
                            && before.liveSessionId == null
                            && updated.liveSessionId != null;
                    if (started && !professor) {
                        notifyStarted(updated);
                    }
                    showCourses();
                });
    }

    // 학생: 화면 아래에 "OO 수업이 시작됐어요 [입장]"
    private void notifyStarted(Course course) {
        Snackbar.make(binding.getRoot(), course.name + " 수업이 시작됐어요", Snackbar.LENGTH_LONG)
                .setAction("입장", v -> openCourse(course))
                .show();
    }

    // 지금까지 받은 과목들을 이름순으로 정렬해서 표시
    private void showCourses() {
        List<Course> list = new ArrayList<>(courses.values());
        Collections.sort(list, (a, b) -> a.name.compareTo(b.name));
        adapter.setCourses(list);
        binding.textEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        showNextClass();
    }

    // 다음 수업 카드: 수업 중인 과목이 있으면 그 과목, 없으면 시작 시간이 가장 가까운 과목
    private void showNextClass() {
        long now = System.currentTimeMillis();
        Course live = null;
        Course next = null;
        long nextStart = Long.MAX_VALUE;
        for (Course course : courses.values()) {
            if (course.liveSessionId != null) {
                live = course;
            }
            long start = course.nextStartMillis(now);
            if (start < nextStart) {
                nextStart = start;
                next = course;
            }
        }

        if (live != null) {
            Course liveCourse = live;
            int liveColor = getColor(R.color.live);
            binding.cardNext.setStrokeColor(liveColor);
            binding.tvNextLabel.setTextColor(liveColor);
            binding.tvNextLabel.setText("● LIVE · 지금 수업 중");
            binding.tvNextName.setText(courseTitle(liveCourse));
            binding.btnNext.setVisibility(View.VISIBLE);
            binding.btnNext.setText(professor ? "교탁 모드 열기" : "입장하기");
            binding.btnNext.setOnClickListener(v -> {
                if (professor) {
                    SessionStarter.openPodium(this, liveCourse.id, liveCourse.liveSessionId);
                } else {
                    openCourse(liveCourse);
                }
            });
        } else if (next != null) {
            Course nextCourse = next;
            int primaryColor = getColor(R.color.primary);
            binding.cardNext.setStrokeColor(primaryColor);
            binding.tvNextLabel.setTextColor(primaryColor);
            binding.tvNextLabel.setText("다음 수업 · " + whenText(nextStart));
            binding.tvNextName.setText(courseTitle(nextCourse));
            binding.btnNext.setVisibility(professor ? View.VISIBLE : View.GONE);
            binding.btnNext.setText("수업 시작");
            binding.btnNext.setOnClickListener(v -> sessionStarter.start(nextCourse.id));
        } else {
            binding.cardNext.setVisibility(View.GONE); // 시간표가 있는 과목이 없음
            return;
        }
        binding.cardNext.setVisibility(View.VISIBLE);
    }

    // 시작 시각을 "오늘 13:00", "내일 09:00", "수 10:30"처럼
    private String whenText(long startMillis) {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(startMillis);

        int days = start.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR);
        String day;
        if (days == 0) {
            day = "오늘";
        } else if (days == 1) {
            day = "내일";
        } else {
            day = String.valueOf("일월화수목금토".charAt(start.get(Calendar.DAY_OF_WEEK) - 1));
        }
        return String.format(Locale.KOREA, "%s %02d:%02d",
                day, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE));
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