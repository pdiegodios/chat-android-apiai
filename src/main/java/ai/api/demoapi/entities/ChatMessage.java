package ai.api.demoapi.entities;

import java.util.Random;

/**
 * Created by pedro on 3/31/2017.
 */

public class ChatMessage {

    public String text;
    public boolean isMine;

    public ChatMessage(String text, boolean isMine) {
        this.text = text;
        this.isMine = isMine;
    }
}
