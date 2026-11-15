package com.gyaut.app.question;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gyaut.app.R;

import java.util.List;

public class QuestionAdapter extends
        RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {

    // 질문 데이터 목록
    private List<Question> questionList;

    // 생성자
    public QuestionAdapter(List<Question> questionList) {
        this.questionList = questionList;
    }

    // 질문 하나의 화면 생성
    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question, parent, false);

        return new QuestionViewHolder(view);
    }

    // 질문 데이터를 화면에 연결
    @Override
    public void onBindViewHolder(
            @NonNull QuestionViewHolder holder, int position) {

        Question question = questionList.get(position);

        holder.textContent.setText(question.content);

        holder.textTopic.setText(
                question.week + "주차 · " + question.topic);

        holder.textLikeCount.setText(
                "좋아요 " + question.likeCount + "개");

        if ("visible".equals(question.status)) {
            holder.textStatus.setText("공개");
        } else {
            holder.textStatus.setText("숨김");
        }
    }

    // 전체 질문 개수 반환
    @Override
    public int getItemCount() {
        return questionList.size();
    }

    // item_question.xml의 요소 관리
    public static class QuestionViewHolder
            extends RecyclerView.ViewHolder {

        TextView textTopic;
        TextView textContent;
        TextView textLikeCount;
        TextView textStatus;

        Button buttonHide;
        Button buttonWarning;
        Button buttonDelete;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);

            textTopic = itemView.findViewById(R.id.textTopic);
            textContent = itemView.findViewById(R.id.textContent);
            textLikeCount = itemView.findViewById(R.id.textLikeCount);
            textStatus = itemView.findViewById(R.id.textStatus);

            buttonHide = itemView.findViewById(R.id.buttonHide);
            buttonWarning = itemView.findViewById(R.id.buttonWarning);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}