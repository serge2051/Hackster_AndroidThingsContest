package ru.hpcnt.androidthingscontest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity

        implements
        Fragment_tab_pins.OnFragmentInteractionListener,
        Fragment_tab_camera.OnFragmentInteractionListener,
        Fragment_tab_clients.OnFragmentInteractionListener
{

    private class EdgeDevice
    {
        public byte device_addr;
        public byte device_host_addr;
        public String deviceName;

        public EdgeDevice (String inDeviceName, byte inDeviceAddr, byte inDevice_hostAddr)
        {
            this.device_addr = inDeviceAddr;
            this.device_host_addr = inDevice_hostAddr;
            this.deviceName = inDeviceName;
        }
    }

    private FirebaseAnalytics mFirebaseAnalytics;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final Integer MAX_RESPONSE_SIZE = 1;


    /* VIEW */
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Fragment_tab_clients clientsPage;

    /* I2C devices */
    private List<I2cDevice> mConnectedDevices;
    private Map<String, Integer> mEdgeDevices;
    private List<EdgeDevice> mEdgeDevicesList;

    /* PubSub Publisher*/
    private PubSubPublisher mPubsubPublisher;

    /* Auth */
    //private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CustomMessage.parentActivity = this;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        RequestPermissions();
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        //ConfigureI2C(4);
		//ConfigureI2C(5);
        PubSubCreate();
    }

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private void RequestPermissions() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Camera permissions not granted!");

            int permissionCamera = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.CAMERA);
            List<String> listPermissionsNeeded = new ArrayList<>();
            if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
                return;
            }
            return;
        }
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Internet permissions not granted!");

            int permissionCamera = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.INTERNET);
            List<String> listPermissionsNeeded = new ArrayList<>();
            if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.INTERNET);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
                return;
            }
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        int v = 0;
        try {
            v = getPackageManager().getPackageInfo("com.google.android.gms", 0 ).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        /*
			Anonimus auth doesn't work with Android Things : cannot find required module and needed to update Google Services.
			
        Log.d(TAG, "FireBase auth: start initialize user.. " + v + " " + GOOGLE_PLAY_SERVICES_VERSION_CODE);

        mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "FireBase auth: user catched ");
        if (currentUser==null) {
            Log.d(TAG, "FireBase auth: create new user");

            mAuth.signInAnonymously()
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(Exception ex) {

                            Log.d(TAG, "FireBase auth problem: " + ex.toString());
                        }
                    })
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "FireBase auth: OK! ");
                                FirebaseUser user = mAuth.getCurrentUser();
                            } else {
                                Log.d(TAG, "FireBase auth: fail - ", task.getException());
                                Toast.makeText(getApplicationContext(), "Can't auth user =(",
                                        Toast.LENGTH_SHORT).show();
                            }

                        }
                    })
                    .addOnSuccessListener(this, new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object o) {
                            Log.d(TAG, "FireBase auth: Success! ");
                        }

                        }
                    );
        }
        else
        {
            Log.w(TAG, "FireBase auth: User already authorized!");
        }
        */
    }

    public void ConfigureI2C(int addr) {
        mEdgeDevices = new HashMap<>();
        mConnectedDevices = new ArrayList<>();
        mEdgeDevicesList = new ArrayList<>();
			
		/* Example of creating devices: device_name, device_addr, host_add(edge) */
        mEdgeDevicesList.add(new EdgeDevice("toggle-zenit-1", (byte)0, (byte)4));
        mEdgeDevicesList.add(new EdgeDevice("toggle-zenit-2", (byte)1, (byte)4));
        mEdgeDevicesList.add(new EdgeDevice("toggle-zenit-3", (byte)2, (byte)4));
        mEdgeDevicesList.add(new EdgeDevice("toggle-zenit-4", (byte)3, (byte)4));
        mEdgeDevicesList.add(new EdgeDevice("sensor_zenit_121", (byte)5, (byte)4));

        PeripheralManagerService manager = new PeripheralManagerService();
        List<String> deviceList = manager.getI2cBusList();

        if (deviceList.isEmpty()) {
            Log.i(TAG, "No I2C bus available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }
        for (int i=0;i<deviceList.size();++i)
        {
            try {
                I2cDevice mDevice = manager.openI2cDevice(deviceList.get(i), addr);
                byte[] buffer =  {0};
                i2c_writeBuffer(mDevice,buffer);

                byte[] response;

                response=i2c_readResponse(mDevice, MAX_RESPONSE_SIZE);
				/* Sometimes I2C send 0xFF as first byte so we need to receive acknoledment from host(edge) to sync states.
					First message to host-edge(Arduino) MUST be 0x00 
				*/
                while (response[0] == 0)
                {
                    Log.i(TAG, "Resending initialization of device..");
                    byte[] buffer2 =  {0};
                    i2c_writeBuffer(mDevice, buffer2);
                    response=i2c_readResponse(mDevice, MAX_RESPONSE_SIZE);
                }
                this.mConnectedDevices.add(mDevice);
				// Number of connected devices
                Log.i(TAG, "Response i2c:" + response[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private byte[] i2c_readResponse(I2cDevice mDevice, int maxResponseSize) {
        byte[] respArray = new byte[maxResponseSize];
        try {
            mDevice.read(respArray,respArray.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Response i2c:" + respArray[0]);

        return respArray;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (int i=0;i<mConnectedDevices.size();++i) {

            try {
                mConnectedDevices.get(i).close();
            } catch (IOException e) {
                Log.w(TAG, "Unable to close I2C device", e);
            }
        }
        mConnectedDevices.clear();

        if (mPubsubPublisher != null) {
            mPubsubPublisher.close();
            mPubsubPublisher = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new Fragment_tab_pins(), "PINS");
        adapter.addFragment(new Fragment_tab_camera(), "CAMERA");

        clientsPage = new Fragment_tab_clients();
        adapter.addFragment(clientsPage, "CONSOLE");
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private void i2c_writeBuffer(I2cDevice device, byte[] buffer) throws IOException {

        device.write(buffer, buffer.length);
        Log.d(TAG, "Wrote " + buffer.length + " bytes over I2C.");
    }
	
	/*
		Send some message to edge(host/Arduino). Args received from server/another Raspberry
	*/
    public void SendToEdge(String deviceName, String command, String value)
    {
        if (( mConnectedDevices==null ) || (mConnectedDevices.size()==0))
        {
            Log.d(TAG, "Dev " + mEdgeDevices + " | " + command + " | " + value);
            return;
        }
        //Log.d(TAG, "Send to edge: " + deviceName + " | " + command + " | " + value);
        boolean flag_IsSetOperation  = false;
        EdgeDevice neededDevice = null;
        byte[] buffer_command = new byte[1];
        byte[] buffer_device_addr = new byte[1];
        byte[] buffer_newVal = new byte[1];

        if (command.equals("set"))
        {
            flag_IsSetOperation = true;
            buffer_command[0] = 2;
        } 
		else if (command.equals("get"))
        {
            flag_IsSetOperation = false;
            buffer_command[0] = 1;
        }

        if (value.equals("on"))
        {
            buffer_newVal[0] = 1;
        } 
		else if (value.equals("off"))
			{
				buffer_newVal[0] = 0;
			}

        for (int i=0;i<mEdgeDevicesList.size();++i)
        {
            EdgeDevice tmpDevice = mEdgeDevicesList.get(i);

            if (tmpDevice.deviceName.equals(deviceName))
            {
                buffer_device_addr[0] = tmpDevice.device_addr;
                neededDevice = tmpDevice;
                break;
            }
        }

        for (int i=0;i<mConnectedDevices.size();++i)
        {
            I2cDevice tmpDevice = mConnectedDevices.get(i);

            if (flag_IsSetOperation)
            {
                if (clientsPage!=null)
                    clientsPage.UpdateTextView(true, false, ">" + deviceName + " | " + command + " | " + value);
                try {
                    i2c_writeBuffer(tmpDevice,buffer_command);
                    i2c_writeBuffer(tmpDevice,buffer_device_addr);
                    i2c_writeBuffer(tmpDevice,buffer_newVal);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] response = i2c_readResponse(tmpDevice, MAX_RESPONSE_SIZE);
            }
            else
            {
                try {
                    i2c_writeBuffer(tmpDevice,buffer_command);
                    i2c_writeBuffer(tmpDevice,buffer_device_addr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] response = i2c_readResponse(tmpDevice, MAX_RESPONSE_SIZE);
                CustomMessage tmpMessage = new CustomMessage("rspi","get",neededDevice.deviceName,new String(String.valueOf(response[0])));
                mPubsubPublisher.sendData(tmpMessage);
                if (clientsPage!=null)
                    clientsPage.UpdateTextView(true, true, "<" + "get" + " | " + new String(String.valueOf(buffer_command[0])) + " | " + new String(String.valueOf(buffer_device_addr[0])));
            }
        }
    }

    private void PubSubCreate()
    {
        int credentialId = getResources().getIdentifier("credentials", "raw", getPackageName());
        if (credentialId != 0) {
            try {
                mPubsubPublisher = new PubSubPublisher(this,
                        BuildConfig.PROJECT_ID, BuildConfig.PUBSUB_TOPIC, credentialId);
                mPubsubPublisher.start();
            } catch (IOException e) {
                Log.e(TAG, "error creating pubsub publisher", e);
            }
        }
    }
}
