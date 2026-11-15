package com.gyaut.app.question;

import com.google.firebase.Timestamp;

public class Question {

    public String id;
    public String authorUid;
    public String content;
    public Timestamp createdAt;
    public long likeCount;
    public String sessionId;
    public String status;
    public String topic;
    public long week;

    public Question() {
    }
}