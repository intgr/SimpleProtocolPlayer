/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2014 kaytat
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
 *
 * Code for NoFilter related from here:
 * http://stackoverflow.com/questions/8512762/autocompletetextview-disable-filtering
 */

package com.kaytat.simpleprotocolplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity: shows media player buttons. This activity shows the media player buttons and
 * lets the user click them. No media handling is done here -- everything is done by passing
 * Intents to our {@link MusicService}.
 * */
public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "MainActivity";

    AutoCompleteTextView mIPAddrText;
    ArrayList<String> mIPAddrList;
    ArrayAdapter<String> mIPAddrAdapter;

    AutoCompleteTextView mAudioPortText;
    ArrayList<String> mAudioPortList;
    ArrayAdapter<String> mAudioPortAdapter;

    int mSampleRate;
    boolean mStereo;
    int mBufferMs;

    Button mPlayButton;
    Button mStopButton;

    ComponentName serviceComponent = new ComponentName("com.kaytat.simpleprotocolplayer",
            "com.kaytat.simpleprotocolplayer.MusicService");

    /**
     * Called when the activity is first created. Here, we simply set the event listeners and
     * start the background service ({@link MusicService}) that will handle the actual media
     * playback.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mIPAddrText = (AutoCompleteTextView) findViewById(R.id.editTextIpAddr);
        mAudioPortText = (AutoCompleteTextView) findViewById(R.id.editTextAudioPort);
        mStopButton = (Button) findViewById(R.id.stopbutton);
        mPlayButton = (Button) findViewById(R.id.playbutton);
        mStopButton = (Button) findViewById(R.id.stopbutton);

        mPlayButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);

        // Allow full list to be shown on first focus
        mIPAddrText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mIPAddrText.showDropDown();
                return false;
            }
        });
        mIPAddrText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && mIPAddrText.getAdapter() != null)
                    mIPAddrText.showDropDown();

            }
        });
        mAudioPortText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mAudioPortText.showDropDown();
                return false;
            }
        });
        mAudioPortText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && mIPAddrText.getAdapter() != null)
                    mAudioPortText.showDropDown();

            }
        });
    }

    /*
        The two different approaches here is an attempt to support both an old preferences
        and new preferences.  The newer version saved to JSON while the old version just saved
        one string.
     */
    static final String IP_PREF = "IP_PREF";
    static final String PORT_PREF = "PORT_PREF";

    static final String IP_JSON_PREF = "IP_JSON_PREF";
    static final String PORT_JSON_PREF = "PORT_JSON_PREF";

    static final String RATE_PREF = "RATE";
    static final String STEREO_PREF = "STEREO";
    static final String BUFFER_MS_PREF = "BUFFER_MS";

    ArrayList<String> getListFromPrefs(
            SharedPreferences prefs,
            String keyJson,
            String keySingle)
    {
        // Retrieve the values from the shared preferences
        String jsonString = prefs.getString(keyJson, null);
        ArrayList<String> arrayList = new ArrayList<String>();

        if (jsonString == null || jsonString.isEmpty())
        {
            // Try to fill with the original key used
            String single = prefs.getString(keySingle, null);
            if (single != null && !single.isEmpty())
            {
                arrayList.add(single);
            }
        }
        else
        {
            try
            {
                JSONObject jsonObject = new JSONObject(jsonString);

                // Note that the array is hard-coded as the element labelled as 'list'
                JSONArray jsonArray = jsonObject.getJSONArray("list");
                if (jsonArray != null)
                {
                    for (int i = 0; i < jsonArray.length(); i++)
                    {
                        String s = (String)jsonArray.get(i);
                        if (s != null && !s.isEmpty())
                        {
                            arrayList.add((String)s);
                        }
                    }
                }
            }
            catch (JSONException jsonException)
            {
                Log.i(TAG, jsonException.toString());
            }
        }

        return arrayList;
    }

    private ArrayList<String>  getUpdatedArrayList(
            SharedPreferences prefs,
            AutoCompleteTextView view,
            String keyJson,
            String keySingle) {
        // Retrieve the values from the shared preferences
        ArrayList<String> arrayList = getListFromPrefs(
                prefs,
                keyJson,
                keySingle);

        // Make sure the most recent IP is on top
        arrayList.remove(view.getText().toString());
        arrayList.add(0, view.getText().toString());

        if (arrayList.size() >= 4)
        {
            arrayList.subList(4, arrayList.size()).clear();
        }

        return arrayList;
    }

    private JSONObject getJson(ArrayList<String> arrayList)
    {
        JSONArray jsonArray = new JSONArray(arrayList);
        JSONObject jsonObject = new JSONObject();
        try
        {
            jsonObject.put("list", jsonArray);
        }
        catch (JSONException jsonException)
        {
            Log.i(TAG, jsonException.toString());
        }

        return jsonObject;
    }

    private void savePrefs()
    {
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();

        mIPAddrList = getUpdatedArrayList(myPrefs, mIPAddrText, IP_JSON_PREF, IP_PREF);
        mAudioPortList = getUpdatedArrayList(myPrefs, mAudioPortText, PORT_JSON_PREF, PORT_PREF);

        // Write out JSON object
        prefsEditor.putString(IP_JSON_PREF, getJson(mIPAddrList).toString());
        prefsEditor.putString(PORT_JSON_PREF, getJson(mAudioPortList).toString());

        prefsEditor.putBoolean(STEREO_PREF, mStereo);
        prefsEditor.putInt(RATE_PREF, mSampleRate);
        prefsEditor.putInt(BUFFER_MS_PREF, mBufferMs);
        prefsEditor.apply();

        // Update adapters
        mIPAddrAdapter.clear();
        mIPAddrAdapter.addAll(mIPAddrList);
        mIPAddrAdapter.notifyDataSetChanged();
        mAudioPortAdapter.clear();
        mAudioPortAdapter.addAll(mAudioPortList);
        mAudioPortAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        savePrefs();
    }

    private class NoFilterArrayAdapter<T>
            extends ArrayAdapter<T>
    {
        private Filter filter = new NoFilter();
        public List<T> items;

        @Override
        public Filter getFilter() {
            return filter;
        }

        public NoFilterArrayAdapter(Context context, int textViewResourceId,
                             List<T> objects) {
            super(context, textViewResourceId, objects);
            Log.v(TAG, "Adapter created " + filter);
            items = objects;
        }

        private class NoFilter extends Filter {

            @Override
            protected android.widget.Filter.FilterResults performFiltering(CharSequence arg0) {
                android.widget.Filter.FilterResults result = new android.widget.Filter.FilterResults();
                result.values = items;
                result.count = items.size();
                return result;
            }

            @Override
            protected void publishResults(CharSequence arg0, android.widget.Filter.FilterResults arg1) {
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);

        mIPAddrList = getListFromPrefs(myPrefs, IP_JSON_PREF, IP_PREF);
        mIPAddrAdapter = new NoFilterArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mIPAddrList);
        mIPAddrText.setAdapter(mIPAddrAdapter);
        mIPAddrText.setThreshold(1);
        if (mIPAddrList.size() != 0)
        {
            mIPAddrText.setText((String)mIPAddrList.get(0));
        }

        mAudioPortList = getListFromPrefs(myPrefs, PORT_JSON_PREF, PORT_PREF);
        mAudioPortAdapter = new NoFilterArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mAudioPortList);
        mAudioPortText.setAdapter(mAudioPortAdapter);
        mAudioPortText.setThreshold(1);
        if (mAudioPortList.size() != 0)
        {
            mAudioPortText.setText((String)mAudioPortList.get(0));
        }

        // These hard-coded values should match the defaults in the strings array
        Resources res = getResources();
        /* FIXME broken on Android TV?
        mSampleRate = myPrefs.getInt(RATE_PREF, MusicService.DEFAULT_SAMPLE_RATE);
        String rateString = Integer.toString(mSampleRate);
        String[] sampleRateStrings = res.getStringArray(R.array.sampleRates);
        for (int i = 0; i < sampleRateStrings.length; i++) {
            if (sampleRateStrings[i].contains(rateString)) {
                Spinner sampleRateSpinner = (Spinner) findViewById(R.id.spinnerSampleRate);
                sampleRateSpinner.setSelection(i);
                break;
            }
        }
        */
        /* FIXME broken on Android TV?
        mStereo = myPrefs.getBoolean(STEREO_PREF, MusicService.DEFAULT_STEREO);
        String[] stereoStrings = res.getStringArray(R.array.stereo);
        Spinner stereoSpinner = (Spinner) findViewById(R.id.stereo);
        String stereoKey = getResources().getString(R.string.stereoKey);
        if (stereoStrings[0].contains(stereoKey) == mStereo) {
            stereoSpinner.setSelection(0);
        } else {
            stereoSpinner.setSelection(1);
        }
        */
        mBufferMs = myPrefs.getInt(BUFFER_MS_PREF, 50);
        Log.d(TAG, "mBufferMs:" + mBufferMs);
        EditText e = (EditText)findViewById(R.id.editTextBufferSize);
        //e.setText(Integer.toString(mBufferMs)); // FIXME broken on Android TV?
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.notice_item:
            Intent intent = new Intent(this, NoticeActivity.class);
            startActivity(intent);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void onClick(View target) {
        // Send the correct intent to the MusicService, according to the button that was clicked
        if (target == mPlayButton) {
            // Get the IP address and port and put it in the intent
            Intent i = new Intent(MusicService.ACTION_PLAY);
            i.setComponent(serviceComponent);

            String ipAddr = mIPAddrText.getText().toString();
            String portStr = mAudioPortText.getText().toString();
            if (ipAddr == null || ipAddr.equals("")) {
                Toast.makeText(getApplicationContext(), "Invalid address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (portStr == null || portStr.equals("")) {
                Toast.makeText(getApplicationContext(), "Invalid port", Toast.LENGTH_SHORT).show();
                return;
            }
            i.putExtra(MusicService.DATA_IP_ADDRESS, ipAddr);

            int audioPort;
            try {
                audioPort = Integer.parseInt(portStr);
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Invalid port:" + nfe);
                Toast.makeText(getApplicationContext(), "Invalid port", Toast.LENGTH_SHORT).show();
                return;
            }
            hideKb();
            i.putExtra(MusicService.DATA_AUDIO_PORT, audioPort);

            // Extract sample rate
            Spinner sampleRateSpinner = (Spinner) findViewById(R.id.spinnerSampleRate);
            try {
                String rateStr = String.valueOf(sampleRateSpinner.getSelectedItem());
                String[] rateSplit = rateStr.split(" ");
                if (rateSplit.length != 0) {
                    // mSampleRate = Integer.parseInt(rateSplit[0]); // FIXME broken on Android TV?
                    mSampleRate = 48000;
                    Log.i(TAG, "rate:" + mSampleRate);
                    i.putExtra(MusicService.DATA_SAMPLE_RATE, mSampleRate);
                }
            }
            catch (Exception e) {
                // Ignore parsing errors.  The intent will have a default
            }

            // Extract stereo/mono setting
            Spinner stereoSpinner = (Spinner) findViewById(R.id.stereo);
            try {
                String stereoSettingString = String.valueOf(stereoSpinner.getSelectedItem());
                String[] stereoSplit = stereoSettingString.split(" ");
                String stereoKey = getResources().getString(R.string.stereoKey);
                if (stereoSplit.length != 0) {
                    mStereo = stereoSplit[0].contains(stereoKey);
                    Log.i(TAG, "stereo:" + mStereo);
                    i.putExtra(MusicService.DATA_STEREO, mStereo);
                }
            }
            catch (Exception e) {
                // Ignore parsing errors.  The intent will have a default
            }

            // Get the latest buffer entry
            /* FIXME broken on Android TV?
            EditText e = (EditText)findViewById(R.id.editTextBufferSize);
            String bufferMsString = e.getText().toString();
            if (bufferMsString == null || bufferMsString.isEmpty()) {
                mBufferMs = 0;
            } else {
                mBufferMs = Integer.parseInt(bufferMsString);
            }
            */
            mBufferMs = 500;
            i.putExtra(MusicService.DATA_BUFFER_MS, mBufferMs);

            // Save current settings
            savePrefs();
            startService(i);
        }
        else if (target == mStopButton) {
            Intent i = new Intent(MusicService.ACTION_STOP);
            i.setComponent(serviceComponent);

            hideKb();
            startService(i);

            //boolean ok = stopService(i);
            //Log.i(TAG, "stopService: " + Boolean.valueOf(ok).toString());
        }
    }

    private void hideKb() {
        InputMethodManager inputManager =
                (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = getCurrentFocus();
        if (v != null) {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
