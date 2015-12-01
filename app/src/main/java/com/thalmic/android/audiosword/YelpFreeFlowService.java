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
public class YelpFreeFlowService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "com.thalmic.android.audiosword.action.FOO";
    private static final String ACTION_BAZ = "com.thalmic.android.audiosword.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.thalmic.android.audiosword.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.thalmic.android.audiosword.extra.PARAM2";


    public String[] params = YelpActivity.optionLists[YelpActivity.secondNavSelection];
    public static HashMap<String, String> map = new HashMap<String, String>();

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, YelpFreeFlowService.class);
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
        Intent intent = new Intent(context, YelpFreeFlowService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public YelpFreeFlowService() {
        super("FreeFlow");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(YelpActivity.navLevel == 3) {
            params = YelpActivity.restOptions;
        }
        YelpActivity.tts.setOnUtteranceProgressListener(new ttsUtteranceListenerYelp());
        YelpActivity.incrementMenuCursorPosition(params);
        speakText(params);
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
        YelpActivity.currentContact = params[YelpActivity.menuCursorPosition-1];
        if(YelpActivity.navLevel == 3){
            YelpActivity.helpRestOption = YelpActivity.currentContact;
        }
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, YelpActivity.currentContact);
        YelpActivity.tts.speak(YelpActivity.currentContact, TextToSpeech.QUEUE_ADD, map);

    }
}

class ttsUtteranceListenerYelp extends UtteranceProgressListener {

    public String[] params = YelpActivity.optionLists[YelpActivity.secondNavSelection];

    @Override
    public void onDone(String utteranceId) {
        YelpActivity.tts.playSilence(500, TextToSpeech.QUEUE_ADD, null);
        if(YelpActivity.navLevel == 3){
            params = YelpActivity.restOptions;
        }
        if( YelpActivity.freeFlow == Boolean.TRUE ) {

            YelpActivity.incrementMenuCursorPosition(params);

            YelpFreeFlowService.speakText(params);
        }
    }

    @Override
    public void onError(String utteranceId) {
    }

    @Override
    public void onStart(String utteranceId) {
    }

}

