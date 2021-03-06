package com.thalmic.android.audiosword;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FreeFlowService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "com.thalmic.android.audiosword.action.FOO";
    private static final String ACTION_BAZ = "com.thalmic.android.audiosword.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.thalmic.android.audiosword.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.thalmic.android.audiosword.extra.PARAM2";


    public String[] params = ConfigActivity.callLists[ConfigActivity.secondNavSelection];
    public static HashMap<String, String> map = new HashMap<String, String>();

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, FreeFlowService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, FreeFlowService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public FreeFlowService() {
        super("FreeFlow");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
        ConfigActivity.tts.setOnUtteranceProgressListener(new ttsUtteranceListener());
        ConfigActivity.incrementMenuCursorPosition(params);
        speakText(params);

//        do {
//            Log.d("freeflow", "me");
//            if(!ConfigActivity.tts.isSpeaking()){
//                ConfigActivity.menuCursorPosition = 0;
//                for(int i = ConfigActivity.menuCursorPosition; i<params.length; i++) {
//                    ConfigActivity.menuCursorPosition++;
//                    ConfigActivity.currentContact = params[ConfigActivity.menuCursorPosition-1];
//
//                    ConfigActivity.addSpeechtoQueue(ConfigActivity.currentContact);
//                    ConfigActivity.tts.playSilence(500, TextToSpeech.QUEUE_ADD, null);
//                }
//            }
//
//        } while (ConfigActivity.freeFlow == Boolean.TRUE);
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static void speakText(String[] params) {
        ConfigActivity.currentContact = params[ConfigActivity.menuCursorPosition-1];
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ConfigActivity.currentContact);
        ConfigActivity.tts.speak(ConfigActivity.currentContact, TextToSpeech.QUEUE_ADD, map);

    }
}

class ttsUtteranceListener extends UtteranceProgressListener {

    public String[] params = ConfigActivity.callLists[ConfigActivity.secondNavSelection];

    @Override
    public void onDone(String utteranceId) {
        ConfigActivity.tts.playEarcon(EarconManager.swooshEarcon, TextToSpeech.QUEUE_ADD, null);
        ConfigActivity.tts.playSilence(500, TextToSpeech.QUEUE_ADD, null);
        if( ConfigActivity.freeFlow == Boolean.TRUE ) {
            ConfigActivity.incrementMenuCursorPosition(params);
            FreeFlowService.speakText(params);
        }
    }

    @Override
    public void onError(String utteranceId) {
    }

    @Override
    public void onStart(String utteranceId) {
    }

}

