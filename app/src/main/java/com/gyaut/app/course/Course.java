package com.gyaut.app.course;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;
import java.util.List;

// courses/{courseId} 문서 하나를 담는 클래스
public class Course {
    private static final String DAYS = "월화수목금";
    private static final long LATE_MS = 30 * 60 * 1000; // 시작 후 30분까지는 아직 그 수업으로 봄

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

    // 지금(now)부터 가장 가까운 수업 시작 시각(밀리초). 시간표가 없으면 Long.MAX_VALUE
    public long nextStartMillis(long now) {
        long next = Long.MAX_VALUE;
        if (schedule == null) {
            return next;
        }
        for (String item : schedule) {                        // 예: "수 10:30"
            int day = DAYS.indexOf(item.charAt(0));           // 월=0, 화=1 ... 금=4
            int hour = Integer.parseInt(item.substring(2, 4));
            int minute = Integer.parseInt(item.substring(5, 7));

            // 오늘부터 하루씩 넘겨 보면서 그 요일을 찾음
            for (int d = 0; d <= 7; d++) {
                Calendar start = Calendar.getInstance();
                start.setTimeInMillis(now);
                start.add(Calendar.DAY_OF_MONTH, d);
                start.set(Calendar.HOUR_OF_DAY, hour);
                start.set(Calendar.MINUTE, minute);
                start.set(Calendar.SECOND, 0);
                start.set(Calendar.MILLISECOND, 0);

                boolean sameDay = start.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY + day;
                if (sameDay && start.getTimeInMillis() + LATE_MS > now) {
                    next = Math.min(next, start.getTimeInMillis());
                    break;
                }
            }
        }
        return next;
    }
}