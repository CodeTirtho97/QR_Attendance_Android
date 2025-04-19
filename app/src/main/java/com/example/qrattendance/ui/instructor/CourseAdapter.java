package com.example.qrattendance.ui.instructor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Course;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private List<Course> courses;
    private final CourseClickListener listener;

    public interface CourseClickListener {
        void onCourseClick(Course course, int position);
        void onViewAttendanceClick(Course course, int position);
        void onManageCourseClick(Course course, int position);
        void onDeleteCourseClick(Course course, int position);
    }

    public CourseAdapter(List<Course> courses, CourseClickListener listener) {
        this.courses = courses;
        this.listener = listener;
    }

    public void updateCourses(List<Course> newCourses) {
        this.courses = newCourses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course, position);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    class CourseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCourseCode;
        private final TextView tvCourseName;
        private final TextView tvDepartment;
        private final TextView tvSemester;
        private final TextView tvStudentCount;
        private final Button btnViewAttendance;
        private final Button btnManageCourse;
        private final ImageButton btnDeleteCourse;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseCode = itemView.findViewById(R.id.tvCourseCode);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvSemester = itemView.findViewById(R.id.tvSemester);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
            btnViewAttendance = itemView.findViewById(R.id.btnViewAttendance);
            btnManageCourse = itemView.findViewById(R.id.btnManageCourse);
            btnDeleteCourse = itemView.findViewById(R.id.btnDeleteCourse);
        }

        public void bind(Course course, int position) {
            // Set course data
            tvCourseCode.setText(course.getCourseCode());
            tvCourseName.setText(course.getCourseName());
            tvDepartment.setText(course.getDepartment());
            tvSemester.setText(course.getSemester());
            tvStudentCount.setVisibility(View.GONE);

            // Set student count
            int studentCount = course.getEnrolledStudentIds() != null ?
                    course.getEnrolledStudentIds().size() : 0;
            tvStudentCount.setText(studentCount + " Students Enrolled");

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCourseClick(course, position);
                }
            });

            btnViewAttendance.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewAttendanceClick(course, position);
                }
            });

            btnManageCourse.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onManageCourseClick(course, position);
                }
            });

            btnDeleteCourse.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteCourseClick(course, position);
                }
            });
        }
    }
}