package com.example.qrattendance.ui.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.repository.CourseRepository;
import com.example.qrattendance.data.repository.UserRepository;
import com.example.qrattendance.util.SessionManager;
import com.example.qrattendance.util.UIHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddEditCourseActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputLayout tilCourseCode, tilCourseName, tilDepartment, tilDescription, tilSemester, tilCredits;
    private TextInputEditText etCourseCode, etCourseName, etDepartment, etDescription, etSemester, etCredits;
    private TextInputLayout tilStartDate, tilEndDate;
    private TextInputEditText etStartDate, etEndDate;
    private Spinner spinnerInstructor;
    private SwitchMaterial switchActive;
    private Button btnSave;
    private ProgressBar progressBar;

    private CourseRepository courseRepository;
    private UserRepository userRepository;
    private Admin currentAdmin;
    private Course currentCourse;
    private String courseId;
    private boolean isEditMode = false;
    private List<Map<String, Object>> instructorsList = new ArrayList<>();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_course);

        // Initialize repositories
        courseRepository = CourseRepository.getInstance();
        userRepository = UserRepository.getInstance();

        // Get current admin from session
        currentAdmin = (Admin) SessionManager.getInstance(this).getUserData();
        if (currentAdmin == null) {
            UIHelper.showErrorDialog(this, "Session Error", "Please log in again.");
            finish();
            return;
        }

        // Get course ID if in edit mode
        if (getIntent().hasExtra("courseId")) {
            courseId = getIntent().getStringExtra("courseId");
            isEditMode = true;
        }

        // Initialize views
        initViews();
        setupEventListeners();

        // Check if editing existing course
        if (isEditMode) {
            setTitle("Edit Course");
            loadCourseData();
        } else {
            setTitle("Add New Course");
        }

        // Load instructors for spinner
        loadInstructors();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tilCourseCode = findViewById(R.id.tilCourseCode);
        tilCourseName = findViewById(R.id.tilCourseName);
        tilDepartment = findViewById(R.id.tilDepartment);
        tilDescription = findViewById(R.id.tilDescription);
        tilSemester = findViewById(R.id.tilSemester);
        tilCredits = findViewById(R.id.tilCredits);
        tilStartDate = findViewById(R.id.tilStartDate);
        tilEndDate = findViewById(R.id.tilEndDate);

        etCourseCode = findViewById(R.id.etCourseCode);
        etCourseName = findViewById(R.id.etCourseName);
        etDepartment = findViewById(R.id.etDepartment);
        etDescription = findViewById(R.id.etDescription);
        etSemester = findViewById(R.id.etSemester);
        etCredits = findViewById(R.id.etCredits);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);

        spinnerInstructor = findViewById(R.id.spinnerInstructor);
        switchActive = findViewById(R.id.switchActive);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupEventListeners() {
        // Setup date pickers
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        // Setup save button
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                if (isEditMode) {
                    updateCourse();
                } else {
                    createCourse();
                }
            }
        });
    }

    private void loadInstructors() {
        progressBar.setVisibility(View.VISIBLE);

        userRepository.fetchInstructors(new UserRepository.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<Map<String, Object>> instructors) {
                instructorsList = instructors;
                List<String> instructorNames = new ArrayList<>();

                // Add placeholder for instructor selection
                instructorNames.add("Select Instructor...");

                // Add instructors to list
                for (Map<String, Object> instructor : instructors) {
                    String name = (String) instructor.get("name");
                    String employeeId = (String) instructor.get("employeeId");

                    String displayText;
                    if (name != null && employeeId != null) {
                        displayText = name + " (" + employeeId + ")";
                    } else if (name != null) {
                        displayText = name;
                    } else if (employeeId != null) {
                        displayText = "Instructor (" + employeeId + ")";
                    } else {
                        displayText = "Unknown Instructor";
                    }

                    instructorNames.add(displayText);
                }

                // Create adapter for spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        AddEditCourseActivity.this,
                        android.R.layout.simple_spinner_item,
                        instructorNames);

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerInstructor.setAdapter(adapter);

                // If editing, select the current instructor
                if (isEditMode && currentCourse != null) {
                    selectInstructorInSpinner(currentCourse.getInstructorId());
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                UIHelper.showErrorDialog(AddEditCourseActivity.this,
                        "Error", "Failed to load instructors: " + errorMessage);
            }
        });
    }

    private void selectInstructorInSpinner(String instructorId) {
        if (instructorId == null || instructorId.isEmpty()) return;

        for (int i = 0; i < instructorsList.size(); i++) {
            Map<String, Object> instructor = instructorsList.get(i);
            if (instructorId.equals(instructor.get("userId"))) {
                // +1 because position 0 is the "Select Instructor..." placeholder
                spinnerInstructor.setSelection(i + 1);
                break;
            }
        }
    }

    private void loadCourseData() {
        progressBar.setVisibility(View.VISIBLE);

        courseRepository.getCourseById(courseId, new CourseRepository.OnCourseListener() {
            @Override
            public void onCourseLoaded(Course course) {
                currentCourse = course;

                etCourseCode.setText(course.getCourseCode());
                etCourseName.setText(course.getCourseName());
                etDepartment.setText(course.getDepartment());
                etDescription.setText(course.getDescription());
                etSemester.setText(course.getSemester());
                etCredits.setText(String.valueOf(course.getCredits()));

                if (course.getStartDate() != null) {
                    etStartDate.setText(dateFormatter.format(course.getStartDate()));
                }

                if (course.getEndDate() != null) {
                    etEndDate.setText(dateFormatter.format(course.getEndDate()));
                }

                switchActive.setChecked(course.isActive());

                selectInstructorInSpinner(course.getInstructorId());

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                UIHelper.showErrorDialog(AddEditCourseActivity.this,
                        "Error", "Failed to load course: " + errorMessage);
            }
        });
    }

    private void showDatePicker(TextInputEditText dateField) {
        Calendar calendar = Calendar.getInstance();

        // If date field already has a value, use it
        if (!TextUtils.isEmpty(dateField.getText())) {
            try {
                Date date = dateFormatter.parse(dateField.getText().toString());
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (ParseException e) {
                // Use current date if parsing fails
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    dateField.setText(dateFormatter.format(calendar.getTime()));
                },
                year, month, day);

        datePickerDialog.show();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate course code
        if (TextUtils.isEmpty(etCourseCode.getText())) {
            tilCourseCode.setError("Course code is required");
            isValid = false;
        } else {
            tilCourseCode.setError(null);
        }

        // Validate course name
        if (TextUtils.isEmpty(etCourseName.getText())) {
            tilCourseName.setError("Course name is required");
            isValid = false;
        } else {
            tilCourseName.setError(null);
        }

        // Validate department
        if (TextUtils.isEmpty(etDepartment.getText())) {
            tilDepartment.setError("Department is required");
            isValid = false;
        } else {
            tilDepartment.setError(null);
        }

        // Validate semester
        if (TextUtils.isEmpty(etSemester.getText())) {
            tilSemester.setError("Semester is required");
            isValid = false;
        } else {
            tilSemester.setError(null);
        }

        // Validate credits
        if (TextUtils.isEmpty(etCredits.getText())) {
            tilCredits.setError("Credits are required");
            isValid = false;
        } else {
            try {
                int credits = Integer.parseInt(etCredits.getText().toString());
                if (credits <= 0) {
                    tilCredits.setError("Credits must be greater than 0");
                    isValid = false;
                } else {
                    tilCredits.setError(null);
                }
            } catch (NumberFormatException e) {
                tilCredits.setError("Please enter a valid number");
                isValid = false;
            }
        }

        // Validate instructor selection
        if (spinnerInstructor.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select an instructor", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate dates
        if (TextUtils.isEmpty(etStartDate.getText())) {
            tilStartDate.setError("Start date is required");
            isValid = false;
        } else {
            tilStartDate.setError(null);
        }

        if (TextUtils.isEmpty(etEndDate.getText())) {
            tilEndDate.setError("End date is required");
            isValid = false;
        } else {
            // Check that end date is after start date
            try {
                Date startDate = dateFormatter.parse(etStartDate.getText().toString());
                Date endDate = dateFormatter.parse(etEndDate.getText().toString());

                if (startDate != null && endDate != null && endDate.before(startDate)) {
                    tilEndDate.setError("End date must be after start date");
                    isValid = false;
                } else {
                    tilEndDate.setError(null);
                }
            } catch (ParseException e) {
                tilEndDate.setError("Invalid date format");
                isValid = false;
            }
        }

        return isValid;
    }

    private Course getCourseFromInputs() {
        Course course = new Course();

        if (isEditMode && currentCourse != null) {
            course = currentCourse;
        }

        course.setCourseCode(etCourseCode.getText().toString().trim());
        course.setCourseName(etCourseName.getText().toString().trim());
        course.setDepartment(etDepartment.getText().toString().trim());
        course.setDescription(etDescription.getText().toString().trim());
        course.setSemester(etSemester.getText().toString().trim());

        try {
            course.setCredits(Integer.parseInt(etCredits.getText().toString().trim()));
        } catch (NumberFormatException e) {
            course.setCredits(0);
        }

        // Get instructor ID from spinner
        if (spinnerInstructor.getSelectedItemPosition() > 0) {
            // -1 because position 0 is the placeholder
            Map<String, Object> selectedInstructor = instructorsList.get(spinnerInstructor.getSelectedItemPosition() - 1);
            course.setInstructorId((String) selectedInstructor.get("userId"));
        }

        // Parse dates
        try {
            course.setStartDate(dateFormatter.parse(etStartDate.getText().toString()));
            course.setEndDate(dateFormatter.parse(etEndDate.getText().toString()));
        } catch (ParseException e) {
            // Use current date if parsing fails
            course.setStartDate(new Date());
            course.setEndDate(new Date());
        }

        course.setActive(switchActive.isChecked());

        // Make sure lists are initialized
        if (course.getEnrolledStudentIds() == null) {
            course.setEnrolledStudentIds(new ArrayList<>());
        }

        if (course.getSessionIds() == null) {
            course.setSessionIds(new ArrayList<>());
        }

        return course;
    }

    private void createCourse() {
        progressBar.setVisibility(View.VISIBLE);

        Course course = getCourseFromInputs();

        courseRepository.addCourse(course, new CourseRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                // Update the instructor's course list
                updateInstructorCourseList(course.getInstructorId(), id);

                Toast.makeText(AddEditCourseActivity.this,
                        "Course created successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                UIHelper.showErrorDialog(AddEditCourseActivity.this,
                        "Error", "Failed to create course: " + errorMessage);
            }
        });
    }

    private void updateCourse() {
        progressBar.setVisibility(View.VISIBLE);

        Course course = getCourseFromInputs();

        // Check if instructor has changed
        String originalInstructorId = currentCourse.getInstructorId();
        String newInstructorId = course.getInstructorId();

        courseRepository.updateCourse(course, new CourseRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String id) {
                // If instructor changed, update both instructors' course lists
                if (!originalInstructorId.equals(newInstructorId)) {
                    // Remove from old instructor
                    removeFromInstructorCourseList(originalInstructorId, courseId);

                    // Add to new instructor
                    updateInstructorCourseList(newInstructorId, courseId);
                }

                Toast.makeText(AddEditCourseActivity.this,
                        "Course updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                UIHelper.showErrorDialog(AddEditCourseActivity.this,
                        "Error", "Failed to update course: " + errorMessage);
            }
        });
    }

    private void updateInstructorCourseList(String instructorId, String courseId) {
        if (instructorId == null || courseId == null) return;

        // Get instructor data
        userRepository.getUserById(instructorId, new UserRepository.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(Map<String, Object> userData) {
                // Get current course list
                List<String> coursesIds = (List<String>) userData.get("coursesIds");
                if (coursesIds == null) {
                    coursesIds = new ArrayList<>();
                }

                // Add course if not already present
                if (!coursesIds.contains(courseId)) {
                    coursesIds.add(courseId);

                    // Update instructor with new course list
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("coursesIds", coursesIds);

                    userRepository.updateUser(instructorId, updateData, new UserRepository.OnCompleteListener() {
                        @Override
                        public void onSuccess() {
                            // Success - continue with UI update
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Non-critical error - just log it
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Non-critical error - just log it
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void removeFromInstructorCourseList(String instructorId, String courseId) {
        if (instructorId == null || courseId == null) return;

        // Get instructor data
        userRepository.getUserById(instructorId, new UserRepository.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(Map<String, Object> userData) {
                // Get current course list
                List<String> coursesIds = (List<String>) userData.get("coursesIds");
                if (coursesIds == null || !coursesIds.contains(courseId)) {
                    return;
                }

                // Remove the course
                coursesIds.remove(courseId);

                // Update instructor with new course list
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("coursesIds", coursesIds);

                userRepository.updateUser(instructorId, updateData, null);
            }

            @Override
            public void onError(String errorMessage) {
                // Non-critical error - just ignore it
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