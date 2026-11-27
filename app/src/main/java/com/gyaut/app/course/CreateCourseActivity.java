package com.gyaut.app.course;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.gyaut.app.R;
import com.gyaut.app.common.FirestorePaths;
import com.gyaut.app.common.IntentKeys;
import com.gyaut.app.common.UserRole;
import com.gyaut.app.common.WindowInsetsHelper;
import com.gyaut.app.databinding.ActivityCreateCourseBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

// 교수: 과목을 만들거나(개설) 고치는(수정) 화면. 과목 ID를 받으면 수정 모드
public class CreateCourseActivity extends AppCompatActivity {

    private static final String[] DAY_NAMES = {"월", "화", "수", "목", "금"};
    // 참여 코드에 쓰는 글자. 헷갈리기 쉬운 I, O, 0, 1은 뺌
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private ActivityCreateCourseBinding binding;
    private FirebaseFirestore db;
    private String uid;
    private String editCourseId; // 수정할 과목 ID. 새로 만들 때는 null

    // 요일 순서(월~금)대로 묶어 둔 화면 요소와, 요일마다 고른 시간
    private int[] dayButtonIds;
    private View[] timeRows;
    private Button[] timeButtons;
    private final int[] hours = {9, 9, 9, 9, 9};
    private final int[] minutes = {0, 0, 0, 0, 0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCreateCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applyTo(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        dayButtonIds = new int[]{R.id.btnDayMon, R.id.btnDayTue, R.id.btnDayWed, R.id.btnDayThu, R.id.btnDayFri};
        timeRows = new View[]{binding.rowMon, binding.rowTue, binding.rowWed, binding.rowThu, binding.rowFri};
        timeButtons = new Button[]{binding.btnTimeMon, binding.btnTimeTue, binding.btnTimeWed,
                binding.btnTimeThu, binding.btnTimeFri};

        for (int i = 0; i < DAY_NAMES.length; i++) {
            int day = i; // 람다 안에서 쓰려면 값이 바뀌지 않는 변수여야 함
            timeButtons[i].setOnClickListener(v -> pickTime(day));
        }

        // 요일을 고르거나 풀면 그 요일의 시간 줄을 보이거나 숨김
        binding.toggleDay.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            for (int i = 0; i < dayButtonIds.length; i++) {
                if (dayButtonIds[i] == checkedId) {
                    timeRows[i].setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            }
        });

        editCourseId = getIntent().getStringExtra(IntentKeys.COURSE_ID);
        if (editCourseId != null) {
            binding.tvTitle.setText("과목 수정");
            binding.tvSubtitle.setText("참여 코드는 바뀌지 않아요");
            binding.btnCreate.setText("저장하기");
            loadCourse();
        } else {
            binding.editSemester.setText(defaultSemester());
        }

        binding.btnCreate.setOnClickListener(v -> tryCreate());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    // 오늘 날짜로 학기 미리 채우기 (3~8월은 1학기, 9~12월은 2학기, 1~2월은 전년도 2학기)
    private String defaultSemester() {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1; // MONTH는 0부터 시작
        if (month <= 2) {
            return (year - 1) + "년 2학기";
        }
        return year + "년 " + (month <= 8 ? 1 : 2) + "학기";
    }

    // 수정 모드: 과목의 지금 값을 입력칸에 채움
    private void loadCourse() {
        binding.btnCreate.setEnabled(false);
        db.collection(FirestorePaths.COURSES).document(editCourseId)
                .get()
                .addOnSuccessListener(doc -> {
                    Course course = Course.from(doc);
                    binding.editCourseName.setText(course.name);
                    binding.editSection.setText(course.section);
                    binding.editSemester.setText(doc.getString("semester"));
                    showSchedule(course.schedule);
                    binding.btnCreate.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "과목 정보를 불러오지 못했어요", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // ["월 09:00", "수 10:30"] → 요일 버튼을 선택하고 시간 버튼에 그 시간 표시
    private void showSchedule(List<String> schedule) {
        if (schedule == null) {
            return;
        }
        boolean hasMonday = false;
        for (String item : schedule) {
            int day = Arrays.asList(DAY_NAMES).indexOf(item.substring(0, 1));
            hours[day] = Integer.parseInt(item.substring(2, 4));
            minutes[day] = Integer.parseInt(item.substring(5, 7));
            timeButtons[day].setText(timeText(day));
            binding.toggleDay.check(dayButtonIds[day]);
            if (day == 0) {
                hasMonday = true;
            }
        }
        // 월요일은 처음부터 선택돼 있으니, 시간표에 없으면 풀어 줌
        if (!hasMonday && !schedule.isEmpty()) {
            binding.toggleDay.uncheck(R.id.btnDayMon);
        }
    }

    // 시계 창을 띄워서 그 요일의 시작 시간 고르기
    private void pickTime(int day) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hours[day])
                .setMinute(minutes[day])
                .setTitleText(DAY_NAMES[day] + "요일 시작 시간")
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            hours[day] = picker.getHour();
            minutes[day] = picker.getMinute();
            timeButtons[day].setText(timeText(day));
        });
        picker.show(getSupportFragmentManager(), "time");
    }

    private String timeText(int day) {
        return String.format(Locale.KOREA, "%02d:%02d", hours[day], minutes[day]);
    }

    // 입력값 검사 → (개설) 안 겹치는 참여 코드 찾기 → 저장 / (수정) 바로 저장
    private void tryCreate() {
        String name = textOf(binding.editCourseName).trim();
        String section = textOf(binding.editSection).trim();
        String semester = textOf(binding.editSemester).trim();

        binding.inputCourseName.setError(null);
        binding.inputSemester.setError(null);

        if (name.isEmpty()) {
            binding.inputCourseName.setError("과목명을 입력해 주세요");
            return;
        }
        if (semester.isEmpty()) {
            binding.inputSemester.setError("학기를 입력해 주세요");
            return;
        }

        // 고른 요일만 요일 순서대로 담기. 예: ["월 09:00", "수 10:30"]
        List<Integer> checkedIds = binding.toggleDay.getCheckedButtonIds();
        List<String> schedule = new ArrayList<>();
        for (int i = 0; i < DAY_NAMES.length; i++) {
            if (checkedIds.contains(dayButtonIds[i])) {
                schedule.add(DAY_NAMES[i] + " " + timeText(i));
            }
        }

        if (editCourseId != null) {
            updateCourse(name, section, semester, schedule);
            return;
        }

        Map<String, Object> course = new HashMap<>();
        course.put("name", name);
        if (!section.isEmpty()) {
            course.put("section", section);
        }
        course.put("semester", semester);
        course.put("schedule", schedule);
        course.put("studentCount", 0);

        setLoading(true);
        findUnusedCode(course);
    }

    // 수정 모드: 입력칸에 있는 값만 바꿈. 참여 코드, 학생 수, 진행 중인 수업은 그대로
    private void updateCourse(String name, String section, String semester, List<String> schedule) {
        Map<String, Object> changes = new HashMap<>();
        changes.put("name", name);
        changes.put("section", section.isEmpty() ? FieldValue.delete() : section);
        changes.put("semester", semester);
        changes.put("schedule", schedule);

        setLoading(true);
        db.collection(FirestorePaths.COURSES).document(editCourseId)
                .update(changes)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "과목 정보를 저장했어요", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> fail());
    }

    // 무작위 코드를 만들고, 이미 그 코드를 쓰는 과목이 있으면 다시 만듦
    private void findUnusedCode(Map<String, Object> course) {
        String code = randomCode();
        db.collection(FirestorePaths.COURSES)
                .whereEqualTo("joinCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        course.put("joinCode", code);
                        saveCourse(course, code);
                    } else {
                        findUnusedCode(course);
                    }
                })
                .addOnFailureListener(e -> fail());
    }

    private String randomCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    // 과목, 멤버(교수 본인), 내 과목 목록을 한 번에 저장
    private void saveCourse(Map<String, Object> course, String code) {
        DocumentReference courseRef = db.collection(FirestorePaths.COURSES).document(); // 새 문서 ID 자동 생성

        Map<String, Object> member = new HashMap<>();
        member.put("role", UserRole.PROFESSOR);
        member.put("joinedAt", FieldValue.serverTimestamp());

        Map<String, Object> myCourse = new HashMap<>();
        myCourse.put("courseName", course.get("name"));

        // batch: 세 문서가 전부 저장되거나 전부 실패하게 묶음
        WriteBatch batch = db.batch();
        batch.set(courseRef, course);
        batch.set(courseRef.collection(FirestorePaths.MEMBERS).document(uid), member);
        batch.set(db.collection(FirestorePaths.USERS).document(uid)
                .collection(FirestorePaths.MY_COURSES).document(courseRef.getId()), myCourse);

        batch.commit()
                .addOnSuccessListener(unused -> showCode(code))
                .addOnFailureListener(e -> fail());
    }

    // 만든 참여 코드를 보여 주고 화면 닫기
    private void showCode(String code) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("과목을 만들었어요")
                .setMessage("참여 코드  " + code + "\n\n학생들에게 이 코드를 알려 주세요.")
                .setCancelable(false)
                .setPositiveButton("확인", (dialog, which) -> finish())
                .setNeutralButton("코드 복사", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("참여 코드", code));
                    Toast.makeText(this, "코드를 복사했어요", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }

    private void fail() {
        setLoading(false);
        String message = editCourseId != null
                ? "저장하지 못했어요. 다시 시도해 주세요"
                : "과목을 만들지 못했어요. 다시 시도해 주세요";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        boolean editing = editCourseId != null;
        binding.btnCreate.setEnabled(!loading);
        if (loading) {
            binding.btnCreate.setText(editing ? "저장하는 중..." : "만드는 중...");
        } else {
            binding.btnCreate.setText(editing ? "저장하기" : "개설하기");
        }
    }

    private String textOf(EditText editText) {
        Editable text = editText.getText();
        return text == null ? "" : text.toString();
    }
}