package com.example.background.workers;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.background.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SaveImageToFileWorker extends Worker {
    public SaveImageToFileWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private static final String TAG =SaveImageToFileWorker.class.getSimpleName();

    private static final String TITLE = "Blurred Image";

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z", Locale.getDefault());

    @NonNull
    @Override
    public Result doWork() {
        Context applicationContext = getApplicationContext();
        ContentResolver resolver = applicationContext.getContentResolver();

        // Makes a notification when the work starts and slows down the work so that it's easier to
        // see each WorkRequest start, even on emulated devices
        WorkerUtils.makeStatusNotification("Saving image", applicationContext);
        WorkerUtils.sleep();

        try{
            String clientApi = getInputData().getString("clientApi");
            Log.w("clientApi", "clientApi " + clientApi);

            String resourceUri = getInputData().getString(Constants.KEY_IMAGE_URI);
            Bitmap bitmap = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(resourceUri)));
            String outputUri = MediaStore.Images.Media.insertImage(
                    resolver, bitmap, TITLE, DATE_FORMATTER.format(new Date())
            );
            if(TextUtils.isEmpty(outputUri)) {
                Log.e(TAG, "Writing to MediaStore failed");
                return Result.failure();
            }
            Data outputData = new Data.Builder()
                    .putString(Constants.KEY_IMAGE_URI, outputUri)
                    .build();

            //make notification on notification bar
            WorkerUtils.makeStatusNotification("Output is " + outputUri.toString(), applicationContext);

            return Result.success(outputData);
        } catch (Exception e) {
            Log.e(TAG, "Unable to save image to Gallery", e);
            return Result.failure();
        }
    }
}
