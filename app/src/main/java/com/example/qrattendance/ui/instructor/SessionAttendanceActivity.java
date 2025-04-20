package com.example.qrattendance.ui.instructor;

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
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.model.Session;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.repository.AttendanceRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SessionAttendanceActivity extends AppCompatActivity {

    private String sessionId;
    private AttendanceRepository attendanceRepository;

    private Toolbar toolbar;
    private TextView tvSessionTitle;
    private TextView tvSessionDate;
    private TextView tvSessionTime;
    private TextView tvSessionLocation;
    private TextView tvAttendanceStats;
    private TextView tvNoAttendance;
    private RecyclerView recyclerViewAttendance;
    private ProgressBar progressBar;

    private AttendanceRecordsAdapter attendanceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_attendance);

        // Get session ID from intent
        sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null) {
            Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repository
        attendanceRepository = AttendanceRepository.getInstance();

        // Initialize views
        initViews();
        setupRecyclerView();
        loadSessionDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvSessionTitle = findViewById(R.id.tvSessionTitle);
        tvSessionDate = findViewById(R.id.tvSessionDate);
        tvSessionTime = findViewById(R.id.tvSessionTime);
        tvSessionLocation = findViewById(R.id.tvSessionLocation);
        tvAttendanceStats = findViewById(R.id.tvAttendanceStats);
        tvNoAttendance = findViewById(R.id.tvNoAttendance);
        recyclerViewAttendance = findViewById(R.id.recyclerViewAttendance);
        progressBar = findViewById(R.id.progressBar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Session Attendance");
        }
    }

    private void setupRecyclerView() {
        attendanceAdapter = new AttendanceRecordsAdapter(new ArrayList<>());
        recyclerViewAttendance.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAttendance.setAdapter(attendanceAdapter);
    }

    private void loadSessionDetails() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoAttendance.setVisibility(View.GONE);
        recyclerViewAttendance.setVisibility(View.GONE);

        // Observe session details
        attendanceRepository.getSessionDetails().observe(this, sessionDetails -> {
            progressBar.setVisibility(View.GONE);

            if (sessionDetails != null) {
                displaySessionDetails(sessionDetails);
            } else {
                Toast.makeText(this, "Failed to load session details", Toast.LENGTH_SHORT).show();
            }
        });

        // Observe error messages
        attendanceRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });

        // Load the session details
        attendanceRepository.fetchSessionWithAttendanceDetails(sessionId);
    }

    private void displaySessionDetails(Map<String, Object> sessionDetails) {
        Session session = (Session) sessionDetails.get("session");
        Course course = (Course) sessionDetails.get("course");
        List<AttendanceRecord> records = (List<AttendanceRecord>) sessionDetails.get("attendanceRecords");
        Map<String, Student> students = (Map<String, Student>) sessionDetails.get("students");
        Map<String, Object> stats = (Map<String, Object>) sessionDetails.get("stats");

        if (session != null) {
            // Set session details
            tvSessionTitle.setText(session.getTitle());

            // Format date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            Date startTime = session.getStartTime();
            if (startTime != null) {
                tvSessionDate.setText(dateFormat.format(startTime));
                tvSessionTime.setText(timeFormat.format(startTime));
            } else {
                tvSessionDate.setText("No date");
                tvSessionTime.setText("No time");
            }

            tvSessionLocation.setText(session.getLocation());

            // Set toolbar title with course info if available
            if (course != null) {
                getSupportActionBar().setTitle(course.getCourseCode() + " Attendance");
            }

            // Display attendance statistics
            if (stats != null) {
                int totalPresent = stats.containsKey("totalPresent") ? (int) stats.get("totalPresent") : 0;
                int totalStudents = stats.containsKey("totalStudents") ? (int) stats.get("totalStudents") : 0;
                int absentCount = stats.containsKey("absentCount") ? (int) stats.get("absentCount") : 0;
                double percentage = stats.containsKey("attendancePercentage") ? (double) stats.get("attendancePercentage") : 0.0;

                tvAttendanceStats.setText(String.format(Locale.getDefault(),
                        "%d/%d students present (%.1f%%) • %d absent",
                        totalPresent, totalStudents, percentage, absentCount));
            }

            // Display attendance records
            if (records != null && !records.isEmpty()) {
                // Create attendance model objects that combine record data with student info
                List<AttendanceModel> attendanceModels = new ArrayList<>();

                for (AttendanceRecord record : records) {
                    String studentId = record.getStudentId();
                    Student student = students != null ? students.get(studentId) : null;

                    String studentName = student != null ? student.getName() : "Unknown Student";
                    String studentRoll = student != null ? student.getRollNumber() : "No Roll Number";

                    AttendanceModel model = new AttendanceModel(record, studentName, studentRoll);
                    attendanceModels.add(model);
                }

                // Update adapter
                tvNoAttendance.setVisibility(View.GONE);
                recyclerViewAttendance.setVisibility(View.VISIBLE);
                attendanceAdapter.updateAttendanceRecords(attendanceModels);
            } else {
                tvNoAttendance.setVisibility(View.VISIBLE);
                recyclerViewAttendance.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(this, "Failed to load session data", Toast.LENGTH_SHORT).show();
            finish();
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

    // Class to hold combined attendance data for display
    public static class AttendanceModel {
        private final AttendanceRecord record;
        private final String studentName;
        private final String studentRoll;

        public AttendanceModel(AttendanceRecord record, String studentName, String studentRoll) {
            this.record = record;
            this.studentName = studentName;
            this.studentRoll = studentRoll;
        }

        public AttendanceRecord getRecord() {
            return record;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStudentRoll() {
            return studentRoll;
        }
    }
}