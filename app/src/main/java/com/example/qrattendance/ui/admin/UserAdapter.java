package com.example.qrattendance.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;

import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<Map<String, Object>> users;
    private final UserActionListener listener;

    public interface UserActionListener {
        void onEditUser(Map<String, Object> user);
        void onToggleUserActiveStatus(Map<String, Object> user);
        void onViewUserDetails(Map<String, Object> user);
    }

    public UserAdapter(List<Map<String, Object>> users, UserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void updateUsers(List<Map<String, Object>> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Map<String, Object> user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvUserType;
        private final TextView tvUserStatus;
        private final ImageButton btnActivateDeactivate;
        private final ImageButton btnEdit;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserType = itemView.findViewById(R.id.tvUserType);
            tvUserStatus = itemView.findViewById(R.id.tvUserStatus);
            btnActivateDeactivate = itemView.findViewById(R.id.btnToggleActive);
            btnEdit = itemView.findViewById(R.id.btnEditUser);
        }

        public void bind(Map<String, Object> user) {
            // Set user basic info
            String name = (String) user.get("name");
            String email = (String) user.get("email");
            String role = (String) user.get("role");

            tvName.setText(name);
            tvEmail.setText(email);

            // Set user type text and badge color
            String userType;
            int badgeColorResId;

            switch (role) {
                case "STUDENT":
                    userType = "Student";
                    badgeColorResId = R.color.teal_700;
                    break;
                case "INSTRUCTOR":
                    userType = "Instructor";
                    badgeColorResId = R.color.purple_700;
                    break;
                case "ADMIN":
                    String privilegeLevel = (String) user.get("privilegeLevel");
                    if (privilegeLevel != null) {
                        if (privilegeLevel.equals("SUPER_ADMIN")) {
                            userType = "Super Admin";
                        } else if (privilegeLevel.equals("DEPARTMENT_ADMIN")) {
                            userType = "Dept. Admin";
                        } else {
                            userType = "Course Admin";
                        }
                    } else {
                        userType = "Admin";
                    }
                    badgeColorResId = R.color.accent_orange;
                    break;
                default:
                    userType = "Unknown";
                    badgeColorResId = R.color.grey_600;
            }

            tvUserType.setText(userType);
            tvUserType.setBackgroundTintList(itemView.getContext().getColorStateList(badgeColorResId));

            // Set user active status
            boolean isActive = user.containsKey("isActive") ? (boolean) user.get("isActive") : true;
            if (isActive) {
                tvUserStatus.setText("Active");
                tvUserStatus.setTextColor(itemView.getContext().getColor(R.color.accent_green));
                btnActivateDeactivate.setImageResource(R.drawable.ic_visibility);
            } else {
                tvUserStatus.setText("Inactive");
                tvUserStatus.setTextColor(itemView.getContext().getColor(R.color.accent_red));
                btnActivateDeactivate.setImageResource(R.drawable.ic_visibility_off);
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewUserDetails(user);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditUser(user);
                }
            });

            btnActivateDeactivate.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleUserActiveStatus(user);
                }
            });
        }
    }
}