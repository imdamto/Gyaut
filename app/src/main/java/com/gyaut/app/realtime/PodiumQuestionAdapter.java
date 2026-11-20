package com.gyaut.app.realtime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gyaut.app.databinding.ItemPodiumQuestionBinding;

import java.util.List;

// 교탁모드 질문 목록 어댑터
public class PodiumQuestionAdapter extends RecyclerView.Adapter<PodiumQuestionAdapter.QuestionViewHolder> {

    private List<PodiumQuestion> questions;

    public PodiumQuestionAdapter(List<PodiumQuestion> questions) {
        this.questions = questions;
    }

    // 카드 한 장 만들기
    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPodiumQuestionBinding binding = ItemPodiumQuestionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new QuestionViewHolder(binding);
    }

    // position번째 질문 내용을 카드에 넣기
    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        holder.bind(questions.get(position));
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    // 카드 한 장 (item_podium_question.xml)
    class QuestionViewHolder extends RecyclerView.ViewHolder {
        ItemPodiumQuestionBinding binding;

        QuestionViewHolder(ItemPodiumQuestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PodiumQuestion question) {
            binding.tvContent.setText(question.content);
            binding.tvLikes.setText("궁금해요 " + question.likeCount);

            // 몇분 전에 올라왔는지
            long minutes = (System.currentTimeMillis() - question.createdAt) / 60000;
            if (minutes < 1) {
                binding.tvTime.setText("방금");
            } else {
                binding.tvTime.setText(minutes + "분 전");
            }

            // 답변한 질문은 흐리게 + 답변완료 버튼만 숨기기 (더보기는 그대로)
            if (question.answered) {
                binding.getRoot().setAlpha(0.5f);
                binding.btnAnswered.setVisibility(View.GONE);
            } else {
                binding.getRoot().setAlpha(1f);
                binding.btnAnswered.setVisibility(View.VISIBLE);
            }
        }
    }
}