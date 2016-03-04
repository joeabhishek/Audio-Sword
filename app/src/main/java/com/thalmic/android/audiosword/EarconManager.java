package com.thalmic.android.audiosword;

import android.speech.tts.TextToSpeech;

/**
 * Created by Mojo on 3/4/16.
 */
public class EarconManager {

    public static String swooshEarcon = String.valueOf(R.string.swoosh_earcon);
    public static String lockEarcon = String.valueOf(R.string.lock_earcon);;
    public static String unlockEarcon = String.valueOf(R.string.unlock_earcon);;
    public static String selectEarcon = String.valueOf(R.string.select_earcon);;


    public static void setupEarcons(TextToSpeech tts, String packageName) {
        tts.addEarcon(swooshEarcon, packageName, R.raw.swoosh);
        tts.addEarcon(lockEarcon, packageName, R.raw.lock);
        tts.addEarcon(unlockEarcon, packageName, R.raw.unlock);
        tts.addEarcon(selectEarcon, packageName, R.raw.select);
    }

}
