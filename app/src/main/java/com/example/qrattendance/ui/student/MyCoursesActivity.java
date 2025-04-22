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
import com.example.qrattendance.util.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyCoursesActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private TextView tvNoCourses;
    private ProgressBar progressBar;
    private Student currentStudent;
    private FirebaseFirestore firestore;
    private MyCourseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_courses);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

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
        loadEnrolledCourses();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerViewCourses);
        tvNoCourses = findViewById(R.id.tvNoCourses);
        progressBar = findViewById(R.id.progressBar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Courses");
        }
    }

    private void setupRecyclerView() {
        adapter = new MyCourseAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadEnrolledCourses() {
        progressBar.setVisibility(View.VISIBLE);

        List<String> enrolledCourseIds = currentStudent.getEnrolledCourseIds();

        if (enrolledCourseIds == null || enrolledCourseIds.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            tvNoCourses.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        List<Course> courses = new ArrayList<>();
        final int[] completedQueries = {0};
        final int totalCourses = enrolledCourseIds.size();

        for (String courseId : enrolledCourseIds) {
            firestore.collection("courses").document(courseId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        completedQueries[0]++;

                        if (documentSnapshot.exists()) {
                            Course course = documentSnapshot.toObject(Course.class);
                            if (course != null) {
                                course.setCourseId(documentSnapshot.getId());
                                courses.add(course);
                            }
                        }

                        // Check if all courses have been fetched
                        if (completedQueries[0] >= totalCourses) {
                            progressBar.setVisibility(View.GONE);

                            if (courses.isEmpty()) {
                                tvNoCourses.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                tvNoCourses.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                                adapter.updateCourses(courses);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedQueries[0]++;

                        // Check if all courses have been fetched
                        if (completedQueries[0] >= totalCourses) {
                            progressBar.setVisibility(View.GONE);

                            if (courses.isEmpty()) {
                                tvNoCourses.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                tvNoCourses.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                                adapter.updateCourses(courses);
                            }
                        }
                    });
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