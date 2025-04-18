package com.example.qrattendance.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.qrattendance.data.model.Course;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseRepository {
    private static final String TAG = "CourseRepository";
    private static final String COURSES_COLLECTION = "courses";

    private static CourseRepository instance;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<List<Course>> coursesLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

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

    // Get all courses
    public LiveData<List<Course>> getCourses() {
        return coursesLiveData;
    }

    // Get error message
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Get loading state
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
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

        DocumentReference courseRef = firestore.collection(COURSES_COLLECTION).document(courseId);

        firestore.runTransaction(transaction -> {
            Course course = transaction.get(courseRef).toObject(Course.class);
            if (course == null) {
                try {
                    throw new Exception("Course not found");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (course.getEnrolledStudentIds() == null) {
                course.setEnrolledStudentIds(new ArrayList<>());
            }

            if (!course.getEnrolledStudentIds().contains(studentId)) {
                course.getEnrolledStudentIds().add(studentId);

                // Update the course with the new student list
                transaction.update(courseRef, "enrolledStudentIds", course.getEnrolledStudentIds());
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            isLoading.setValue(false);
            listener.onSuccess(courseId);
        }).addOnFailureListener(e -> {
            errorMessage.setValue("Failed to enroll student: " + e.getMessage());
            isLoading.setValue(false);
            listener.onFailure(e.getMessage());
        });
    }

    // Remove student from course
    public void unenrollStudent(String courseId, String studentId, OnCompleteListener listener) {
        isLoading.setValue(true);

        DocumentReference courseRef = firestore.collection(COURSES_COLLECTION).document(courseId);

        firestore.runTransaction(transaction -> {
            Course course = transaction.get(courseRef).toObject(Course.class);
            if (course == null) {
                try {
                    throw new Exception("Course not found");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (course.getEnrolledStudentIds() != null && course.getEnrolledStudentIds().contains(studentId)) {
                course.getEnrolledStudentIds().remove(studentId);

                // Update the course with the new student list
                transaction.update(courseRef, "enrolledStudentIds", course.getEnrolledStudentIds());
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            isLoading.setValue(false);
            listener.onSuccess(courseId);
        }).addOnFailureListener(e -> {
            errorMessage.setValue("Failed to unenroll student: " + e.getMessage());
            isLoading.setValue(false);
            listener.onFailure(e.getMessage());
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
}