package com.roamtool.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class FilePickerActivity extends Activity {

    private static final int FILE_PICKER_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Start file picker immediately
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == FILE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    processSelectedFile(uri);
                }
            }
            finish(); // Close this activity
        }
    }

    private void processSelectedFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            // Send result to service via broadcast
            Intent resultIntent = new Intent("com.roamtool.app.FILE_SELECTED");
            resultIntent.putExtra("file_content", content.toString());
            resultIntent.putExtra("file_name", getFileName(uri));
            sendBroadcast(resultIntent);
            
        } catch (IOException e) {
            // Send error to service
            Intent errorIntent = new Intent("com.roamtool.app.FILE_ERROR");
            errorIntent.putExtra("error_message", e.getMessage());
            sendBroadcast(errorIntent);
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "selected_file.txt";
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            // Use default name if error
        }
        return fileName;
    }
}
