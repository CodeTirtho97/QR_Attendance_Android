package com.example.qrattendance.ui.common;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.model.User;
import com.example.qrattendance.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvName, tvEmail, tvPhoneNumber, tvUserType;
    private TextView tvRollNumber, tvDepartment, tvSemester, tvBatch; // Student fields
    private TextView tvEmployeeId, tvDesignation; // Instructor fields
    private View layoutStudentDetails, layoutInstructorDetails;
    private TextView tvAdminMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Get current user from session
        User currentUser = SessionManager.getInstance(this).getUserData();
        if (currentUser == null) {
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Display user details
        displayUserDetails(currentUser);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvUserType = findViewById(R.id.tvUserType);

        // Student-specific views
        tvRollNumber = findViewById(R.id.tvRollNumber);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvSemester = findViewById(R.id.tvSemester);
        tvBatch = findViewById(R.id.tvBatch);
        layoutStudentDetails = findViewById(R.id.layoutStudentDetails);

        // Instructor-specific views
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvDesignation = findViewById(R.id.tvDesignation);
        layoutInstructorDetails = findViewById(R.id.layoutInstructorDetails);

        // Admin message
        tvAdminMessage = findViewById(R.id.tvAdminMessage);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }
    }

    private void displayUserDetails(User user) {
        // Display common user details
        tvName.setText(user.getName());
        tvEmail.setText(user.getEmail());
        tvPhoneNumber.setText(user.getPhoneNumber());

        // Set user type and role-specific fields
        if (user instanceof Student) {
            Student student = (Student) user;
            tvUserType.setText("Student");

            // Show student details
            layoutStudentDetails.setVisibility(View.VISIBLE);
            layoutInstructorDetails.setVisibility(View.GONE);

            // Set student-specific fields
            tvRollNumber.setText(student.getRollNumber());
            tvDepartment.setText(student.getDepartment());
            tvSemester.setText(student.getSemester());
            tvBatch.setText(student.getBatch());

        } else if (user instanceof Instructor) {
            Instructor instructor = (Instructor) user;
            tvUserType.setText("Instructor");

            // Show instructor details
            layoutStudentDetails.setVisibility(View.GONE);
            layoutInstructorDetails.setVisibility(View.VISIBLE);

            // Set instructor-specific fields
            tvEmployeeId.setText(instructor.getEmployeeId());
            tvDepartment.setText(instructor.getDepartment());
            tvDesignation.setText(instructor.getDesignation());

        } else if (user instanceof Admin) {
            tvUserType.setText("Administrator");

            // Hide both detail sections for admin
            layoutStudentDetails.setVisibility(View.GONE);
            layoutInstructorDetails.setVisibility(View.GONE);
        }

        // Set creation date if available
        if (user.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            TextView tvCreatedAt = findViewById(R.id.tvCreatedAt);
            tvCreatedAt.setText("Account created: " + sdf.format(user.getCreatedAt()));
            tvCreatedAt.setVisibility(View.VISIBLE);
        }

        // Display admin message
        tvAdminMessage.setVisibility(View.VISIBLE);
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