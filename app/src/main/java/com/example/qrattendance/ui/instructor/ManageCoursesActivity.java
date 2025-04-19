package com.example.qrattendance.ui.instructor;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.data.repository.CourseRepository;
import com.example.qrattendance.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Date;

public class ManageCoursesActivity extends AppCompatActivity implements CourseAdapter.CourseClickListener {

    private RecyclerView recyclerViewCourses;
    private CourseAdapter courseAdapter;
    private View emptyStateView;
    private View progressBar;
    private FloatingActionButton fabAddCourse;

    private CourseRepository courseRepository;
    private Instructor currentInstructor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_courses);

        // Initialize repository
        courseRepository = CourseRepository.getInstance();

        // Get current instructor from session
        currentInstructor = (Instructor) SessionManager.getInstance(this).getUserData();
        if (currentInstructor == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();
        setupRecyclerView();
        loadCourses();

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViews() {
        recyclerViewCourses = findViewById(R.id.recyclerViewCourses);
        emptyStateView = findViewById(R.id.emptyStateView);
        progressBar = findViewById(R.id.progressBar);
        fabAddCourse = findViewById(R.id.fabAddCourse);

        fabAddCourse.setOnClickListener(v -> showAddCourseDialog());
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(new ArrayList<>(), this);
        recyclerViewCourses.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCourses.setAdapter(courseAdapter);
    }

    private void loadCourses() {
        progressBar.setVisibility(View.VISIBLE);

        // Observe courses
        courseRepository.getCourses().observe(this, courses -> {
            progressBar.setVisibility(View.GONE);

            if (courses.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                recyclerViewCourses.setVisibility(View.GONE);
            } else {
                emptyStateView.setVisibility(View.GONE);
                recyclerViewCourses.setVisibility(View.VISIBLE);
                courseAdapter.updateCourses(courses);
            }
        });

        // Observe errors
        courseRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        // Load courses for this instructor
        courseRepository.fetchCoursesByInstructor(currentInstructor.getUserId());
    }

    private void showAddCourseDialog() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);

        TextInputLayout tilCourseCode = dialogView.findViewById(R.id.tilCourseCode);
        TextInputLayout tilCourseName = dialogView.findViewById(R.id.tilCourseName);
        TextInputLayout tilDepartment = dialogView.findViewById(R.id.tilDepartment);
        TextInputLayout tilSemester = dialogView.findViewById(R.id.tilSemester);
        TextInputLayout tilCredits = dialogView.findViewById(R.id.tilCredits);
        TextInputLayout tilDescription = dialogView.findViewById(R.id.tilDescription);

        TextInputEditText etCourseCode = dialogView.findViewById(R.id.etCourseCode);
        TextInputEditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        TextInputEditText etDepartment = dialogView.findViewById(R.id.etDepartment);
        TextInputEditText etSemester = dialogView.findViewById(R.id.etSemester);
        TextInputEditText etCredits = dialogView.findViewById(R.id.etCredits);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Add New Course")
                .setView(dialogView)
                .setPositiveButton("Add", null) // We'll set this later to avoid auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Set positive button click listener after showing dialog to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate inputs
            boolean isValid = true;

            String courseCode = etCourseCode.getText().toString().trim();
            if (courseCode.isEmpty()) {
                tilCourseCode.setError("Course code is required");
                isValid = false;
            } else {
                tilCourseCode.setError(null);
            }

            String courseName = etCourseName.getText().toString().trim();
            if (courseName.isEmpty()) {
                tilCourseName.setError("Course name is required");
                isValid = false;
            } else {
                tilCourseName.setError(null);
            }

            String department = etDepartment.getText().toString().trim();
            if (department.isEmpty()) {
                tilDepartment.setError("Department is required");
                isValid = false;
            } else {
                tilDepartment.setError(null);
            }

            String semester = etSemester.getText().toString().trim();
            if (semester.isEmpty()) {
                tilSemester.setError("Semester is required");
                isValid = false;
            } else {
                tilSemester.setError(null);
            }

            String creditsStr = etCredits.getText().toString().trim();
            int credits = 0;
            if (creditsStr.isEmpty()) {
                tilCredits.setError("Credits are required");
                isValid = false;
            } else {
                try {
                    credits = Integer.parseInt(creditsStr);
                    if (credits <= 0) {
                        tilCredits.setError("Credits must be a positive number");
                        isValid = false;
                    } else {
                        tilCredits.setError(null);
                    }
                } catch (NumberFormatException e) {
                    tilCredits.setError("Credits must be a number");
                    isValid = false;
                }
            }

            String description = etDescription.getText().toString().trim();

            // If all inputs are valid, create and add the course
            if (isValid) {
                Course newCourse = new Course();
                newCourse.setCourseCode(courseCode);
                newCourse.setCourseName(courseName);
                newCourse.setDepartment(department);
                newCourse.setSemester(semester);
                newCourse.setCredits(credits);
                newCourse.setDescription(description);
                newCourse.setInstructorId(currentInstructor.getUserId());
                newCourse.setStartDate(new Date()); // Set to current date for now
                newCourse.setEndDate(new Date(System.currentTimeMillis() + 7776000000L)); // ~3 months later

                // Add course to Firebase
                courseRepository.addCourse(newCourse, new CourseRepository.OnCompleteListener() {
                    @Override
                    public void onSuccess(String id) {
                        Toast.makeText(ManageCoursesActivity.this,
                                "Course added successfully", Toast.LENGTH_SHORT).show();

                        // Add course to instructor's list
                        currentInstructor.addCourse(id);

                        // Dismiss dialog
                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(ManageCoursesActivity.this,
                                "Failed to add course: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showEditCourseDialog(Course course) {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);

        TextInputLayout tilCourseCode = dialogView.findViewById(R.id.tilCourseCode);
        TextInputLayout tilCourseName = dialogView.findViewById(R.id.tilCourseName);
        TextInputLayout tilDepartment = dialogView.findViewById(R.id.tilDepartment);
        TextInputLayout tilSemester = dialogView.findViewById(R.id.tilSemester);
        TextInputLayout tilCredits = dialogView.findViewById(R.id.tilCredits);
        TextInputLayout tilDescription = dialogView.findViewById(R.id.tilDescription);

        TextInputEditText etCourseCode = dialogView.findViewById(R.id.etCourseCode);
        TextInputEditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        TextInputEditText etDepartment = dialogView.findViewById(R.id.etDepartment);
        TextInputEditText etSemester = dialogView.findViewById(R.id.etSemester);
        TextInputEditText etCredits = dialogView.findViewById(R.id.etCredits);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);

        // Fill with existing course data
        etCourseCode.setText(course.getCourseCode());
        etCourseName.setText(course.getCourseName());
        etDepartment.setText(course.getDepartment());
        etSemester.setText(course.getSemester());
        etCredits.setText(String.valueOf(course.getCredits()));
        if (course.getDescription() != null) {
            etDescription.setText(course.getDescription());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Course")
                .setView(dialogView)
                .setPositiveButton("Update", null) // We'll set this later to avoid auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Set positive button click listener after showing dialog to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate inputs
            boolean isValid = true;

            String courseCode = etCourseCode.getText().toString().trim();
            if (courseCode.isEmpty()) {
                tilCourseCode.setError("Course code is required");
                isValid = false;
            } else {
                tilCourseCode.setError(null);
            }

            String courseName = etCourseName.getText().toString().trim();
            if (courseName.isEmpty()) {
                tilCourseName.setError("Course name is required");
                isValid = false;
            } else {
                tilCourseName.setError(null);
            }

            String department = etDepartment.getText().toString().trim();
            if (department.isEmpty()) {
                tilDepartment.setError("Department is required");
                isValid = false;
            } else {
                tilDepartment.setError(null);
            }

            String semester = etSemester.getText().toString().trim();
            if (semester.isEmpty()) {
                tilSemester.setError("Semester is required");
                isValid = false;
            } else {
                tilSemester.setError(null);
            }

            String creditsStr = etCredits.getText().toString().trim();
            int credits = 0;
            if (creditsStr.isEmpty()) {
                tilCredits.setError("Credits are required");
                isValid = false;
            } else {
                try {
                    credits = Integer.parseInt(creditsStr);
                    if (credits <= 0) {
                        tilCredits.setError("Credits must be a positive number");
                        isValid = false;
                    } else {
                        tilCredits.setError(null);
                    }
                } catch (NumberFormatException e) {
                    tilCredits.setError("Credits must be a number");
                    isValid = false;
                }
            }

            String description = etDescription.getText().toString().trim();

            // If all inputs are valid, update the course
            if (isValid) {
                // Update course object with new values
                course.setCourseCode(courseCode);
                course.setCourseName(courseName);
                course.setDepartment(department);
                course.setSemester(semester);
                course.setCredits(credits);
                course.setDescription(description);

                // Update course in Firebase
                courseRepository.updateCourse(course, new CourseRepository.OnCompleteListener() {
                    @Override
                    public void onSuccess(String id) {
                        Toast.makeText(ManageCoursesActivity.this,
                                "Course updated successfully", Toast.LENGTH_SHORT).show();

                        // Dismiss dialog
                        dialog.dismiss();

                        // Refresh course list to show updated data
                        courseRepository.fetchCoursesByInstructor(currentInstructor.getUserId());
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(ManageCoursesActivity.this,
                                "Failed to update course: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onCourseClick(Course course, int position) {
        // TODO: Implement course details view
        Toast.makeText(this, "Course details coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewAttendanceClick(Course course, int position) {
        // TODO: Implement view attendance
        Toast.makeText(this, "View attendance coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onManageCourseClick(Course course, int position) {
        showEditCourseDialog(course);
    }

    @Override
    public void onDeleteCourseClick(Course course, int position) {
        showDeleteConfirmationDialog(course);
    }

    private void showDeleteConfirmationDialog(Course course) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse(course))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCourse(Course course) {
        courseRepository.deleteCourse(course.getCourseId(), new CourseRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                Toast.makeText(ManageCoursesActivity.this,
                        "Course deleted successfully", Toast.LENGTH_SHORT).show();

                // Remove course from instructor's list
                currentInstructor.removeCourse(id);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ManageCoursesActivity.this,
                        "Failed to delete course: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}