package com.gyaut.app.question;

import com.google.firebase.Timestamp;
import java.util.List;
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
    public List<String> likedUsers;
    public Question() {
    }
}