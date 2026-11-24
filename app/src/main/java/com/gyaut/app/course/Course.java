package com.gyaut.app.course;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

// courses/{courseId} 문서 하나를 담는 클래스
public class Course {
    public String id;
    public String name;
    public String section;        // 분반. 입력 안 했으면 null
    public List<String> schedule; // 예: ["월 09:00", "수 10:30"]
    public String joinCode;
    public long studentCount;
    public String liveSessionId;  // 진행 중인 수업 ID. 수업 중이 아니면 null

    // Firestore 문서 → Course
    @SuppressWarnings("unchecked")
    public static Course from(DocumentSnapshot doc) {
        Course course = new Course();
        course.id = doc.getId();
        course.name = doc.getString("name");
        course.section = doc.getString("section");
        course.schedule = (List<String>) doc.get("schedule");
        course.joinCode = doc.getString("joinCode");
        Long count = doc.getLong("studentCount"); // 숫자는 Long으로 꺼냄
        course.studentCount = count == null ? 0 : count;
        course.liveSessionId = doc.getString("liveSessionId");
        return course;
    }

    // 카드 제목. 예: "사용자인터페이스 (01분반)"
    public String titleText() {
        return section == null ? name : name + " (" + section + "분반)";
    }

    // 카드 아래 줄. 예: "월 09:00, 수 10:30 · 48명"
    public String infoText() {
        String students = studentCount + "명";
        if (schedule == null || schedule.isEmpty()) {
            return students;
        }
        return String.join(", ", schedule) + " · " + students;
    }
}