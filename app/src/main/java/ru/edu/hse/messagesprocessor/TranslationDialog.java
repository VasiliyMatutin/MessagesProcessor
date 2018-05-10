package ru.edu.hse.messagesprocessor;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class TranslationDialog extends Activity {

    public static final String TRANSLATION_KEY = "translation_key";
    public static final String KEY_IS_VOICE_ENABLED = "is_voice_enabled";

    private Speaker speaker;
    private String translated;

    boolean bound = false;
    private ServiceConnection serviceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_popup);

        Bundle extras = getIntent().getExtras();
        translated = (extras == null) ? null : extras.getString(TRANSLATION_KEY);

        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                bound = true;
                Speaker.LocalBinder localBinder = (Speaker.LocalBinder) binder;
                speaker = localBinder.getService();
            }

            public void onServiceDisconnected(ComponentName name) {
                bound = false;
                speaker = null;
            }
        };

        //Intent intent = new Intent(TranslationDialog.this, Speaker.class);
        startService(new Intent(this, Speaker.class));

        if(translated != null)
            displayResult();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, Speaker.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
    private void displayResult(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message =  getResources().getString(R.string.message_string) + "\n\n" + translated;


        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.speak, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        finish();
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkTTSVoiceData();

                String customSharedPrefs = getResources().getString(R.string.custom_shared_preferences);
                SharedPreferences sharedPref = getSharedPreferences(customSharedPrefs, Context.MODE_PRIVATE);
                Boolean isVoiceEnabled = sharedPref.getBoolean(TranslationDialog.KEY_IS_VOICE_ENABLED, false);

                if(isVoiceEnabled) {
                    if(speaker != null && isServiceRunning(Speaker.class))
                        speaker.speak(translated, true);
                }
            }
        });
    }

    private void checkTTSVoiceData(){
        // Check if we have TTS voice data
        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, Speaker.CHECK_TTS_DATA);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Speaker.CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // Data exists, so we instantiate the TTS engine
                enableVoicing();

            } else {
                // Data is missing, so we start the TTS installation process
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                Toast.makeText(this, "Installation required", Toast.LENGTH_LONG).show();
                startActivity(installIntent);
            }
        }
    }

    private void enableVoicing(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.custom_shared_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(KEY_IS_VOICE_ENABLED, true);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        this.stopService(new Intent(this, Speaker.class));
        super.onDestroy();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
