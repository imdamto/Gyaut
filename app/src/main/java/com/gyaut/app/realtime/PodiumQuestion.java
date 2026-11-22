package com.gyaut.app.realtime;

// 교탁모드 질문 카드 하나에 들어갈 내용
public class PodiumQuestion {
    public String id;        // 질문 문서 아이디 (답변완료 저장할 때 씀)
    public String content;   // 질문 내용
    public long createdAt;   // 올린 시간 (밀리초)
    public long likeCount;   // 궁금해요 수
    public boolean answered; // 답변 완료했는지
}