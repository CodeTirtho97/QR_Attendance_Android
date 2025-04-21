package com.example.qrattendance.ui.student;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Course;

import java.util.List;

public class CourseEnrollmentAdapter extends RecyclerView.Adapter<CourseEnrollmentAdapter.CourseViewHolder> {

    private List<Course> courses;
    private List<String> enrolledCourseIds;
    private final CourseEnrollmentListener listener;

    public interface CourseEnrollmentListener {
        void onEnrollClicked(Course course);
        void onUnenrollClicked(Course course);
    }

    public CourseEnrollmentAdapter(List<Course> courses, List<String> enrolledCourseIds, CourseEnrollmentListener listener) {
        this.courses = courses;
        this.enrolledCourseIds = enrolledCourseIds;
        this.listener = listener;
    }

    public void updateCourses(List<Course> newCourses) {
        this.courses = newCourses;
        notifyDataSetChanged();
    }

    public void updateEnrolledCourses(List<String> newEnrolledCourseIds) {
        this.enrolledCourseIds = newEnrolledCourseIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_enrollment_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        boolean isEnrolled = enrolledCourseIds != null && enrolledCourseIds.contains(course.getCourseId());
        holder.bind(course, isEnrolled);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    class CourseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCourseCode;
        private final TextView tvCourseName;
        private final TextView tvInstructor;
        private final TextView tvDescription;
        private final Button btnEnroll;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseCode = itemView.findViewById(R.id.tvCourseCode);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvInstructor = itemView.findViewById(R.id.tvInstructor);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnEnroll = itemView.findViewById(R.id.btnEnroll);
        }

        public void bind(Course course, boolean isEnrolled) {
            tvCourseCode.setText(course.getCourseCode());
            tvCourseName.setText(course.getCourseName());

            // Set description if available
            if (course.getDescription() != null && !course.getDescription().isEmpty()) {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(course.getDescription());
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Set instructor information
            tvInstructor.setText("Department: " + course.getDepartment());

            // Configure the enrollment button based on enrollment status
            if (isEnrolled) {
                btnEnroll.setText("Unenroll");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    btnEnroll.setBackgroundTintList(itemView.getContext().getColorStateList(R.color.accent_red));
                }
                btnEnroll.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUnenrollClicked(course);
                    }
                });
            } else {
                btnEnroll.setText("Enroll");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    btnEnroll.setBackgroundTintList(itemView.getContext().getColorStateList(R.color.accent_green));
                }
                btnEnroll.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEnrollClicked(course);
                    }
                });
            }
        }
    }
}