package com.example.background.workers;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.background.Constants;
import com.example.background.R;

public class BlurWorker extends Worker {
    public BlurWorker(
                @NonNull Context appContext,
                @NonNull WorkerParameters workerParams
            ) {
        super(appContext, workerParams);
    }

    private static final String TAG = BlurWorker.class.getSimpleName();

    @NonNull
    @Override
    public Result doWork() {
        Context applicationContext = getApplicationContext();
        String resourceUri = getInputData().getString(Constants.KEY_IMAGE_URI);

        try {

            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid input uri");
                throw new IllegalArgumentException("Invalid input uri");
            }

            ContentResolver resolver = applicationContext.getContentResolver();

            //create the bitmap with user input uri
            Bitmap picture = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(resourceUri)));

            //Blur the bitmap
            Bitmap output = WorkerUtils.blurBitmap(picture, applicationContext);

            //Write bitmap to temp file
            Uri outputUri = WorkerUtils.writeBitmapToFile(applicationContext, output);

            Data outputData = new Data.Builder()
                    .putString(Constants.KEY_IMAGE_URI, outputUri.toString())
                    .build();

            //make notification on notification bar
            WorkerUtils.makeStatusNotification("Output is " + outputUri.toString(), applicationContext);

            //if it is error free, return success
            return Result.success(outputData);
        } catch (Throwable throwable) {
            Log.e(TAG, "Error applying blur", throwable);
            return Result.failure();
        }
    }
}
