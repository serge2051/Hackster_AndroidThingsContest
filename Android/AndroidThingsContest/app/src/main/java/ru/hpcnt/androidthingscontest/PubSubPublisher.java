package ru.hpcnt.androidthingscontest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.PubsubScopes;
import com.google.api.services.pubsub.model.AcknowledgeRequest;
import com.google.api.services.pubsub.model.PublishRequest;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.api.services.pubsub.model.PullRequest;
import com.google.api.services.pubsub.model.PullResponse;
import com.google.api.services.pubsub.model.ReceivedMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Sergey Pakharev on 27.10.2017.
 */

public class PubSubPublisher {

    private static final String TAG = PubSubPublisher.class.getSimpleName();

    private final Context mContext;
    private final String mProjname;
    private final String mTopic;
    private final String mSubscription;
    private final String mDevice = "rpi2";

    private Pubsub mPubsub;
    private HttpTransport mHttpTransport;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private List<CustomMessage> mMessagesQueue;

    private static final long PUSHING_SMALL_INTERVAL_MS = TimeUnit.MICROSECONDS.toMillis(100);
    private static final long PULLING_BIG_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long PULLING_SMALL_INTERVAL_MS = TimeUnit.MICROSECONDS.toMillis(300);

    PubSubPublisher(Context context, String project, String topic,
                    int credentialResourceId) throws IOException {
        mContext = context;
        mProjname = project;
        mTopic = "projects/" + mProjname + "/topics/" + topic;
        mSubscription = "projects/" + mProjname + "/subscriptions/" + mDevice + "_sub";

        mMessagesQueue = new ArrayList<>();
        mHandlerThread = new HandlerThread("pubsubPublisherThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        InputStream jsonCredentials = mContext.getResources().openRawResource(credentialResourceId);
        final GoogleCredential credentials;
        try {
            credentials = GoogleCredential.fromStream(jsonCredentials).createScoped(
                    Collections.singleton(PubsubScopes.PUBSUB));
        } finally {
            try {
                jsonCredentials.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream", e);
            }
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mHttpTransport = AndroidHttp.newCompatibleTransport();
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                mPubsub = new Pubsub.Builder(mHttpTransport, jsonFactory, credentials)
                        .setApplicationName(mProjname).build();
            }
        });
    }

    public void start() {
        mHandler.post(mPullingRunnable);
    }

    public void sendData(CustomMessage tmpObject){
        mMessagesQueue.add(tmpObject);
        mHandler.post(mPublishRunnable);
    }

    public void stop() {
        mHandler.removeCallbacks(mPublishRunnable);
        mHandler.removeCallbacks(mPullingRunnable);
    }

    public void close() {
        mHandler.removeCallbacks(mPublishRunnable);
        mHandler.removeCallbacks(mPullingRunnable);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mHttpTransport.shutdown();
                } catch (IOException e) {
                    Log.d(TAG, "error destroying http transport");
                } finally {
                    mHttpTransport = null;
                    mPubsub = null;
                }
            }
        });
        mHandlerThread.quitSafely();
    }

    private Runnable mPublishRunnable = new Runnable() {
        @Override
        public void run() {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
                Log.e(TAG, "no active network");
                return;
            }
            if (mMessagesQueue.size()==0)
            {
                return;
            }
            try {

                CustomMessage tmpMessage = mMessagesQueue.get(0);
                tmpMessage.mSource = mDevice;
                JSONObject messagePayload = tmpMessage.GetJSONObject();

                Log.d(TAG, "publishing message: " + messagePayload);

                PubsubMessage m = new PubsubMessage();

                m.setData(Base64.encodeToString(messagePayload.toString().getBytes(),
                        Base64.NO_WRAP));
                PublishRequest request = new PublishRequest();
                request.setMessages(Collections.singletonList(m));
                mPubsub.projects().topics().publish(mTopic, request).execute();

            } catch (IOException e) {
                Log.e(TAG, "Error publishing message", e);
            } finally {
                mMessagesQueue.remove(0);
                if (mMessagesQueue.size()>0)
                    mHandler.postDelayed(mPublishRunnable, PUSHING_SMALL_INTERVAL_MS);
            }
        }

        private JSONObject createMessagePayload(float temperature, float pressure)
                throws JSONException {

            CustomMessage newMsg = new CustomMessage(mDevice, "status", "mchs", "okey");
            return newMsg.GetJSONObject();
        }
    };

    private Runnable mPullingRunnable = new Runnable() {
        @Override
        public void run() {

            //Log.d(TAG, "mPullingRunnable!");

            ConnectivityManager connectivityManager =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
                Log.e(TAG, "no active network");
                return;
            }

            PullRequest pullRequest = new PullRequest()
                    .setReturnImmediately(true)
                    .setMaxMessages(10);

            PullResponse pullResponse = null;

            try {
                pullResponse = mPubsub.projects().subscriptions()
                        .pull(mSubscription, pullRequest).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (pullResponse == null)
            {
                mHandler.postDelayed(mPullingRunnable, PULLING_BIG_INTERVAL_MS);
                return;
            }
            List<String> ackIds = new ArrayList<>(10);
            List<ReceivedMessage> receivedMessages =
                    pullResponse.getReceivedMessages();
            if (receivedMessages != null) {
                for (ReceivedMessage receivedMessage : receivedMessages) {
                    PubsubMessage pubsubMessage =
                            receivedMessage.getMessage();
                    if (pubsubMessage != null
                            && pubsubMessage.decodeData() != null) {
                        try {
                            String resString = new String(pubsubMessage.decodeData(),"UTF-8");
                            System.out.println(resString);

                            CustomMessage newMsg = new CustomMessage(resString);


                            if (!newMsg.mSource.equals(mDevice))
                                newMsg.NotifyDevice();

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    ackIds.add(receivedMessage.getAckId());
                }
                AcknowledgeRequest ackRequest = new AcknowledgeRequest();
                ackRequest.setAckIds(ackIds);
                try {
                    mPubsub.projects().subscriptions()
                            .acknowledge(mSubscription, ackRequest)
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    mHandler.postDelayed(mPullingRunnable, PULLING_SMALL_INTERVAL_MS);
                }
            }
            else
                mHandler.postDelayed(mPullingRunnable, PULLING_BIG_INTERVAL_MS);
        }
    };
}
