package ru.hpcnt.androidthingscontest;

import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Sergey Pakharev on 27.10.2017.
 */

public class CloudVisionUtils {

    public static final String TAG = CloudVisionUtils.class.getSimpleName();

	private static final String PROJECT_NAME = "<YOUR_PROJECT_NAME>";
	
    private static final String CLOUD_VISION_API_KEY = "<YOUR_API_KEY>";

    private static final String LABEL_DETECTION = "LABEL_DETECTION";

    private static final int MAX_LABEL_RESULTS = 10;

    public static Map<String, Float> annotateImage(byte[] imageBytes) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        VisionRequestInitializer initializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);
        Vision vision = new Vision.Builder(httpTransport, jsonFactory, null)
                .setVisionRequestInitializer(initializer)
                .setApplicationName(PROJECT_NAME)
                .build();

        AnnotateImageRequest imageRequest = new AnnotateImageRequest();
        Image img = new Image();
        img.encodeContent(imageBytes);
        imageRequest.setImage(img);

        // Add the features we want
        Feature labelDetection = new Feature();
        labelDetection.setType(LABEL_DETECTION);
        labelDetection.setMaxResults(MAX_LABEL_RESULTS);
        imageRequest.setFeatures(Collections.singletonList(labelDetection));

        // Batch and execute the request
        BatchAnnotateImagesRequest requestBatch = new BatchAnnotateImagesRequest();
        requestBatch.setRequests(Collections.singletonList(imageRequest));

        Vision.Images im1 = vision.images();
        Vision.Images.Annotate an1 = im1.annotate(requestBatch);
        an1.setDisableGZipContent(false);
        BatchAnnotateImagesResponse response = an1.execute();

        return convertResponseToMap(response);
    }

    private static Map<String, Float> convertResponseToMap(BatchAnnotateImagesResponse response) {

        // Convert response into a readable collection of annotations
        Map<String, Float> annotations = new HashMap<>();
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                annotations.put(label.getDescription(), label.getScore());
            }
        }

        Log.d(TAG, "Cloud Vision request completed:" + annotations);
        return annotations;
    }

}
