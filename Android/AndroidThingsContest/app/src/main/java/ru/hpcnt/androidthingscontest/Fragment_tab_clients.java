package ru.hpcnt.androidthingscontest;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static com.google.android.gms.internal.zzahg.runOnUiThread;

public class Fragment_tab_clients extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private TextView textView_client1_to;
    private TextView textView_client1_from;


    public Fragment_tab_clients() {
        // Required empty public constructor
    }

    public static Fragment_tab_clients newInstance(String param1, String param2) {
        Fragment_tab_clients fragment = new Fragment_tab_clients();
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
        return inflater.inflate(R.layout.fragment_fragment_tab_clients, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button tmpButton = (Button) view.findViewById(R.id.button_connectEdge1);
        tmpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Fragment_tab_clients.this.getContext(), "Connecting to device..", Toast.LENGTH_SHORT).show();
                CustomMessage.parentActivity.ConfigureI2C(4);
            }
        });
        textView_client1_to = (TextView) view.findViewById(R.id.TextView_client1_To);
        textView_client1_from = (TextView) view.findViewById(R.id.TextView_client1_From);
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

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void UpdateTextView(boolean isFirstClient, boolean isFromDirection, final String inText)
    {
        if (isFirstClient)
        {
            if (isFromDirection)
            {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Fragment_tab_clients.this.textView_client1_from.append(inText + "\r\n");
                    }
                });

            }
            else
            {
                runOnUiThread(new Runnable() {
					public void run() {
						Fragment_tab_clients.this.textView_client1_to.append(inText + "\r\n");
					}
                });
            }
        }
    }
}
