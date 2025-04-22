package com.example.qrattendance.ui.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Course;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MyCourseAdapter extends RecyclerView.Adapter<MyCourseAdapter.CourseViewHolder> {

    private List<Course> courses;

    public MyCourseAdapter(List<Course> courses) {
        this.courses = courses;
    }

    public void updateCourses(List<Course> newCourses) {
        this.courses = newCourses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCourseCode;
        private final TextView tvCourseName;
        private final TextView tvDepartment;
        private final TextView tvSemester;
        private final TextView tvCredits;
        private final TextView tvInstructor;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseCode = itemView.findViewById(R.id.tvCourseCode);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvSemester = itemView.findViewById(R.id.tvSemester);
            tvCredits = itemView.findViewById(R.id.tvCredits);
            tvInstructor = itemView.findViewById(R.id.tvInstructor);
        }

        public void bind(Course course) {
            tvCourseCode.setText(course.getCourseCode());
            tvCourseName.setText(course.getCourseName());
            tvDepartment.setText("Department: " + course.getDepartment());
            tvSemester.setText("Semester: " + course.getSemester());
            tvCredits.setText("Credits: " + course.getCredits());

            // Fetch instructor name
            if (course.getInstructorId() != null) {
                FirebaseFirestore.getInstance().collection("users").document(course.getInstructorId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String instructorName = documentSnapshot.getString("name");
                                if (instructorName != null) {
                                    tvInstructor.setText("Instructor: " + instructorName);
                                    tvInstructor.setVisibility(View.VISIBLE);
                                } else {
                                    tvInstructor.setVisibility(View.GONE);
                                }
                            } else {
                                tvInstructor.setVisibility(View.GONE);
                            }
                        })
                        .addOnFailureListener(e -> tvInstructor.setVisibility(View.GONE));
            } else {
                tvInstructor.setVisibility(View.GONE);
            }
        }
    }
}