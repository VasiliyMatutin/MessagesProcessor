package ru.edu.hse.messagesprocessor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public class Speaker extends Service implements TextToSpeech.OnInitListener {

    private static TextToSpeech mTTS = null;
    private final IBinder binder = new LocalBinder();

    public static final int CHECK_TTS_DATA = 1000;

    public static final String TEXT_KEY = "text_key";
    public static final String Q_KEY = "query_mode_key";

    public static final int OK = 0;
    public static final int TTS_IS_NULL = 1;
    public static final int TEXT_IS_EMPTY = 2;
    public static final int VOICE_IS_DISABLED = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        mTTS = new TextToSpeech(this, this);
    }

    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (mTTS != null) {
                String custom_shared_prefs = this.getApplication().getString(R.string.custom_shared_preferences);
                String target_language_code = this.getApplication().getString(R.string.saved_target_language_code);

                SharedPreferences sharedPref = this.getApplication().getSharedPreferences(custom_shared_prefs, Context.MODE_PRIVATE);
                String targetLanguageCode = sharedPref.getString(target_language_code, SMSTranslator.defaultString);

                String tts_error = this.getApplication().getString(R.string.tts_isnt_supported_error);
                String tts_ready = this.getApplication().getString(R.string.tts_ready);

                int result = mTTS.setLanguage(new Locale(targetLanguageCode));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, tts_error, Toast.LENGTH_LONG).show();
                    Log.e("TTS", "Initialization failed!");
                } else {
                    //Toast.makeText(this, "I can speak!", Toast.LENGTH_LONG).show(); //Useful for testing
                    //speak(tts_ready, true);
                    Log.i("TTS", "Initialization succeeded");
                }
            }
        } else {
            String tts_failed = getResources().getString(R.string.tts_init_failed_error);
            Toast.makeText(this, tts_failed, Toast.LENGTH_LONG).show();
            Log.i("TTS", "Initialization failed! TTs is null.");
        }
    }

    public int onStartCommand (Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public int speak(String text, boolean qmode) {
        final String tag = this.getClass().getSimpleName();
        Log.i(tag, "Text to speak:\n" + text);

        if (mTTS == null){
            Log.e(tag, "TTS is null. Exiting.");
            return TTS_IS_NULL;
        }
        if (text.isEmpty()){
            Log.e(tag, "Text is empty. Exiting.");
            return TEXT_IS_EMPTY;
        }
        if(VOICE_IS_DISABLED == check()){
            Log.e(tag, "Voice disabled. Exiting.");
            //return VOICE_IS_DISABLED; //For some reason the value is always disabled TODO: investigate further to uncomment
        }

        if (qmode)
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        else
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

        Log.i(tag, "Success");
        return OK;
    }

    private int check(){
        String customSharedPrefs = this.getApplication().getString(R.string.custom_shared_preferences);
        SharedPreferences sharedPref = this.getApplication().getSharedPreferences(customSharedPrefs, Context.MODE_PRIVATE);
        Boolean isVoiceEnabled = sharedPref.getBoolean(TranslationDialog.KEY_IS_VOICE_ENABLED, false);

        if (!isVoiceEnabled) {
            return VOICE_IS_DISABLED;
        } else {
            return OK;
        }
    }

    @Override
    public void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public Speaker getService() {
            return Speaker.this;
        }
    }

}
