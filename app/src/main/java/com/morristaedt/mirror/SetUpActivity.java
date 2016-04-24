package com.morristaedt.mirror;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.morristaedt.mirror.configuration.ConfigurationSettings;

import io.flic.lib.FlicAppNotInstalledException;
import io.flic.lib.FlicBroadcastReceiverFlags;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;

public class SetUpActivity extends AppCompatActivity {

    private static final long HOUR_MILLIS = 60 * 60 * 1000;
    private static final int METERS_MIN = 500;

    private ConfigurationSettings mConfigSettings;

    private LocationManager mLocationManager;

    @Nullable
    private LocationListener mLocationListener;

    @Nullable
    private Location mLocation;

    private RadioGroup mTemperatureChoice;
    private CheckBox mBikingCheckbox;
    private CheckBox mMoodDetectionCheckbox;
    private CheckBox mShowNextCaledarEventCheckbox;
    private CheckBox mXKCDCheckbox;
    private CheckBox mXKCDInvertCheckbox;
    private View mLocationView;
    private EditText mLatitude;
    private EditText mLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        FlicConfig.setFlicCredentials();
        grabButton();
        Log.d("Flic Logged", "True");

        mConfigSettings = new ConfigurationSettings(this);

        mTemperatureChoice = (RadioGroup) findViewById(R.id.temperature_group);
        mTemperatureChoice.check(mConfigSettings.getIsCelsius() ? R.id.celsius : R.id.farenheit);

        mBikingCheckbox = (CheckBox) findViewById(R.id.biking_checkbox);
        mBikingCheckbox.setChecked(mConfigSettings.showBikingHint());

        mMoodDetectionCheckbox = (CheckBox) findViewById(R.id.mood_detection_checkbox);
        mMoodDetectionCheckbox.setChecked(mConfigSettings.showMoodDetection());

        mShowNextCaledarEventCheckbox = (CheckBox) findViewById(R.id.calendar_checkbox);
        mShowNextCaledarEventCheckbox.setChecked(mConfigSettings.showNextCalendarEvent());

        mXKCDCheckbox = (CheckBox) findViewById(R.id.xkcd_checkbox);
        mXKCDCheckbox.setChecked(mConfigSettings.showXKCD());

        mXKCDInvertCheckbox = (CheckBox) findViewById(R.id.xkcd_invert_checkbox);
        mXKCDInvertCheckbox.setChecked(mConfigSettings.invertXKCD());

        mLatitude = (EditText) findViewById(R.id.latitude);
        mLongitude = (EditText) findViewById(R.id.longitude);

        mLatitude.setText(String.valueOf(mConfigSettings.getLatitude()));
        mLongitude.setText(String.valueOf(mConfigSettings.getLongitude()));

        mLocationView = findViewById(R.id.location_view);
        setUpLocationMonitoring();

        findViewById(R.id.launch_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFields();

                Intent intent = new Intent(SetUpActivity.this, MirrorActivity.class);
                startActivity(intent);
            }
        });
    }

    //View v
    public void grabButton() {
        Log.d("Flic Grab", "True");
        try {
            FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
                @Override
                public void onInitialized(FlicManager manager) {
                    manager.initiateGrabButton(SetUpActivity.this);
                }
            });
        } catch (FlicAppNotInstalledException err) {
            Toast.makeText(this, "Flic App is not installed", Toast.LENGTH_SHORT).show();
        }
        Log.d("Flic Grabbed", "True");
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d("OnActivity", "True");
        FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
            @Override
            public void onInitialized(FlicManager manager) {
                FlicButton button = manager.completeGrabButton(requestCode, resultCode, data);
                if (button != null) {
                    button.registerListenForBroadcast(FlicBroadcastReceiverFlags.UP_OR_DOWN | FlicBroadcastReceiverFlags.REMOVED);
                    Toast.makeText(SetUpActivity.this, "Grabbed a button", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SetUpActivity.this, "Did not grab any button", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    private void setUpLocationMonitoring() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        String provider = mLocationManager.getBestProvider(criteria, true);

        try {
            mLocation = mLocationManager.getLastKnownLocation(provider);

            if (mLocation == null) {
                mLocationView.setVisibility(View.VISIBLE);
                mLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (location != null) {
                            Toast.makeText(SetUpActivity.this, R.string.found_location, Toast.LENGTH_SHORT).show();
                            mLocation = location;
                            mConfigSettings.setLatLon(String.valueOf(mLocation.getLatitude()), String.valueOf(mLocation.getLongitude()));
                            mLocationManager.removeUpdates(this);
                            if (mLocationView != null) {
                                mLocationView.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                };
                mLocationManager.requestLocationUpdates(provider, HOUR_MILLIS, METERS_MIN, mLocationListener);
            } else {
                mLocationView.setVisibility(View.GONE);
            }
        } catch (IllegalArgumentException e) {
            Log.e("SetUpActivity", "Location manager could not use provider", e);
        }
    }

    private void saveFields() {
        mConfigSettings.setIsCelsius(mTemperatureChoice.getCheckedRadioButtonId() == R.id.celsius);
        mConfigSettings.setShowBikingHint(mBikingCheckbox.isChecked());
        mConfigSettings.setShowMoodDetection(mMoodDetectionCheckbox.isChecked());
        mConfigSettings.setShowNextCalendarEvent(mShowNextCaledarEventCheckbox.isChecked());
        mConfigSettings.setXKCDPreference(mXKCDCheckbox.isChecked(), mXKCDInvertCheckbox.isChecked());

        if (mLocation == null) {
            mConfigSettings.setLatLon(mLatitude.getText().toString(), mLongitude.getText().toString());
        } else {
            mConfigSettings.setLatLon(String.valueOf(mLocation.getLatitude()), String.valueOf(mLocation.getLongitude()));
        }

    }
}
