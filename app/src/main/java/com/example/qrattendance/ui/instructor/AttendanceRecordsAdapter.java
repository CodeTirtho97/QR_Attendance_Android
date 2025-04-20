package com.example.qrattendance.ui.instructor;

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

public class AttendanceRecordsAdapter extends RecyclerView.Adapter<AttendanceRecordsAdapter.AttendanceViewHolder> {

    private List<SessionAttendanceActivity.AttendanceModel> attendanceRecords;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());

    public AttendanceRecordsAdapter(List<SessionAttendanceActivity.AttendanceModel> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public void updateAttendanceRecords(List<SessionAttendanceActivity.AttendanceModel> newRecords) {
        this.attendanceRecords = newRecords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_record, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        SessionAttendanceActivity.AttendanceModel model = attendanceRecords.get(position);
        holder.bind(model, position + 1);
    }

    @Override
    public int getItemCount() {
        return attendanceRecords.size();
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSerialNumber;
        private final TextView tvStudentName;
        private final TextView tvStudentRoll;
        private final TextView tvAttendanceTime;
        private final TextView tvAttendanceStatus;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSerialNumber = itemView.findViewById(R.id.tvSerialNumber);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentRoll = itemView.findViewById(R.id.tvStudentRoll);
            tvAttendanceTime = itemView.findViewById(R.id.tvAttendanceTime);
            tvAttendanceStatus = itemView.findViewById(R.id.tvAttendanceStatus);
        }

        public void bind(SessionAttendanceActivity.AttendanceModel model, int serialNumber) {
            AttendanceRecord record = model.getRecord();

            // Set serial number
            tvSerialNumber.setText(String.valueOf(serialNumber));

            // Set student info
            tvStudentName.setText(model.getStudentName());
            tvStudentRoll.setText(model.getStudentRoll());

            // Set attendance time
            if (record.getTimestamp() != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
                tvAttendanceTime.setText(timeFormat.format(record.getTimestamp()));
            } else {
                tvAttendanceTime.setText("Unknown");
            }

            // Set attendance status
            String statusText = "Present";
            int textColor = R.color.accent_green;

            if (record.getStatus() != null) {
                switch (record.getStatus()) {
                    case PRESENT:
                        statusText = "Present";
                        textColor = R.color.accent_green;
                        break;
                    case LATE:
                        statusText = "Late";
                        textColor = R.color.accent_orange;
                        break;
                    case ABSENT:
                        statusText = "Absent";
                        textColor = R.color.accent_red;
                        break;
                    case EXCUSED:
                        statusText = "Excused";
                        textColor = R.color.grey_600;
                        break;
                }
            }

            tvAttendanceStatus.setText(statusText);
            tvAttendanceStatus.setTextColor(itemView.getContext().getResources().getColor(textColor));
        }
    }
}