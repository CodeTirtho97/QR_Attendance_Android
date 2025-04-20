package com.example.qrattendance.ui.instructor;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.data.model.Session;
import com.example.qrattendance.data.repository.CourseRepository;
import com.example.qrattendance.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ViewAttendanceActivity extends AppCompatActivity implements SessionsAdapter.SessionClickListener {

    private Toolbar toolbar;
    private Spinner spinnerCourses;
    private RecyclerView recyclerViewSessions;
    private TextView tvNoSessions;
    private ProgressBar progressBar;

    private CourseRepository courseRepository;
    private Instructor currentInstructor;

    private List<Course> instructorCourses = new ArrayList<>();
    private SessionsAdapter sessionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        // Initialize repositories
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
        loadInstructorCourses();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerCourses = findViewById(R.id.spinnerCourses);
        recyclerViewSessions = findViewById(R.id.recyclerViewSessions);
        tvNoSessions = findViewById(R.id.tvNoSessions);
        progressBar = findViewById(R.id.progressBar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("View Attendance");
        }

        // Set up courses spinner
        spinnerCourses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= instructorCourses.size()) {
                    // Subtract 1 from position because of the "Select Course" item at position 0
                    Course selectedCourse = instructorCourses.get(position - 1);
                    loadSessionsForCourse(selectedCourse.getCourseId());
                } else {
                    // Clear sessions list
                    sessionsAdapter.updateSessions(new ArrayList<>());
                    tvNoSessions.setVisibility(View.VISIBLE);
                    recyclerViewSessions.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupRecyclerView() {
        sessionsAdapter = new SessionsAdapter(new ArrayList<>(), this);
        recyclerViewSessions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSessions.setAdapter(sessionsAdapter);
    }

    private void loadInstructorCourses() {
        progressBar.setVisibility(View.VISIBLE);

        // Observe courses
        courseRepository.getCourses().observe(this, courses -> {
            progressBar.setVisibility(View.GONE);

            if (courses.isEmpty()) {
                Toast.makeText(this, "You don't have any courses yet", Toast.LENGTH_SHORT).show();
            } else {
                instructorCourses = courses;
                setupCoursesSpinner();
            }
        });

        // Observe errors
        courseRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });

        // Load the instructor's courses
        courseRepository.fetchCoursesByInstructor(currentInstructor.getUserId());
    }

    private void setupCoursesSpinner() {
        // Create array with "Select Course" as first item, followed by actual courses
        String[] courseItems = new String[instructorCourses.size() + 1];
        courseItems[0] = "Select Course";

        for (int i = 0; i < instructorCourses.size(); i++) {
            Course course = instructorCourses.get(i);
            courseItems[i + 1] = course.getCourseCode() + " - " + course.getCourseName();
        }

        // Set up spinner adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, courseItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapter);
    }

    private void loadSessionsForCourse(String courseId) {
        progressBar.setVisibility(View.VISIBLE);
        tvNoSessions.setVisibility(View.GONE);
        recyclerViewSessions.setVisibility(View.GONE);

        // Get sessions for the selected course
        courseRepository.getSessions().observe(this, sessions -> {
            progressBar.setVisibility(View.GONE);

            if (sessions.isEmpty()) {
                tvNoSessions.setVisibility(View.VISIBLE);
                recyclerViewSessions.setVisibility(View.GONE);
            } else {
                tvNoSessions.setVisibility(View.GONE);
                recyclerViewSessions.setVisibility(View.VISIBLE);
                sessionsAdapter.updateSessions(sessions);
            }
        });

        // Get sessions for this course
        courseRepository.fetchSessionsByCourse(courseId);
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
    public void onSessionClick(Session session) {
        // Open session attendance details
        Intent intent = new Intent(this, SessionAttendanceActivity.class);
        intent.putExtra("sessionId", session.getSessionId());
        startActivity(intent);
    }
}