/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import com.example.background.workers.BlurWorker;
import com.example.background.workers.CleanupWorker;
import com.example.background.workers.SaveImageToFileWorker;

import java.util.List;
import java.util.ListIterator;

import static com.example.background.Constants.BLUR_OUTPUT;
import static com.example.background.Constants.IMAGE_MANIPULATION_WORK_NAME;
import static com.example.background.Constants.KEY_IMAGE_URI;
import static com.example.background.Constants.TAG_OUTPUT;

public class BlurViewModel extends AndroidViewModel {

    private Uri mImageUri;

    private WorkManager mWorkManager;

    private Uri mOutputUri;

    //new instance variable for the WorkInfo
    private LiveData<List<WorkInfo>> mSavedWorkInfo;

    private LiveData<List<WorkInfo>> mBlurWorkInfo;

    public BlurViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);
        mSavedWorkInfo = mWorkManager.getWorkInfosByTagLiveData(TAG_OUTPUT);
        mBlurWorkInfo = mWorkManager.getWorkInfosByTagLiveData(BLUR_OUTPUT);
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    void applyBlur(int blurLevel) {
//        mWorkManager.enqueue(OneTimeWorkRequest.from(BlurWorker.class));
        //Add WorkRequest to Cleanup temp images
        WorkContinuation continuation = mWorkManager.beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                //use REPLACE because if the user decides to blur another image before the current one
                //is finished, the app will need to stop the current one and start blurring the new image
                //so that the app will now only ever blur one picture at a time
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker.class)
        );

        //Create charging constraint
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();

        //Add WorkRequests to blur the image the number of times requested
        for (int i=0 ; i < blurLevel ; i++) {
            OneTimeWorkRequest.Builder blurBuilder = new OneTimeWorkRequest.Builder(BlurWorker.class)
                    .addTag(BLUR_OUTPUT);

            //Input the Uri if this is the first blur operation
            //After the first blur operation the input will be the output of previous
            //blur operations
            if ( i == 0 ) {
                blurBuilder.setInputData(createInputDataForUri());
            }

            continuation = continuation.then(blurBuilder.build());
        }

        Data.Builder builder = new Data.Builder();
        builder.putString("clientApi", "request_fdata");
        //Add WorkRequest to save the image to the filesystem
        OneTimeWorkRequest saveImageRequest = new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
                .setConstraints(constraints)//This adds the constraint which requires the users to charge the devices while saving image
                .setInputData(builder.build())//This adds the input data to worker class;
                .addTag(TAG_OUTPUT) //This adds the tag
                .build();

        continuation = continuation.then(saveImageRequest);

        //Actually start the work
        continuation.enqueue();
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Setters
     */
    void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    /**
     * Getters
     */
    Uri getImageUri() {
        return mImageUri;
    }

    //Add a getter method for mSavedWorkInfo
    LiveData<List<WorkInfo>> getSavedWorkInfo() {
        return mSavedWorkInfo;
    }

    //Add a getter method for mBlurWorkInfo
    LiveData<List<WorkInfo>> getBlurWorkInfo() {
        return mBlurWorkInfo;
    }

    /**
     * Setter method for outputUri
     */
    void setOutputUri(String outputImageUri) {
        mOutputUri = uriOrNull(outputImageUri);
    }

    /**
     * Getter method for getOutputUri
     */
    Uri getOutputUri() {
        return mOutputUri;
    }

    /**
     * Cancel work using the work's unique name
     * to cancel all the work instead of just a particular step
     * by unique chain name
     */
    void cancelWork() {
        mWorkManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME);
    }

    /**
     * Create the input data bundle which includes the Uri to operate on
     * @return Data which contains the image uri as a string
     */
    private Data createInputDataForUri() {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null )
            builder.putString(KEY_IMAGE_URI, mImageUri.toString());

        return builder.build();
    }



}