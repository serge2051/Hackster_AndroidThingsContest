package ru.hpcnt.androidthingscontest;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class Fragment_tab_pins extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private Map<String, Gpio> mGpioMap = new LinkedHashMap<>();

    public Fragment_tab_pins() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static Fragment_tab_pins newInstance(String param1, String param2) {
        Fragment_tab_pins fragment = new Fragment_tab_pins();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fragment_tab_pins, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout gpioPinsView = (LinearLayout) getView().findViewById(R.id.gpio_pins);
        LayoutInflater inflater = getLayoutInflater(savedInstanceState);
        PeripheralManagerService pioService = new PeripheralManagerService();
        for (String name : pioService.getGpioList()) {

            View child = inflater.inflate(R.layout.list_item_gpio, gpioPinsView, false);
            Switch button = (Switch) child.findViewById(R.id.gpio_switch);
            button.setText(name);
            gpioPinsView.addView(button);
            Log.d(TAG, "Added button for GPIO: " + name);


            try {
                if (!mGpioMap.containsKey(name)) {
                    final Gpio ledPin = pioService.openGpio(name);
                    ledPin.setEdgeTriggerType(Gpio.EDGE_NONE);
                    ledPin.setActiveType(Gpio.ACTIVE_HIGH);
                    ledPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

                    button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            try {
                                ledPin.setValue(isChecked);
                            } catch (IOException e) {
                                Log.e(TAG, "error toggling gpio:", e);
                                buttonView.setOnCheckedChangeListener(null);
                                buttonView.setChecked(!isChecked);
                                buttonView.setOnCheckedChangeListener(this);
                            }
                        }
                    });
                    mGpioMap.put(name, ledPin);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error initializing GPIO: " + name, e);
                button.setEnabled(false);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

        for (Map.Entry<String, Gpio> entry : mGpioMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing GPIO " + entry.getKey(), e);
            }
        }
        mGpioMap.clear();
    }
	
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
