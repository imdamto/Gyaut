package com.gyaut.app.common;

public final class FirestorePaths {
    public static final String USERS = "users";
    public static final String MY_COURSES = "myCourses";
    public static final String COURSES = "courses";
    public static final String MEMBERS = "members";
    public static final String SESSIONS = "sessions";
    public static final String CONFUSION = "confusion";
    public static final String WARNINGS = "warnings";
    public static final String QUESTIONS = "questions";
    public static final String LIKES = "likes";

    public static final long CONFUSION_WINDOW_MS = 3 * 60 * 1000; // 3분

    private FirestorePaths() {}
}