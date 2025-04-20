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
import com.example.qrattendance.data.model.AttendanceRecord;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.repository.AttendanceRepository;
import com.example.qrattendance.util.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvStudentName;
    private TextView tvStudentRoll;
    private TextView tvNoAttendance;
    private RecyclerView recyclerViewAttendance;
    private ProgressBar progressBar;

    private AttendanceRepository attendanceRepository;
    private Student currentStudent;
    private MyAttendanceAdapter attendanceAdapter;

    // Maps to store course and session details
    private Map<String, String> courseNames = new HashMap<>();
    private Map<String, String> sessionTitles = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_attendance);

        // Get current student from session
        currentStudent = (Student) SessionManager.getInstance(this).getUserData();
        if (currentStudent == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repository
        attendanceRepository = AttendanceRepository.getInstance();

        // Initialize views
        initViews();
        setupRecyclerView();
        loadStudentAttendance();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentRoll = findViewById(R.id.tvStudentRoll);
        tvNoAttendance = findViewById(R.id.tvNoAttendance);
        recyclerViewAttendance = findViewById(R.id.recyclerViewAttendance);
        progressBar = findViewById(R.id.progressBar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Attendance");
        }

        // Set student information
        tvStudentName.setText(currentStudent.getName());
        tvStudentRoll.setText(currentStudent.getRollNumber());
    }

    private void setupRecyclerView() {
        attendanceAdapter = new MyAttendanceAdapter(new ArrayList<>(), courseNames, sessionTitles);
        recyclerViewAttendance.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAttendance.setAdapter(attendanceAdapter);
    }

    private void loadStudentAttendance() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoAttendance.setVisibility(View.GONE);
        recyclerViewAttendance.setVisibility(View.GONE);

        // Observe attendance records
        attendanceRepository.getAttendanceRecords().observe(this, records -> {
            progressBar.setVisibility(View.GONE);

            if (records.isEmpty()) {
                tvNoAttendance.setVisibility(View.VISIBLE);
                recyclerViewAttendance.setVisibility(View.GONE);
            } else {
                // Fetch additional information for each record (course and session names)
                fetchAdditionalInfo(records);
            }
        });

        // Observe errors
        attendanceRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });

        // Fetch attendance records for this student
        attendanceRepository.fetchAttendanceByStudent(currentStudent.getUserId());
    }

    // Fetch course and session information for each attendance record
    private void fetchAdditionalInfo(List<AttendanceRecord> records) {
        courseNames.clear();
        sessionTitles.clear();

        // We'll count how many async operations we need to complete
        final int[] totalOperations = {records.size() * 2}; // Course + Session for each record
        final int[] completedOperations = {0};

        for (AttendanceRecord record : records) {
            // Fetch course info for each unique courseId
            String courseId = record.getCourseId();
            if (!courseNames.containsKey(courseId)) {
                fetchCourseInfo(courseId, () -> {
                    completedOperations[0]++;
                    checkIfAllCompleted(records, totalOperations[0], completedOperations[0]);
                });
            } else {
                completedOperations[0]++;
            }

            // Fetch session info for each unique sessionId
            String sessionId = record.getSessionId();
            if (!sessionTitles.containsKey(sessionId)) {
                fetchSessionInfo(sessionId, () -> {
                    completedOperations[0]++;
                    checkIfAllCompleted(records, totalOperations[0], completedOperations[0]);
                });
            } else {
                completedOperations[0]++;
            }
        }

        // In case there are no records or all information is already fetched
        checkIfAllCompleted(records, totalOperations[0], completedOperations[0]);
    }

    private void fetchCourseInfo(String courseId, Runnable onComplete) {
        if (courseId == null) {
            courseNames.put("null", "Unknown Course");
            onComplete.run();
            return;
        }

        attendanceRepository.fetchCourseInfo(courseId, (courseInfo, success) -> {
            courseNames.put(courseId, courseInfo);
            onComplete.run();
        });
    }

    private void fetchSessionInfo(String sessionId, Runnable onComplete) {
        if (sessionId == null) {
            sessionTitles.put("null", "Unknown Session");
            onComplete.run();
            return;
        }

        attendanceRepository.fetchSessionInfo(sessionId, (sessionInfo, success) -> {
            sessionTitles.put(sessionId, sessionInfo);
            onComplete.run();
        });
    }

    private void checkIfAllCompleted(List<AttendanceRecord> records, int total, int completed) {
        if (completed >= total) {
            // All info has been fetched, update the adapter
            tvNoAttendance.setVisibility(View.GONE);
            recyclerViewAttendance.setVisibility(View.VISIBLE);
            attendanceAdapter.updateAttendanceRecords(records);
        }
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