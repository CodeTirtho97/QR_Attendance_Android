package com.example.qrattendance.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.model.Session;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseRepository {
    private static final String TAG = "CourseRepository";
    private static final String COURSES_COLLECTION = "courses";
    private static final String USERS_COLLECTION = "users";

    private static CourseRepository instance;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<List<Course>> coursesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Course>> allCoursesLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Session>> sessionsLiveData = new MutableLiveData<>();

    // Private constructor for singleton pattern
    private CourseRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    // Get singleton instance
    public static synchronized CourseRepository getInstance() {
        if (instance == null) {
            instance = new CourseRepository();
        }
        return instance;
    }

    // Get all courses for a specific instructor
    public LiveData<List<Course>> getCourses() {
        return coursesLiveData;
    }

    // Get all courses in the system
    public LiveData<List<Course>> getAllCourses() {
        return allCoursesLiveData;
    }

    // Get error message
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Get loading state
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<List<Session>> getSessions() {
        return sessionsLiveData;
    }

    // Fetch courses by instructor ID
    public void fetchCoursesByInstructor(String instructorId) {
        isLoading.setValue(true);

        firestore.collection(COURSES_COLLECTION)
                .whereEqualTo("instructorId", instructorId)
                //.orderBy("courseName", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        errorMessage.setValue("Failed to load courses: " + e.getMessage());
                        isLoading.setValue(false);
                        return;
                    }

                    List<Course> courses = new ArrayList<>();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot document : snapshots) {
                            Course course = document.toObject(Course.class);
                            course.setCourseId(document.getId());
                            courses.add(course);
                        }
                    }

                    coursesLiveData.setValue(courses);
                    isLoading.setValue(false);
                });
    }

    // Fetch all active courses in the system
    public void fetchAllActiveCourses() {
        isLoading.setValue(true);

        // Use a simpler query that filters by isActive but doesn't sort (to avoid index issues)
        firestore.collection(COURSES_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Course> courses = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Course course = document.toObject(Course.class);
                        if (course != null) {
                            course.setCourseId(document.getId());
                            courses.add(course);
                        }
                    }

                    // Sort locally instead of in the query
                    Collections.sort(courses, (a, b) -> {
                        if (a.getCourseName() == null && b.getCourseName() == null) return 0;
                        if (a.getCourseName() == null) return -1;
                        if (b.getCourseName() == null) return 1;
                        return a.getCourseName().compareToIgnoreCase(b.getCourseName());
                    });

                    allCoursesLiveData.setValue(courses);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to load courses: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    public void fetchSessionsByCourse(String courseId) {
        isLoading.setValue(true);

        // Make sure the query is properly filtering by courseId
        firestore.collection("sessions")
                .whereEqualTo("courseId", courseId) // This is crucial - ensure it's being applied correctly
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get() // Use get() instead of addSnapshotListener for one-time loading
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Session> sessions = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Session session = document.toObject(Session.class);
                        if (session != null) {
                            session.setSessionId(document.getId());
                            session.updateStatus();
                            sessions.add(session);
                        }
                    }

                    Log.d("CourseRepository", "Fetching sessions for courseId: " + courseId);
                    Log.d("CourseRepository", "Found " + sessions.size() + " sessions");

                    sessionsLiveData.setValue(sessions);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to load sessions: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // Add new course
    public void addCourse(Course course, OnCompleteListener listener) {
        isLoading.setValue(true);

        Map<String, Object> courseMap = courseToMap(course);

        firestore.collection(COURSES_COLLECTION)
                .add(courseMap)
                .addOnSuccessListener(documentReference -> {
                    String courseId = documentReference.getId();
                    course.setCourseId(courseId);
                    isLoading.setValue(false);
                    listener.onSuccess(courseId);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to add course: " + e.getMessage());
                    isLoading.setValue(false);
                    listener.onFailure(e.getMessage());
                });
    }

    // Update existing course
    public void updateCourse(Course course, OnCompleteListener listener) {
        isLoading.setValue(true);

        if (course.getCourseId() == null) {
            errorMessage.setValue("Course ID cannot be null");
            isLoading.setValue(false);
            listener.onFailure("Course ID cannot be null");
            return;
        }

        Map<String, Object> courseMap = courseToMap(course);

        firestore.collection(COURSES_COLLECTION)
                .document(course.getCourseId())
                .update(courseMap)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    listener.onSuccess(course.getCourseId());
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to update course: " + e.getMessage());
                    isLoading.setValue(false);
                    listener.onFailure(e.getMessage());
                });
    }

    // Delete course
    public void deleteCourse(String courseId, OnCompleteListener listener) {
        isLoading.setValue(true);

        firestore.collection(COURSES_COLLECTION)
                .document(courseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    listener.onSuccess(courseId);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to delete course: " + e.getMessage());
                    isLoading.setValue(false);
                    listener.onFailure(e.getMessage());
                });
    }

    // Get course by ID
    public void getCourseById(String courseId, OnCourseListener listener) {
        isLoading.setValue(true);

        firestore.collection(COURSES_COLLECTION)
                .document(courseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLoading.setValue(false);
                    if (documentSnapshot.exists()) {
                        Course course = documentSnapshot.toObject(Course.class);
                        if (course != null) {
                            course.setCourseId(documentSnapshot.getId());
                            listener.onCourseLoaded(course);
                        } else {
                            listener.onFailure("Failed to parse course data");
                        }
                    } else {
                        listener.onFailure("Course not found");
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to load course: " + e.getMessage());
                    isLoading.setValue(false);
                    listener.onFailure(e.getMessage());
                });
    }

    // Enroll student in course
    public void enrollStudent(String courseId, String studentId, OnCompleteListener listener) {
        isLoading.setValue(true);

        // Get references to the course and student documents
        DocumentReference courseRef = firestore.collection(COURSES_COLLECTION).document(courseId);
        DocumentReference studentRef = firestore.collection(USERS_COLLECTION).document(studentId);

        // Non-transaction approach for better reliability
        // First, get the course and check if student is already enrolled
        courseRef.get()
                .addOnSuccessListener(courseSnapshot -> {
                    if (courseSnapshot.exists()) {
                        Course course = courseSnapshot.toObject(Course.class);
                        if (course != null) {
                            // Make sure the enrolledStudentIds list exists
                            List<String> enrolledStudents = course.getEnrolledStudentIds();
                            if (enrolledStudents == null) {
                                enrolledStudents = new ArrayList<>();
                            }

                            // Check if student is already enrolled
                            if (enrolledStudents.contains(studentId)) {
                                isLoading.setValue(false);
                                listener.onSuccess(courseId); // Already enrolled, consider it a success
                                return;
                            }

                            // Add student to course
                            enrolledStudents.add(studentId);
                            courseRef.update("enrolledStudentIds", enrolledStudents)
                                    .addOnSuccessListener(aVoid -> {
                                        // Now update the student's enrolled courses
                                        studentRef.get()
                                                .addOnSuccessListener(studentSnapshot -> {
                                                    if (studentSnapshot.exists()) {
                                                        // Get current enrolled courses
                                                        List<String> enrolledCourses = (List<String>) studentSnapshot.get("enrolledCourseIds");
                                                        if (enrolledCourses == null) {
                                                            enrolledCourses = new ArrayList<>();
                                                        }

                                                        // Add course if not already enrolled
                                                        if (!enrolledCourses.contains(courseId)) {
                                                            enrolledCourses.add(courseId);
                                                            studentRef.update("enrolledCourseIds", enrolledCourses)
                                                                    .addOnSuccessListener(aVoid2 -> {
                                                                        isLoading.setValue(false);
                                                                        listener.onSuccess(courseId);
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        isLoading.setValue(false);
                                                                        listener.onFailure("Failed to update student record: " + e.getMessage());
                                                                    });
                                                        } else {
                                                            isLoading.setValue(false);
                                                            listener.onSuccess(courseId);
                                                        }
                                                    } else {
                                                        isLoading.setValue(false);
                                                        listener.onFailure("Student not found");
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    isLoading.setValue(false);
                                                    listener.onFailure("Failed to get student data: " + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        isLoading.setValue(false);
                                        listener.onFailure("Failed to update course: " + e.getMessage());
                                    });
                        } else {
                            isLoading.setValue(false);
                            listener.onFailure("Course data is invalid");
                        }
                    } else {
                        isLoading.setValue(false);
                        listener.onFailure("Course not found");
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    listener.onFailure("Failed to get course data: " + e.getMessage());
                });
    }

    // Remove student from course
    public void unenrollStudent(String courseId, String studentId, OnCompleteListener listener) {
        isLoading.setValue(true);

        // Get references to the course and student documents
        DocumentReference courseRef = firestore.collection(COURSES_COLLECTION).document(courseId);
        DocumentReference studentRef = firestore.collection(USERS_COLLECTION).document(studentId);

        // Non-transaction approach for better reliability
        // First, get the course and check if student is enrolled
        courseRef.get()
                .addOnSuccessListener(courseSnapshot -> {
                    if (courseSnapshot.exists()) {
                        Course course = courseSnapshot.toObject(Course.class);
                        if (course != null) {
                            // Make sure the enrolledStudentIds list exists
                            List<String> enrolledStudents = course.getEnrolledStudentIds();
                            if (enrolledStudents == null || !enrolledStudents.contains(studentId)) {
                                isLoading.setValue(false);
                                listener.onSuccess(courseId); // Not enrolled, consider it a success
                                return;
                            }

                            // Remove student from course
                            enrolledStudents.remove(studentId);
                            courseRef.update("enrolledStudentIds", enrolledStudents)
                                    .addOnSuccessListener(aVoid -> {
                                        // Now update the student's enrolled courses
                                        studentRef.get()
                                                .addOnSuccessListener(studentSnapshot -> {
                                                    if (studentSnapshot.exists()) {
                                                        // Get current enrolled courses
                                                        List<String> enrolledCourses = (List<String>) studentSnapshot.get("enrolledCourseIds");
                                                        if (enrolledCourses != null && enrolledCourses.contains(courseId)) {
                                                            enrolledCourses.remove(courseId);
                                                            studentRef.update("enrolledCourseIds", enrolledCourses)
                                                                    .addOnSuccessListener(aVoid2 -> {
                                                                        isLoading.setValue(false);
                                                                        listener.onSuccess(courseId);
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        isLoading.setValue(false);
                                                                        listener.onFailure("Failed to update student record: " + e.getMessage());
                                                                    });
                                                        } else {
                                                            isLoading.setValue(false);
                                                            listener.onSuccess(courseId);
                                                        }
                                                    } else {
                                                        isLoading.setValue(false);
                                                        listener.onFailure("Student not found");
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    isLoading.setValue(false);
                                                    listener.onFailure("Failed to get student data: " + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        isLoading.setValue(false);
                                        listener.onFailure("Failed to update course: " + e.getMessage());
                                    });
                        } else {
                            isLoading.setValue(false);
                            listener.onFailure("Course data is invalid");
                        }
                    } else {
                        isLoading.setValue(false);
                        listener.onFailure("Course not found");
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    listener.onFailure("Failed to get course data: " + e.getMessage());
                });
    }

    // Get enrolled students for a course
    public void getEnrolledStudents(String courseId, OnStudentsListener listener) {
        isLoading.setValue(true);

        getCourseById(courseId, new OnCourseListener() {
            @Override
            public void onCourseLoaded(Course course) {
                List<String> studentIds = course.getEnrolledStudentIds();
                if (studentIds == null || studentIds.isEmpty()) {
                    isLoading.setValue(false);
                    listener.onStudentsLoaded(new ArrayList<>());
                    return;
                }

                List<Map<String, Object>> students = new ArrayList<>();
                final int[] completedQueries = {0};

                for (String studentId : studentIds) {
                    firestore.collection(USERS_COLLECTION).document(studentId)
                            .get()
                            .addOnSuccessListener(document -> {
                                completedQueries[0]++;

                                if (document.exists()) {
                                    Map<String, Object> studentData = document.getData();
                                    if (studentData != null) {
                                        studentData.put("userId", document.getId());
                                        students.add(studentData);
                                    }
                                }

                                // Check if all queries are completed
                                if (completedQueries[0] >= studentIds.size()) {
                                    isLoading.setValue(false);
                                    listener.onStudentsLoaded(students);
                                }
                            })
                            .addOnFailureListener(e -> {
                                completedQueries[0]++;

                                // Check if all queries are completed
                                if (completedQueries[0] >= studentIds.size()) {
                                    isLoading.setValue(false);
                                    listener.onStudentsLoaded(students);
                                }
                            });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                isLoading.setValue(false);
                listener.onFailure(errorMessage);
            }
        });
    }

    // Check if a student is enrolled in a course
    public void isStudentEnrolled(String courseId, String studentId, OnEnrollmentCheckListener listener) {
        isLoading.setValue(true);

        getCourseById(courseId, new OnCourseListener() {
            @Override
            public void onCourseLoaded(Course course) {
                List<String> enrolledStudents = course.getEnrolledStudentIds();
                boolean isEnrolled = enrolledStudents != null && enrolledStudents.contains(studentId);
                isLoading.setValue(false);
                listener.onResult(isEnrolled);
            }

            @Override
            public void onFailure(String errorMessage) {
                isLoading.setValue(false);
                listener.onResult(false);
            }
        });
    }

    // Helper function to convert Course object to Map for Firestore
    private Map<String, Object> courseToMap(Course course) {
        Map<String, Object> courseMap = new HashMap<>();
        courseMap.put("courseCode", course.getCourseCode());
        courseMap.put("courseName", course.getCourseName());
        courseMap.put("description", course.getDescription());
        courseMap.put("department", course.getDepartment());
        courseMap.put("semester", course.getSemester());
        courseMap.put("credits", course.getCredits());
        courseMap.put("startDate", course.getStartDate());
        courseMap.put("endDate", course.getEndDate());
        courseMap.put("instructorId", course.getInstructorId());
        courseMap.put("enrolledStudentIds", course.getEnrolledStudentIds());
        courseMap.put("sessionIds", course.getSessionIds());
        courseMap.put("attendanceThreshold", course.getAttendanceThreshold());
        courseMap.put("isActive", course.isActive());
        return courseMap;
    }

    // Callback interfaces
    public interface OnCompleteListener {
        void onSuccess(String id);
        void onFailure(String errorMessage);
    }

    public interface OnCourseListener {
        void onCourseLoaded(Course course);
        void onFailure(String errorMessage);
    }

    public interface OnStudentsListener {
        void onStudentsLoaded(List<Map<String, Object>> students);
        void onFailure(String errorMessage);
    }

    public interface OnEnrollmentCheckListener {
        void onResult(boolean isEnrolled);
    }
}