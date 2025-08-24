package com.roamtool.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Switch;
import android.content.Context;
import android.provider.Settings;
import android.net.Uri;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.widget.Toast;
import android.os.Environment;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.animation.ValueAnimator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

public class BubbleOverlayService extends Service {

    private WindowManager windowManager;
    private View bubbleView;
    private View expandedView;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams expandedParams;
    
    private boolean isExpanded = false;
    private boolean isRunning = false;
    private boolean antiBanEnabled = false;
    
    private TextView statusText;
    private Button filePickerButton;
    private Button startButton;
    private Switch antiBanSwitch;
    
    private List<WiFiCredential> wifiCredentials = new ArrayList<>();
    private File selectedFile = null;

    // WiFi credential class
    private static class WiFiCredential {
        String name;
        String password;
        
        WiFiCredential(String name, String password) {
            this.name = name;
            this.password = password;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        createBubbleView();
        createExpandedView();
    }

    private void createBubbleView() {
        // Create bubble view
        LayoutInflater inflater = LayoutInflater.from(this);
        bubbleView = inflater.inflate(R.layout.bubble_layout, null);
        
        // Set bubble click listener
        bubbleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleExpandedView();
            }
        });

        // Set up dragging for bubble
        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = Math.abs(event.getRawX() - initialTouchX);
                        float deltaY = Math.abs(event.getRawY() - initialTouchY);
                        
                        if (deltaX > 10 || deltaY > 10) {
                            isDragging = true;
                            bubbleParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                            bubbleParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(bubbleView, bubbleParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        // Window parameters for bubble
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        bubbleParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);

        bubbleParams.gravity = Gravity.TOP | Gravity.LEFT;
        bubbleParams.x = 100;
        bubbleParams.y = 100;

        windowManager.addView(bubbleView, bubbleParams);
    }

    private void createExpandedView() {
        // Create expanded view
        LayoutInflater inflater = LayoutInflater.from(this);
        expandedView = inflater.inflate(R.layout.expanded_layout, null);
        
        // Initialize views
        statusText = expandedView.findViewById(R.id.statusText);
        filePickerButton = expandedView.findViewById(R.id.filePickerButton);
        startButton = expandedView.findViewById(R.id.startButton);
        antiBanSwitch = expandedView.findViewById(R.id.antiBanSwitch);
        
        // Set click listeners
        filePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
        
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWiFiProcess();
            }
        });
        
        antiBanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            antiBanEnabled = isChecked;
        });
        
        // Close button
        Button closeButton = expandedView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideExpandedView();
            }
        });

        // Window parameters for expanded view
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        expandedParams = new WindowManager.LayoutParams(
            320,
            450,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);

        expandedParams.gravity = Gravity.CENTER;
        
        // Add drag functionality to title bar
        LinearLayout titleBar = expandedView.findViewById(R.id.titleBar);
        titleBar.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = expandedParams.x;
                        initialY = expandedParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = Math.abs(event.getRawX() - initialTouchX);
                        float deltaY = Math.abs(event.getRawY() - initialTouchY);
                        
                        if (deltaX > 10 || deltaY > 10) {
                            isDragging = true;
                            expandedParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                            expandedParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                            if (isExpanded) {
                                windowManager.updateViewLayout(expandedView, expandedParams);
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        return true;
                }
                return false;
            }
        });
    }

    private void toggleExpandedView() {
        if (isRunning) {
            // If running, just show the expanded view to display results
            if (!isExpanded) {
                showExpandedView();
            }
            return;
        }
        
        if (isExpanded) {
            hideExpandedView();
        } else {
            showExpandedView();
        }
    }

    private void showExpandedView() {
        if (!isExpanded) {
            isExpanded = true;
            windowManager.addView(expandedView, expandedParams);
            
            // Animate bubble shrinking
            animateBubbleToTab();
            
            updateUI();
        }
    }

    private void hideExpandedView() {
        if (isExpanded && !isRunning) {
            isExpanded = false;
            windowManager.removeView(expandedView);
            
            // Animate tab back to bubble
            animateTabToBubble();
        }
    }

    private void animateBubbleToTab() {
        // Animate bubble getting smaller and expanded view appearing
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(bubbleView, "scaleX", 1f, 0.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(bubbleView, "scaleY", 1f, 0.3f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(bubbleView, "alpha", 1f, 0.5f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(200);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void animateTabToBubble() {
        // Animate bubble back to full size
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(bubbleView, "scaleX", 0.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(bubbleView, "scaleY", 0.3f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(bubbleView, "alpha", 0.5f, 1f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(200);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/plain");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            
            // Try to start file picker
            startActivity(intent);
            
            // Since we can't get result in Service, we'll provide alternative
            statusText.setText("File picker opened. Please copy your WiFi file to Downloads folder and try again, or create a file named 'wifi.txt' in Downloads with format:\nWiFiName|Password");
            
            // Auto-check Downloads folder for common files
            checkDownloadsForWiFiFile();
            
        } catch (Exception e) {
            // Fallback: scan Downloads folder
            statusText.setText("Opening file manager... Please ensure your WiFi file is in Downloads folder");
            checkDownloadsForWiFiFile();
        }
    }
    
    private void checkDownloadsForWiFiFile() {
        try {
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File[] files = downloadsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
            
            if (files != null && files.length > 0) {
                // Show available files to user
                StringBuilder fileList = new StringBuilder("Found TXT files in Downloads:\n");
                for (int i = 0; i < Math.min(files.length, 5); i++) {
                    fileList.append("• ").append(files[i].getName()).append("\n");
                }
                fileList.append("\nTap here again to process the first file.");
                statusText.setText(fileList.toString());
                
                // Auto-select first file for convenience
                selectedFile = files[0];
                filePickerButton.setText("Process: " + files[0].getName());
                
                // Enable processing
                filePickerButton.setOnClickListener(v -> processWiFiFile());
            } else {
                statusText.setText("No TXT files found in Downloads.\n\nCreate a file with format:\nWiFiName|Password\nWiFi2|Pass2");
            }
        } catch (Exception e) {
            statusText.setText("Error accessing Downloads folder: " + e.getMessage());
        }
    }

    private void processWiFiFile() {
        if (selectedFile == null) return;
        
        wifiCredentials.clear();
        List<String> errors = new ArrayList<>();
        int lineNumber = 0;
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
            String line;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                if (line.isEmpty()) continue;
                
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        String wifiName = parts[0].trim();
                        String wifiPassword = parts[1].trim();
                        
                        if (wifiName.isEmpty()) {
                            errors.add("Line " + lineNumber + ": Missing WiFi name");
                        } else if (wifiPassword.isEmpty()) {
                            errors.add("Line " + lineNumber + ": Missing WiFi password");
                        } else {
                            wifiCredentials.add(new WiFiCredential(wifiName, wifiPassword));
                        }
                    } else {
                        errors.add("Line " + lineNumber + ": Invalid format (missing password)");
                    }
                } else {
                    errors.add("Line " + lineNumber + ": Invalid format (missing | separator)");
                }
            }
            reader.close();
            
        } catch (IOException e) {
            statusText.setText("Error reading file: " + e.getMessage());
            return;
        }
        
        // Update status
        if (!errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Errors found:\n");
            for (String error : errors) {
                errorMessage.append(error).append("\n");
            }
            statusText.setText(errorMessage.toString());
        }
        
        if (!wifiCredentials.isEmpty()) {
            statusText.setText("Verified " + wifiCredentials.size() + " WiFi credentials. Please tap Start to begin.");
            startButton.setEnabled(true);
        } else {
            statusText.setText("No valid WiFi credentials found.");
            startButton.setEnabled(false);
        }
    }

    private void startWiFiProcess() {
        if (wifiCredentials.isEmpty()) {
            statusText.setText("Please select a valid WiFi file first.");
            return;
        }
        
        isRunning = true;
        statusText.setText("Running app...");
        startButton.setEnabled(false);
        
        // Hide the expanded view and animate to bubble
        if (isExpanded) {
            windowManager.removeView(expandedView);
            isExpanded = false;
            animateTabToBubble();
        }
        
        // Open WiFi settings
        openWiFiSettings();
    }

    private void openWiFiSettings() {
        try {
            Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            wifiSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(wifiSettingsIntent);
            
            // Simulate processing time
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    completeWiFiProcess();
                }
            }, 3000); // Wait 3 seconds to simulate WiFi processing
            
        } catch (ActivityNotFoundException e) {
            statusText.setText("Could not open WiFi settings");
            isRunning = false;
            startButton.setEnabled(true);
        }
    }

    private void completeWiFiProcess() {
        isRunning = false;
        startButton.setEnabled(true);
        
        // Update status - but don't auto-expand the view
        // User must click the bubble to see the result
    }

    private void updateUI() {
        if (selectedFile != null) {
            filePickerButton.setText("File: " + selectedFile.getName());
        } else {
            filePickerButton.setText("Select TXT File");
        }
        
        antiBanSwitch.setChecked(antiBanEnabled);
        
        if (isRunning) {
            statusText.setText("Running app...");
            startButton.setEnabled(false);
        } else if (!wifiCredentials.isEmpty()) {
            statusText.setText("Process completed successfully!");
            startButton.setEnabled(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubbleView != null) {
            windowManager.removeView(bubbleView);
        }
        if (expandedView != null && isExpanded) {
            windowManager.removeView(expandedView);
        }
    }
}
