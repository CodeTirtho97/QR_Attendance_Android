package com.example.qrattendance.ui.admin;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCourseAdapter extends RecyclerView.Adapter<AdminCourseAdapter.CourseViewHolder> {

    private List<Course> courses;
    private final CourseActionListener listener;
    private final UserRepository userRepository;
    private final Map<String, String> instructorCache = new HashMap<>();

    public interface CourseActionListener {
        void onCourseClick(Course course);
        void onEditCourse(Course course);
        void onDeleteCourse(Course course);
    }

    public AdminCourseAdapter(List<Course> courses, CourseActionListener listener) {
        this.courses = courses;
        this.listener = listener;
        this.userRepository = UserRepository.getInstance();
    }

    public void updateCourses(List<Course> newCourses) {
        this.courses = newCourses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_course, parent, false);
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

    class CourseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCourseName;
        private final TextView tvCourseCode;
        private final TextView tvDepartment;
        private final TextView tvInstructor;
        private final TextView tvStudentCount;
        private final TextView tvCourseStatus;
        private final ImageButton btnEditCourse;
        private final ImageButton btnDeleteCourse;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvCourseCode = itemView.findViewById(R.id.tvCourseCode);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvInstructor = itemView.findViewById(R.id.tvInstructor);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
            tvCourseStatus = itemView.findViewById(R.id.tvCourseStatus);
            btnEditCourse = itemView.findViewById(R.id.btnEditCourse);
            btnDeleteCourse = itemView.findViewById(R.id.btnToggleActive); // Reusing the button but changing functionality
        }

        public void bind(Course course) {
            // Set course basic info
            tvCourseName.setText(course.getCourseName());
            tvCourseCode.setText(course.getCourseCode());
            tvDepartment.setText(course.getDepartment());

            // Set student count
            int studentCount = course.getEnrolledStudentIds() != null ?
                    course.getEnrolledStudentIds().size() : 0;
            tvStudentCount.setText(studentCount + " Students");

            // Handle instructor info
            String instructorId = course.getInstructorId();
            if (instructorId != null && !instructorId.isEmpty()) {
                // First check if we have this instructor in our cache
                if (instructorCache.containsKey(instructorId)) {
                    tvInstructor.setText("Instructor: " + instructorCache.get(instructorId));
                } else {
                    // Set a loading text
                    tvInstructor.setText("Loading instructor info...");

                    // Fetch instructor info
                    userRepository.getUserById(instructorId, new UserRepository.OnUserLoadedListener() {
                        @Override
                        public void onUserLoaded(Map<String, Object> userData) {
                            // Get the instructor name and employee ID
                            String instructorName = (String) userData.get("name");
                            String employeeId = (String) userData.get("employeeId");

                            String displayText;
                            if (instructorName != null && employeeId != null) {
                                displayText = instructorName + " (" + employeeId + ")";
                            } else if (instructorName != null) {
                                displayText = instructorName;
                            } else if (employeeId != null) {
                                displayText = "Instructor (" + employeeId + ")";
                            } else {
                                displayText = "Unknown Instructor";
                            }

                            // Update cache
                            instructorCache.put(instructorId, displayText);

                            // Update UI
                            tvInstructor.setText("Instructor: " + displayText);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Just show a generic label on error
                            tvInstructor.setText("Instructor");
                        }
                    });
                }
            } else {
                tvInstructor.setText("No instructor assigned");
            }

            // Set course active status
            if (course.isActive()) {
                tvCourseStatus.setText("Active");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvCourseStatus.setBackgroundTintList(
                            itemView.getContext().getColorStateList(R.color.accent_green));
                }
            } else {
                tvCourseStatus.setText("Inactive");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvCourseStatus.setBackgroundTintList(
                            itemView.getContext().getColorStateList(R.color.accent_red));
                }
            }

            // Update delete button appearance for admin (using delete icon instead of visibility)
            btnDeleteCourse.setImageResource(R.drawable.ic_delete);
            btnDeleteCourse.setContentDescription("Delete course");

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCourseClick(course);
                }
            });

            btnEditCourse.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditCourse(course);
                }
            });

            btnDeleteCourse.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteCourse(course);
                }
            });
        }
    }
}