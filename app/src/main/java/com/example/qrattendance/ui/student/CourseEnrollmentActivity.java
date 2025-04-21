package com.example.qrattendance.ui.student;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.repository.CourseRepository;
import com.example.qrattendance.util.SessionManager;
import com.example.qrattendance.util.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class CourseEnrollmentActivity extends AppCompatActivity implements CourseEnrollmentAdapter.CourseEnrollmentListener {

    private Toolbar toolbar;
    private RecyclerView recyclerViewCourses;
    private TextView tvNoCourses;
    private ProgressBar progressBar;

    private CourseRepository courseRepository;
    private Student currentStudent;
    private CourseEnrollmentAdapter courseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_enrollment);

        // Get current student from session
        currentStudent = (Student) SessionManager.getInstance(this).getUserData();
        if (currentStudent == null) {
            UIHelper.showErrorDialog(this, "Session Error", "Please log in again.");
            finish();
            return;
        }

        // Initialize repository
        courseRepository = CourseRepository.getInstance();

        // Initialize views
        initViews();
        setupRecyclerView();
        loadAvailableCourses();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewCourses = findViewById(R.id.recyclerViewCourses);
        tvNoCourses = findViewById(R.id.tvNoCourses);
        progressBar = findViewById(R.id.progressBar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Course Enrollment");
        }
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseEnrollmentAdapter(new ArrayList<>(), currentStudent.getEnrolledCourseIds(), this);
        recyclerViewCourses.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCourses.setAdapter(courseAdapter);
    }

    private void loadAvailableCourses() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoCourses.setVisibility(View.GONE);
        recyclerViewCourses.setVisibility(View.GONE);

        // Observe all available courses
        courseRepository.getAllCourses().observe(this, courses -> {
            progressBar.setVisibility(View.GONE);

            if (courses.isEmpty()) {
                tvNoCourses.setVisibility(View.VISIBLE);
                recyclerViewCourses.setVisibility(View.GONE);
            } else {
                tvNoCourses.setVisibility(View.GONE);
                recyclerViewCourses.setVisibility(View.VISIBLE);
                courseAdapter.updateCourses(courses);
            }
        });

        // Observe errors
        courseRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                UIHelper.showErrorToast(this, errorMsg);
                progressBar.setVisibility(View.GONE);
            }
        });

        // Fetch all available courses
        courseRepository.fetchAllActiveCourses();
    }

    @Override
    public void onEnrollClicked(Course course) {
        progressBar.setVisibility(View.VISIBLE);
        courseRepository.enrollStudent(course.getCourseId(), currentStudent.getUserId(), new CourseRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                progressBar.setVisibility(View.GONE);

                // Update local student object with new course
                if (currentStudent.getEnrolledCourseIds() == null) {
                    currentStudent.setEnrolledCourseIds(new ArrayList<>());
                }
                currentStudent.enrollInCourse(course.getCourseId());

                // Update SessionManager with updated student data
                SessionManager.getInstance(CourseEnrollmentActivity.this).saveUserSession(currentStudent);

                // Update the adapter to reflect the change
                courseAdapter.updateEnrolledCourses(currentStudent.getEnrolledCourseIds());

                UIHelper.showSuccessToast(CourseEnrollmentActivity.this,
                        "Successfully enrolled in " + course.getCourseName());
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                UIHelper.showErrorDialog(CourseEnrollmentActivity.this, "Enrollment Failed", errorMessage);
            }
        });
    }

    @Override
    public void onUnenrollClicked(Course course) {
        progressBar.setVisibility(View.VISIBLE);
        courseRepository.unenrollStudent(course.getCourseId(), currentStudent.getUserId(), new CourseRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                progressBar.setVisibility(View.GONE);

                // Update local student object by removing course
                currentStudent.unenrollFromCourse(course.getCourseId());

                // Update SessionManager with updated student data
                SessionManager.getInstance(CourseEnrollmentActivity.this).saveUserSession(currentStudent);

                // Update the adapter to reflect the change
                courseAdapter.updateEnrolledCourses(currentStudent.getEnrolledCourseIds());

                UIHelper.showSuccessToast(CourseEnrollmentActivity.this,
                        "Successfully unenrolled from " + course.getCourseName());
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                UIHelper.showErrorDialog(CourseEnrollmentActivity.this, "Unenrollment Failed", errorMessage);
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