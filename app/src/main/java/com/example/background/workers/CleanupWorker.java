package com.example.background.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.background.Constants;

import java.io.File;

/**
 * Worker to clean up the tmp files
 * Doesn't need to take any input or pass any output.
 * It always deletes the temporary files if the exist.
 */

public class CleanupWorker extends Worker {
    public CleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private static final String TAG = CleanupWorker.class.getSimpleName();

    @NonNull
    @Override
    public Result doWork() {
        Context applicationContext = getApplicationContext();
        // Makes a notification when the work starts and slows down the work so that it's easier to
        // see each WorkRequest start, even on emulated devices
        WorkerUtils.makeStatusNotification("Cleaning up old temporary files",
                applicationContext);
        WorkerUtils.sleep();

        try {
            File outputDirectory = new File(applicationContext.getFilesDir(), Constants.OUTPUT_PATH);
            if (outputDirectory.exists()) {
                File[] entries = outputDirectory.listFiles();
                if (entries != null && entries.length > 0 ) {
                    for (File entry : entries) {
                        String name = entry.getName();
                        if (!TextUtils.isEmpty(name) && name.endsWith(".png")) {
                            boolean deleted = entry.delete();
                            Log.i(TAG, String.format("Deleted %s - %s ", name, deleted));
                        }
                    }
                }
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up", e);
            return Result.failure();
        }
    }
}
