package com.example.qrattendance.ui.instructor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Session;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.SessionViewHolder> {

    private List<Session> sessions;
    private final SessionClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public interface SessionClickListener {
        void onSessionClick(Session session);
    }

    public SessionsAdapter(List<Session> sessions, SessionClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    public void updateSessions(List<Session> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessions.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvSessionTitle;
        private final TextView tvSessionDate;
        private final TextView tvSessionTime;
        private final TextView tvSessionLocation;
        private final TextView tvSessionStatus;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardSession);
            tvSessionTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvSessionDate = itemView.findViewById(R.id.tvSessionDate);
            tvSessionTime = itemView.findViewById(R.id.tvSessionTime);
            tvSessionLocation = itemView.findViewById(R.id.tvSessionLocation);
            tvSessionStatus = itemView.findViewById(R.id.tvSessionStatus);
        }

        public void bind(Session session) {
            tvSessionTitle.setText(session.getTitle());

            // Format date and time
            Date startTime = session.getStartTime();
            if (startTime != null) {
                tvSessionDate.setText(dateFormat.format(startTime));
                tvSessionTime.setText(timeFormat.format(startTime));
            } else {
                tvSessionDate.setText("No date");
                tvSessionTime.setText("No time");
            }

            tvSessionLocation.setText(session.getLocation());

            // Update status text and color based on session status
            session.updateStatus(); // Ensure status is up to date
            String statusText = "Unknown";
            int statusColor = R.color.grey_600;

            switch (session.getStatus()) {
                case SCHEDULED:
                    statusText = "Scheduled";
                    statusColor = R.color.purple_500;
                    break;
                case IN_PROGRESS:
                    statusText = "In Progress";
                    statusColor = R.color.accent_green;
                    break;
                case COMPLETED:
                    statusText = "Completed";
                    statusColor = R.color.grey_600;
                    break;
                case CANCELLED:
                    statusText = "Cancelled";
                    statusColor = R.color.accent_red;
                    break;
            }

            tvSessionStatus.setText(statusText);
            tvSessionStatus.setTextColor(itemView.getContext().getResources().getColor(statusColor));

            // Set click listener for the entire card
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSessionClick(session);
                }
            });
        }
    }
}