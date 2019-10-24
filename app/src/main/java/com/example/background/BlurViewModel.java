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
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import com.example.background.workers.BlurWorker;
import com.example.background.workers.CleanupWorker;
import com.example.background.workers.SaveImageToFileWorker;

import static com.example.background.Constants.IMAGE_MANIPULATION_WORK_NAME;
import static com.example.background.Constants.KEY_IMAGE_URI;

public class BlurViewModel extends AndroidViewModel {

    private Uri mImageUri;

    private WorkManager mWorkManager;

    public BlurViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);
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

        //Add WorkRequests to blur the image the number of times requested
        for (int i=0 ; i < blurLevel ; i++) {
            OneTimeWorkRequest.Builder blurBuilder = new OneTimeWorkRequest.Builder(BlurWorker.class);

            //Input the Uri if this is the first blur operation
            //After the first blur operation the input will be the output of previous
            //blur operations
            if ( i == 0 ) {
                blurBuilder.setInputData(createInputDataForUri());
            }

            continuation = continuation.then(blurBuilder.build());

        }
        //Add WorkRequest to save the image to the filesystem
        OneTimeWorkRequest saveImageRequest = new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
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