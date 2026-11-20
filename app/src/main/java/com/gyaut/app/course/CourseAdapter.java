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
    private boolean professor; // 교수에게만 참여 코드를 보여 줌

    public void setProfessor(boolean professor) {
        this.professor = professor;
        notifyDataSetChanged();
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
        holder.bind(courses.get(position), professor);
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

        void bind(Course course, boolean professor) {
            binding.tvCourseName.setText(course.titleText());
            binding.tvCourseInfo.setText(course.infoText());
            binding.tvJoinCode.setText("참여 코드  " + course.joinCode);
            binding.tvJoinCode.setVisibility(professor ? View.VISIBLE : View.GONE);
        }
    }
}