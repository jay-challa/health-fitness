package com.healthfitness.activity.pedometer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.healthfitness.R;
import com.healthfitness.activity.LoginActivity;

public class Pedometer extends Activity implements View.OnClickListener {
    private static final String TAG = "Pedometer";
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    private Utils mUtils;

    private TextView mStepValueView;
    private TextView mPaceValueView;
    private TextView mDistanceValueView;
    private TextView mSpeedValueView;
    private TextView mCaloriesValueView;
    TextView mDesiredPaceView;
    private int mStepValue;
    private int mPaceValue;
    private float mDistanceValue;
    private float mSpeedValue;
    private int mCaloriesValue;
    private float mDesiredPaceOrSpeed;
    private int mMaintain;
    private boolean mIsMetric;
    private float mMaintainInc;
    private boolean mQuitting = false; // Set when user selected Quit from menu, can be used by onPause, onStop, onDestroy

    private TextView logout;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleApiClient mGoogleApiClient;

    /**
     * True, when service is running.
     */
    private boolean mIsRunning;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "[ACTIVITY] onCreate");
        super.onCreate(savedInstanceState);

        mStepValue = 0;
        mPaceValue = 0;

        setContentView(R.layout.main);
        initGoogleClient();
        mUtils = Utils.getInstance();
    }

    private void initGoogleClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.SENSORS_API)  // Required for SensorsApi calls
                .useDefaultAccount()
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


    }

    @Override
    protected void onStart() {
        Log.i(TAG, "[ACTIVITY] onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "[ACTIVITY] onResume");
        super.onResume();

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(mSettings);

        mUtils.setSpeak(mSettings.getBoolean("speak", false));

        // Read from preferences if the service was running on the last onPause
        mIsRunning = mPedometerSettings.isServiceRunning();

        // Start the service if this is considered to be an application start (last onPause was long ago)
        if (!mIsRunning && mPedometerSettings.isNewStart()) {
            startStepService();
            bindStepService();
        } else if (mIsRunning) {
            bindStepService();
        }

        mPedometerSettings.clearServiceRunning();

        mStepValueView = (TextView) findViewById(R.id.step_value);
        mPaceValueView = (TextView) findViewById(R.id.pace_value);
        mDistanceValueView = (TextView) findViewById(R.id.distance_value);
        mSpeedValueView = (TextView) findViewById(R.id.speed_value);
        mCaloriesValueView = (TextView) findViewById(R.id.calories_value);
        mDesiredPaceView = (TextView) findViewById(R.id.desired_pace_value);
        logout = (TextView) findViewById(R.id.logout);

        logout.setOnClickListener(this);

        mIsMetric = mPedometerSettings.isMetric();
        ((TextView) findViewById(R.id.distance_units)).setText(getString(
                mIsMetric
                        ? R.string.kilometers
                        : R.string.miles
        ));
        ((TextView) findViewById(R.id.speed_units)).setText(getString(
                mIsMetric
                        ? R.string.kilometers_per_hour
                        : R.string.miles_per_hour
        ));

        mMaintain = mPedometerSettings.getMaintainOption();
        ((LinearLayout) this.findViewById(R.id.desired_pace_control)).setVisibility(
                mMaintain != PedometerSettings.M_NONE
                        ? View.VISIBLE
                        : View.GONE
        );
        if (mMaintain == PedometerSettings.M_PACE) {
            mMaintainInc = 5f;
            mDesiredPaceOrSpeed = (float) mPedometerSettings.getDesiredPace();
        } else if (mMaintain == PedometerSettings.M_SPEED) {
            mDesiredPaceOrSpeed = mPedometerSettings.getDesiredSpeed();
            mMaintainInc = 0.1f;
        }
        Button button1 = (Button) findViewById(R.id.button_desired_pace_lower);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDesiredPaceOrSpeed -= mMaintainInc;
                mDesiredPaceOrSpeed = Math.round(mDesiredPaceOrSpeed * 10) / 10f;
                displayDesiredPaceOrSpeed();
                setDesiredPaceOrSpeed(mDesiredPaceOrSpeed);
            }
        });
        Button button2 = (Button) findViewById(R.id.button_desired_pace_raise);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDesiredPaceOrSpeed += mMaintainInc;
                mDesiredPaceOrSpeed = Math.round(mDesiredPaceOrSpeed * 10) / 10f;
                displayDesiredPaceOrSpeed();
                setDesiredPaceOrSpeed(mDesiredPaceOrSpeed);
            }
        });
        if (mMaintain != PedometerSettings.M_NONE) {
            ((TextView) findViewById(R.id.desired_pace_label)).setText(
                    mMaintain == PedometerSettings.M_PACE
                            ? R.string.desired_pace
                            : R.string.desired_speed
            );
        }


        displayDesiredPaceOrSpeed();
    }

    private void displayDesiredPaceOrSpeed() {
        if (mMaintain == PedometerSettings.M_PACE) {
            mDesiredPaceView.setText("" + (int) mDesiredPaceOrSpeed);
        } else {
            mDesiredPaceView.setText("" + mDesiredPaceOrSpeed);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "[ACTIVITY] onPause");
        if (mIsRunning) {
            unbindStepService();
        }
        if (mQuitting) {
            mPedometerSettings.saveServiceRunningWithNullTimestamp(mIsRunning);
        } else {
            mPedometerSettings.saveServiceRunningWithTimestamp(mIsRunning);
        }

        super.onPause();
        savePaceSetting();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "[ACTIVITY] onStop");
        super.onStop();
    }

    protected void onDestroy() {
        Log.i(TAG, "[ACTIVITY] onDestroy");
        super.onDestroy();
    }

    protected void onRestart() {
        Log.i(TAG, "[ACTIVITY] onRestart");
        super.onDestroy();
    }

    private void setDesiredPaceOrSpeed(float desiredPaceOrSpeed) {
        if (mService != null) {
            if (mMaintain == PedometerSettings.M_PACE) {
                mService.setDesiredPace((int) desiredPaceOrSpeed);
            } else if (mMaintain == PedometerSettings.M_SPEED) {
                mService.setDesiredSpeed(desiredPaceOrSpeed);
            }
        }
    }

    private void savePaceSetting() {
        mPedometerSettings.savePaceOrSpeedSetting(mMaintain, mDesiredPaceOrSpeed);
    }

    private StepService mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((StepService.StepBinder) service).getService();

            mService.registerCallback(mCallback);
            mService.reloadSettings();

        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };


    private void startStepService() {
        if (!mIsRunning) {
            Log.i(TAG, "[SERVICE] Start");
            mIsRunning = true;
            startService(new Intent(Pedometer.this,
                    StepService.class));
        }
    }

    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(Pedometer.this,
                StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindStepService() {
        Log.i(TAG, "[SERVICE] Unbind");
        unbindService(mConnection);
    }

    private void stopStepService() {
        Log.i(TAG, "[SERVICE] Stop");
        if (mService != null) {
            Log.i(TAG, "[SERVICE] stopService");
            stopService(new Intent(Pedometer.this,
                    StepService.class));
        }
        mIsRunning = false;
    }

    private void resetValues(boolean updateDisplay) {
        if (mService != null && mIsRunning) {
            mService.resetValues();
        } else {
            mStepValueView.setText("0");
            mPaceValueView.setText("0");
            mDistanceValueView.setText("0");
            mSpeedValueView.setText("0");
            mCaloriesValueView.setText("0");
            SharedPreferences state = getSharedPreferences("state", 0);
            SharedPreferences.Editor stateEditor = state.edit();
            if (updateDisplay) {
                stateEditor.putInt("steps", 0);
                stateEditor.putInt("pace", 0);
                stateEditor.putFloat("distance", 0);
                stateEditor.putFloat("speed", 0);
                stateEditor.putFloat("calories", 0);
                stateEditor.commit();
            }
        }
    }

    private static final int MENU_SETTINGS = 8;
    private static final int MENU_QUIT = 9;

    private static final int MENU_PAUSE = 1;
    private static final int MENU_RESUME = 2;
    private static final int MENU_RESET = 3;

    /* Creates the menu items */
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mIsRunning) {
            menu.add(0, MENU_PAUSE, 0, R.string.pause)
                    .setIcon(android.R.drawable.ic_media_pause)
                    .setShortcut('1', 'p');
        } else {
            menu.add(0, MENU_RESUME, 0, R.string.resume)
                    .setIcon(android.R.drawable.ic_media_play)
                    .setShortcut('1', 'p');
        }
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setShortcut('2', 'r');
        menu.add(0, MENU_QUIT, 0, R.string.quit)
                .setIcon(android.R.drawable.ic_lock_power_off)
                .setShortcut('9', 'q');
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PAUSE:
                unbindStepService();
                stopStepService();
                return true;
            case MENU_RESUME:
                startStepService();
                bindStepService();
                return true;
            case MENU_RESET:
                resetValues(true);
                return true;
            case MENU_QUIT:
                resetValues(false);
                unbindStepService();
                stopStepService();
                mQuitting = true;
                finish();
                return true;
        }
        return false;
    }

    // TODO: unite all into 1 type of message
    private StepService.ICallback mCallback = new StepService.ICallback() {
        public void stepsChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }

        public void paceChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(PACE_MSG, value, 0));
        }

        public void distanceChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(DISTANCE_MSG, (int) (value * 1000), 0));
        }

        public void speedChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(SPEED_MSG, (int) (value * 1000), 0));
        }

        public void caloriesChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(CALORIES_MSG, (int) (value), 0));
        }
    };

    private static final int STEPS_MSG = 1;
    private static final int PACE_MSG = 2;
    private static final int DISTANCE_MSG = 3;
    private static final int SPEED_MSG = 4;
    private static final int CALORIES_MSG = 5;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    mStepValue = (int) msg.arg1;
                    mStepValueView.setText("" + mStepValue);
                    break;
                case PACE_MSG:
                    mPaceValue = msg.arg1;
                    if (mPaceValue <= 0) {
                        mPaceValueView.setText("0");
                    } else {
                        mPaceValueView.setText("" + (int) mPaceValue);
                    }
                    break;
                case DISTANCE_MSG:
                    mDistanceValue = ((int) msg.arg1) / 1000f;
                    if (mDistanceValue <= 0) {
                        mDistanceValueView.setText("0");
                    } else {
                        mDistanceValueView.setText(
                                ("" + (mDistanceValue + 0.000001f)).substring(0, 5)
                        );
                    }
                    break;
                case SPEED_MSG:
                    mSpeedValue = ((int) msg.arg1) / 1000f;
                    if (mSpeedValue <= 0) {
                        mSpeedValueView.setText("0");
                    } else {
                        mSpeedValueView.setText(
                                ("" + (mSpeedValue + 0.000001f)).substring(0, 4)
                        );
                    }
                    break;
                case CALORIES_MSG:
                    mCaloriesValue = msg.arg1;
                    if (mCaloriesValue <= 0) {
                        mCaloriesValueView.setText("0");
                    } else {
                        mCaloriesValueView.setText("" + (int) mCaloriesValue);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

    };


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.logout:
                mPedometerSettings.clearServiceRunning();

                stopStepService();
                mGoogleSignInClient.signOut();
                Intent start = new Intent(this, LoginActivity.class);
                start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(start);
                break;
        }
    }

}