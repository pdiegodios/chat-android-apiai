package ai.api.demoapi;

import android.content.Context;
import android.speech.tts.TextToSpeech;


public class TTS {

    private static TextToSpeech textToSpeech;

    public static void init(final Context context) {
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    textToSpeech.setSpeechRate(8/10);
                }
            });
        }
    }

    public static void speak(final String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}
