package com.example.qrattendance.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.repository.CourseRepository;
import com.example.qrattendance.data.repository.EnhancedCourseRepository;
import com.example.qrattendance.data.repository.UserRepository;
import com.example.qrattendance.util.SessionManager;
import com.example.qrattendance.util.UIHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CourseManagementActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerViewCourses;
    private TextView tvNoCourses;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddCourse;

    private CourseRepository courseRepository;
    private EnhancedCourseRepository enhancedCourseRepository;
    private UserRepository userRepository;
    private Admin currentAdmin;
    private AdminCourseAdapter courseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_courses);

        // Initialize repositories
        courseRepository = CourseRepository.getInstance();
        enhancedCourseRepository = EnhancedCourseRepository.getInstance();
        userRepository = UserRepository.getInstance();

        // Get current admin from session
        currentAdmin = (Admin) SessionManager.getInstance(this).getUserData();
        if (currentAdmin == null) {
            UIHelper.showErrorDialog(this, "Session Error", "Please log in again.");
            finish();
            return;
        }

        // Initialize views
        initViews();
        setupRecyclerView();
        observeCourseData();
        loadCourses();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewCourses = findViewById(R.id.recyclerViewCourses);
        tvNoCourses = findViewById(R.id.tvNoCourses);
        progressBar = findViewById(R.id.progressBar);
        fabAddCourse = findViewById(R.id.fabAddCourse);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Courses");
        }

        // Set FAB click listener
        fabAddCourse.setOnClickListener(v -> {
            // Check admin privileges
            if (currentAdmin.hasPrivilege(Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN)) {
                navigateToAddCourseActivity();
            } else {
                UIHelper.showErrorDialog(this, "Permission Denied",
                        "You need Department Admin or higher privileges to add courses.");
            }
        });
    }

    private void setupRecyclerView() {
        courseAdapter = new AdminCourseAdapter(new ArrayList<>(), new AdminCourseAdapter.CourseActionListener() {
            @Override
            public void onCourseClick(Course course) {
                showCourseDetails(course);
            }

            @Override
            public void onEditCourse(Course course) {
                if (!currentAdmin.hasPrivilege(Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN)) {
                    UIHelper.showErrorDialog(CourseManagementActivity.this, "Permission Denied",
                            "You need Department Admin or higher privileges to edit courses.");
                    return;
                }

                navigateToEditCourseActivity(course);
            }

            @Override
            public void onDeleteCourse(Course course) {
                if (!currentAdmin.hasPrivilege(Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN)) {
                    UIHelper.showErrorDialog(CourseManagementActivity.this, "Permission Denied",
                            "You need Department Admin or higher privileges to delete courses.");
                    return;
                }

                deleteCourse(course);
            }
        });

        recyclerViewCourses.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCourses.setAdapter(courseAdapter);
    }

    private void observeCourseData() {
        // Observe loading state
        courseRepository.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe error messages
        courseRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                UIHelper.showErrorDialog(this, "Error", errorMsg);
            }
        });

        // Observe all courses
        courseRepository.getAllCourses().observe(this, courses -> {
            if (courses != null) {
                if (courses.isEmpty()) {
                    tvNoCourses.setVisibility(View.VISIBLE);
                    recyclerViewCourses.setVisibility(View.GONE);
                } else {
                    tvNoCourses.setVisibility(View.GONE);
                    recyclerViewCourses.setVisibility(View.VISIBLE);
                    courseAdapter.updateCourses(courses);
                }
            }
        });
    }

    private void loadCourses() {
        // Load all courses, both active and inactive for admin view
        courseRepository.fetchAllActiveCourses();
    }

    private void showCourseDetails(Course course) {
        // Get instructor details
        userRepository.getUserById(course.getInstructorId(), new UserRepository.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(Map<String, Object> instructorData) {
                StringBuilder details = new StringBuilder();
                details.append("Course Name: ").append(course.getCourseName()).append("\n");
                details.append("Course Code: ").append(course.getCourseCode()).append("\n");
                details.append("Department: ").append(course.getDepartment()).append("\n");
                details.append("Semester: ").append(course.getSemester()).append("\n");

                if (course.getDescription() != null && !course.getDescription().isEmpty()) {
                    details.append("Description: ").append(course.getDescription()).append("\n");
                }

                // Add instructor info
                String instructorName = (String) instructorData.get("name");
                details.append("Instructor: ").append(instructorName).append("\n");

                // Add student count
                int studentCount = course.getEnrolledStudentIds() != null ?
                        course.getEnrolledStudentIds().size() : 0;
                details.append("Students Enrolled: ").append(studentCount).append("\n");

                // Add active status
                details.append("Status: ").append(course.isActive() ? "Active" : "Inactive").append("\n");

                UIHelper.showInfoDialog(CourseManagementActivity.this,
                        "Course Details", details.toString());
            }

            @Override
            public void onError(String errorMessage) {
                // Show course details without instructor info
                StringBuilder details = new StringBuilder();
                details.append("Course Name: ").append(course.getCourseName()).append("\n");
                details.append("Course Code: ").append(course.getCourseCode()).append("\n");
                details.append("Department: ").append(course.getDepartment()).append("\n");
                details.append("Semester: ").append(course.getSemester()).append("\n");

                if (course.getDescription() != null && !course.getDescription().isEmpty()) {
                    details.append("Description: ").append(course.getDescription()).append("\n");
                }

                // Add instructor ID (since we couldn't get the name)
                details.append("Instructor ID: ").append(course.getInstructorId()).append("\n");

                // Add student count
                int studentCount = course.getEnrolledStudentIds() != null ?
                        course.getEnrolledStudentIds().size() : 0;
                details.append("Students Enrolled: ").append(studentCount).append("\n");

                // Add active status
                details.append("Status: ").append(course.isActive() ? "Active" : "Inactive").append("\n");

                UIHelper.showInfoDialog(CourseManagementActivity.this,
                        "Course Details", details.toString());
            }
        });
    }

    private void navigateToAddCourseActivity() {
        Intent intent = new Intent(this, AddEditCourseActivity.class);
        startActivity(intent);
    }

    private void navigateToEditCourseActivity(Course course) {
        Intent intent = new Intent(this, AddEditCourseActivity.class);
        intent.putExtra("courseId", course.getCourseId());
        startActivity(intent);
    }

    private void deleteCourse(Course course) {
        // Create confirmation dialog with warning about data deletion
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete the course \"" + course.getCourseName() +
                        "\"?\n\nWARNING: This will permanently delete ALL related data including:\n" +
                        "• All sessions for this course\n" +
                        "• All QR codes used for attendance\n" +
                        "• All attendance records\n" +
                        "• Course references for students and instructor\n\n" +
                        "This action CANNOT be undone!")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Show progress dialog for deletion
                    AlertDialog progressDialog = new AlertDialog.Builder(this)
                            .setTitle("Deleting Course")
                            .setMessage("Please wait while we delete the course and all related data...")
                            .setCancelable(false)
                            .show();

                    // Use enhanced deletion to remove all related data
                    enhancedCourseRepository.deleteCourseWithAllData(course.getCourseId(),
                            new EnhancedCourseRepository.OnCompleteListener() {
                                @Override
                                public void onSuccess(String id) {
                                    // Dismiss progress dialog
                                    progressDialog.dismiss();

                                    // Show success message
                                    Toast.makeText(CourseManagementActivity.this,
                                            "Course and all related data deleted successfully",
                                            Toast.LENGTH_LONG).show();

                                    // Refresh the courses list
                                    loadCourses();
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    // Dismiss progress dialog
                                    progressDialog.dismiss();

                                    UIHelper.showErrorDialog(CourseManagementActivity.this,
                                            "Error", "Failed to delete course: " + errorMessage);
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning to this activity
        loadCourses();
    }
}