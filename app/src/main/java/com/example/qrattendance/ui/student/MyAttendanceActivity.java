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
import com.example.qrattendance.util.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    private FirebaseFirestore db;
    private Student currentStudent;
    private MyAttendanceAdapter attendanceAdapter;

    // Maps to store course and session details
    private Map<String, String> courseNames = new HashMap<>();
    private Map<String, String> sessionTitles = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_attendance);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get current student from session
        currentStudent = (Student) SessionManager.getInstance(this).getUserData();
        if (currentStudent == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();
        setupRecyclerView();
        loadAttendanceRecords();
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

    private void loadAttendanceRecords() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoAttendance.setVisibility(View.GONE);
        recyclerViewAttendance.setVisibility(View.GONE);

        // Direct Firestore query without using repository for more control
        db.collection("attendance_records")
                .whereEqualTo("studentId", currentStudent.getUserId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AttendanceRecord> records = new ArrayList<>();

                    if (queryDocumentSnapshots.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        tvNoAttendance.setVisibility(View.VISIBLE);
                        recyclerViewAttendance.setVisibility(View.GONE);
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        AttendanceRecord record = document.toObject(AttendanceRecord.class);
                        record.setRecordId(document.getId());
                        records.add(record);

                        // Fetch course and session info for each record
                        fetchCourseInfo(record.getCourseId());
                        fetchSessionInfo(record.getSessionId());
                    }

                    // Now we have all the attendance records
                    progressBar.setVisibility(View.GONE);

                    if (records.isEmpty()) {
                        tvNoAttendance.setVisibility(View.VISIBLE);
                        recyclerViewAttendance.setVisibility(View.GONE);
                    } else {
                        tvNoAttendance.setVisibility(View.GONE);
                        recyclerViewAttendance.setVisibility(View.VISIBLE);
                        attendanceAdapter.updateAttendanceRecords(records);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvNoAttendance.setText("Error loading attendance: " + e.getMessage());
                    tvNoAttendance.setVisibility(View.VISIBLE);
                    recyclerViewAttendance.setVisibility(View.GONE);
                });
    }

    private void fetchCourseInfo(String courseId) {
        if (courseId == null || courseNames.containsKey(courseId)) {
            return;
        }

        // Set a placeholder while fetching
        courseNames.put(courseId, "Loading...");

        db.collection("courses").document(courseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String courseCode = documentSnapshot.getString("courseCode");
                        String courseName = documentSnapshot.getString("courseName");
                        String displayName;

                        if (courseCode != null && courseName != null) {
                            displayName = courseCode + " - " + courseName;
                        } else if (courseName != null) {
                            displayName = courseName;
                        } else if (courseCode != null) {
                            displayName = courseCode;
                        } else {
                            displayName = "Unknown Course";
                        }

                        courseNames.put(courseId, displayName);
                        attendanceAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    courseNames.put(courseId, "Unknown Course");
                    attendanceAdapter.notifyDataSetChanged();
                });
    }

    private void fetchSessionInfo(String sessionId) {
        if (sessionId == null || sessionTitles.containsKey(sessionId)) {
            return;
        }

        // Set a placeholder while fetching
        sessionTitles.put(sessionId, "Loading...");

        db.collection("sessions").document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        sessionTitles.put(sessionId, title != null ? title : "Unnamed Session");
                        attendanceAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    sessionTitles.put(sessionId, "Unknown Session");
                    attendanceAdapter.notifyDataSetChanged();
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