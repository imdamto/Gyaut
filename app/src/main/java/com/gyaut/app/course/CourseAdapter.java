package com.gyaut.app.course;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gyaut.app.databinding.ItemCourseBinding;

import java.util.ArrayList;
import java.util.List;

// 과목 목록(RecyclerView)에 과목 카드를 채워 넣는 어댑터
public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private final List<Course> courses = new ArrayList<>();

    // 카드를 눌렀을 때 / 길게 눌렀을 때 화면(Activity)에 알려 주는 통로
    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public interface OnCourseLongClickListener {
        void onCourseLongClick(Course course);
    }

    private final OnCourseClickListener listener;
    private final OnCourseLongClickListener longListener;

    public CourseAdapter(OnCourseClickListener listener, OnCourseLongClickListener longListener) {
        this.listener = listener;
        this.longListener = longListener;
    }

    // 목록을 새 내용으로 통째로 바꿈
    public void setCourses(List<Course> newCourses) {
        courses.clear();
        courses.addAll(newCourses);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCourseBinding binding = ItemCourseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course);
        holder.itemView.setOnClickListener(v -> listener.onCourseClick(course));
        holder.itemView.setOnLongClickListener(v -> {
            longListener.onCourseLongClick(course);
            return true; // 길게 누르기를 여기서 처리했다는 뜻 (짧게 누르기가 같이 불리지 않음)
        });
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    // 카드 한 칸
    static class CourseViewHolder extends RecyclerView.ViewHolder {
        private final ItemCourseBinding binding;

        CourseViewHolder(ItemCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Course course) {
            binding.tvCourseName.setText(course.titleText());
            binding.tvCourseInfo.setText(course.infoText());
            binding.tvLive.setVisibility(course.liveSessionId != null ? View.VISIBLE : View.GONE);
        }
    }
}