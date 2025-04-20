package com.example.qrattendance.ui.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.AttendanceRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyAttendanceAdapter extends RecyclerView.Adapter<MyAttendanceAdapter.AttendanceViewHolder> {

    private List<AttendanceRecord> attendanceRecords;
    private final Map<String, String> courseNames;
    private final Map<String, String> sessionTitles;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public MyAttendanceAdapter(List<AttendanceRecord> attendanceRecords,
                               Map<String, String> courseNames,
                               Map<String, String> sessionTitles) {
        this.attendanceRecords = attendanceRecords;
        this.courseNames = courseNames;
        this.sessionTitles = sessionTitles;
    }

    public void updateAttendanceRecords(List<AttendanceRecord> newRecords) {
        this.attendanceRecords = newRecords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        AttendanceRecord record = attendanceRecords.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return attendanceRecords.size();
    }

    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCourseName;
        private final TextView tvSessionTitle;
        private final TextView tvDate;
        private final TextView tvTime;
        private final TextView tvStatus;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvSessionTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        public void bind(AttendanceRecord record) {
            // Set course name
            String courseId = record.getCourseId();
            String courseName = courseNames.getOrDefault(courseId, "Unknown Course");
            tvCourseName.setText(courseName);

            // Set session title
            String sessionId = record.getSessionId();
            String sessionTitle = sessionTitles.getOrDefault(sessionId, "Unknown Session");
            tvSessionTitle.setText(sessionTitle);

            // Set date and time
            if (record.getTimestamp() != null) {
                tvDate.setText(dateFormat.format(record.getTimestamp()));
                tvTime.setText(timeFormat.format(record.getTimestamp()));
            } else {
                tvDate.setText("Unknown Date");
                tvTime.setText("Unknown Time");
            }

            // Set status
            String statusText = "Present";
            int statusColor = R.color.accent_green;

            if (record.getStatus() != null) {
                switch (record.getStatus()) {
                    case PRESENT:
                        statusText = "Present";
                        statusColor = R.color.accent_green;
                        break;
                    case LATE:
                        statusText = "Late";
                        statusColor = R.color.accent_orange;
                        break;
                    case ABSENT:
                        statusText = "Absent";
                        statusColor = R.color.accent_red;
                        break;
                    case EXCUSED:
                        statusText = "Excused";
                        statusColor = R.color.grey_600;
                        break;
                }
            }

            tvStatus.setText(statusText);
            tvStatus.setTextColor(itemView.getContext().getResources().getColor(statusColor));
        }
    }
}