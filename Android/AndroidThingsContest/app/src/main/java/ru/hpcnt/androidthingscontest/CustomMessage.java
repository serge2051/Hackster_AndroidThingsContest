package ru.hpcnt.androidthingscontest;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Sergey Pakharev on 27.10.2017.
 */

public class CustomMessage {

    public static MainActivity parentActivity;
    private String TAG = CustomMessage.class.getSimpleName();

    public String mSource;
    public String mType;
    public String mDevice;
    public String mValue;

    public CustomMessage(String inJSON)
    {

        try {
            JSONObject mainObject = new JSONObject(inJSON);

            this.mSource = mainObject.getString("src");
            this.mType = mainObject.getString("type");
            this.mDevice = mainObject.getString("device");
            this.mValue = mainObject.getString("value");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Log.e(TAG, "CustomMessage received from " + this.mSource);
    }

    public void NotifyDevice()
    {
        if (this.parentActivity!=null)
        {
            //Log.d(TAG, "NotifyDevice!");
            parentActivity.SendToEdge(this.mDevice, this.mType, this.mValue);
        }
    }

    public CustomMessage(String inSource, String inType, String inDevice, String inValue)
    {
        this.mSource = inSource;
        this.mType = inType;
        this.mDevice = inDevice;
        this.mValue = inValue;
    }

    public String GetStringFromJSONObject()
    {
        JSONObject jArrayOutputData = new JSONObject();
        String retString = "";
        try {
            jArrayOutputData.put("src", mSource);
            jArrayOutputData.put("type", mType);
            jArrayOutputData.put("device", mDevice);
            jArrayOutputData.put("value", mValue);
            retString = jArrayOutputData.toString(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return retString;
    }

    public JSONObject GetJSONObject()
    {
        JSONObject jArrayOutputData = new JSONObject();
        String retString = "";
        try {
            jArrayOutputData.put("src", mSource);
            jArrayOutputData.put("type", mType);
            jArrayOutputData.put("device", mDevice);
            jArrayOutputData.put("value", mValue);
            retString = jArrayOutputData.toString(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jArrayOutputData;
    }
}
