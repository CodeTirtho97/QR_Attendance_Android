package com.example.qrattendance.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qrattendance.R;

/**
 * Helper class for UI related utilities such as showing error messages,
 * custom dialogs, and toast messages with better styling.
 */
public class UIHelper {

    /**
     * Shows an error dialog with a title and message
     * @param context The context
     * @param title The dialog title
     * @param message The error message
     */
    public static void showErrorDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Shows an information dialog with a title and message
     * @param context The context
     * @param title The dialog title
     * @param message The information message
     */
    public static void showInfoDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * Shows a confirmation dialog with a title and message
     * @param context The context
     * @param title The dialog title
     * @param message The confirmation message
     * @param positiveButtonText The text for the positive button
     * @param onConfirm Callback to execute when confirmed
     */
    public static void showConfirmationDialog(Context context, String title, String message,
                                              String positiveButtonText, Runnable onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Shows a custom error toast
     * @param context The context
     * @param message The error message
     */
    public static void showErrorToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a custom success toast
     * @param context The context
     * @param message The success message
     */
    public static void showSuccessToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}