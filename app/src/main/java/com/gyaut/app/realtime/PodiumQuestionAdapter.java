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
    private OnAnsweredListener answeredListener;

    // 답변 완료 눌렀을 때 할 일 (화면에서 정해서 넘겨줌)
    public interface OnAnsweredListener {
        void onAnswered(PodiumQuestion question);
    }

    public PodiumQuestionAdapter(List<PodiumQuestion> questions, OnAnsweredListener answeredListener) {
        this.questions = questions;
        this.answeredListener = answeredListener;
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

            // 언제 올라왔는지 (1시간 넘으면 시간, 하루 넘으면 일로)
            long minutes = (System.currentTimeMillis() - question.createdAt) / 60000;
            if (minutes < 1) {
                binding.tvTime.setText("방금");
            } else if (minutes < 60) {
                binding.tvTime.setText(minutes + "분 전");
            } else if (minutes < 60 * 24) {
                long hours = minutes / 60;
                long rest = minutes % 60;
                if (rest == 0) {
                    binding.tvTime.setText(hours + "시간 전");
                } else {
                    binding.tvTime.setText(hours + "시간 " + rest + "분 전");
                }
            } else {
                binding.tvTime.setText(minutes / (60 * 24) + "일 전");
            }

            // 답변한 질문은 흐리게 + 답변완료 버튼만 숨기기 (더보기는 그대로)
            if (question.answered) {
                binding.getRoot().setAlpha(0.5f);
                binding.btnAnswered.setVisibility(View.GONE);
            } else {
                binding.getRoot().setAlpha(1f);
                binding.btnAnswered.setVisibility(View.VISIBLE);
            }

            // 답변 완료 누르면 화면한테 알려주기
            binding.btnAnswered.setOnClickListener(v -> answeredListener.onAnswered(question));
        }
    }
}