package ru.hpcnt.androidthingscontest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static com.google.android.gms.internal.zzahg.runOnUiThread;


public class Fragment_tab_camera extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private Handler mCameraHandler;
    private Handler mCloudHandler;
    private HandlerThread mCameraThread;
    private HandlerThread mCloudThread;
    private FirebaseDatabase mDatabase;
    private RaspCamera mCamera;
    public ImageView imageFrame;
    public TextView textView_timestamp;
    public TextView textView_metadata;
    public TextView textView_fire_1;

    private OnFragmentInteractionListener mListener;

    public Fragment_tab_camera() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static Fragment_tab_camera newInstance(String param1, String param2) {
        Fragment_tab_camera fragment = new Fragment_tab_camera();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
	
    // Callback to receive captured camera image data
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //Log.d(TAG, "onImageAvailable!!");
					
                    // Get the raw image bytes
                    Image image = reader.acquireLatestImage();

                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    if (bitmap != null) {
                        Fragment_tab_camera.this.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                imageFrame.setImageBitmap(bitmap);
                            }
                        });
                    }

                    onPictureTaken(imageBytes);
                }
            };

    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {

            final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            final byte[] byteArray_new = stream.toByteArray();

            mCloudHandler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, "sending image to cloud vision");
                    // annotate image by uploading to Cloud Vision API
                    try {
                        Map<String, Float> annotations = CloudVisionUtils.annotateImage(byteArray_new);
                        //Log.d(TAG, "cloud vision annotations:" + annotations);
                        if (annotations != null) {

                            final DatabaseReference log = mDatabase.getReference("mchs").push();
                            log.child("timestamp").setValue(ServerValue.TIMESTAMP);

                            if (textView_fire_1 != null)
                                if (annotations.containsKey("flame"))
                                {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            Fragment_tab_camera.this.textView_fire_1.setText("FIRE!!!");
                                        }
                                    });

                                }
                                else
                                {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            Fragment_tab_camera.this.textView_fire_1.setText("");
                                        }
                                    });
                                }
                            for (Map.Entry<String, Float> entry : annotations.entrySet())
                            {
                                log.child(entry.getKey()).setValue(entry.getValue().toString());
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Cloud Vison API error: ", e);
                    }
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fragment_tab_camera, container, false);
    }

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {

        super.onViewCreated(view, savedInstanceState);


        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCloudThread = new HandlerThread("CloudThread");
        mCloudThread.start();
        mCloudHandler = new Handler(mCloudThread.getLooper());

        mDatabase = FirebaseDatabase.getInstance();


        mCamera = RaspCamera.getInstance();

        if (ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Camera permissions not granted!");

            int permissionCamera = ContextCompat.checkSelfPermission(getActivity(),
                    android.Manifest.permission.CAMERA);
            List<String> listPermissionsNeeded = new ArrayList<>();
            if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(getActivity(), listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
                return;
            }
            return;
        }
        mCamera.initializeCamera(this.getContext(), mCameraHandler, mOnImageAvailableListener);

        this.textView_fire_1 = (TextView) view.findViewById(R.id.textView_fire);
        this.imageFrame = (ImageView) view.findViewById(R.id.imageView1);
        this.textView_metadata = (TextView) view.findViewById(R.id.textView_label1);
        this.textView_metadata = (TextView) view.findViewById(R.id.textView_label2);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_picImage);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ButtonPicture();
                Snackbar.make(view, "Image captured", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mCameraThread.quitSafely();
        mCloudThread.quitSafely();
        mCamera.shutDown();
    }

    public void ButtonPicture() {
        mCamera.takePicture();
    }
	
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
