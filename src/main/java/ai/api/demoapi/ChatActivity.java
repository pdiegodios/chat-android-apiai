package ai.api.demoapi;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.demoapi.adapter.ChatAdapter;
import ai.api.demoapi.entities.ChatMessage;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class ChatActivity extends BaseActivity implements View.OnClickListener, View.OnTouchListener {

    private ImageButton sendButton;
    private ImageButton micButton;
    private EditText queryEditText;
    private AIService aiService;

    public static ArrayList<ChatMessage> chatList;
    private ChatAdapter chatAdapter;
    ListView msgListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        TTS.init(getApplicationContext());

        // Views
        sendButton = (ImageButton) findViewById(R.id.sendMessageButton);
        micButton = (ImageButton) findViewById(R.id.recordMessageButton);
        msgListView = (ListView) findViewById(R.id.msgListView);
        queryEditText = (EditText) findViewById(R.id.textQuery);

        // AutoScroll
        msgListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        msgListView.setStackFromBottom(true);

        // Adapter
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatList);
        msgListView.setAdapter(chatAdapter);

        // Handle Keyboard enter action in EditText
        TextView.OnEditorActionListener keyboardListener = new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN){
                    onClick(sendButton);
                }
                return true;
            }
        };

        // Handle text changes to modify visibility
        TextWatcher textChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()>0){
                    findViewById(R.id.sendMessageButton).setVisibility(View.VISIBLE);
                    findViewById(R.id.recordMessageButton).setVisibility(View.GONE);
                }
                else{
                    findViewById(R.id.sendMessageButton).setVisibility(View.GONE);
                    findViewById(R.id.recordMessageButton).setVisibility(View.VISIBLE);
                }

            }
        };

        final AIConfiguration config = new AIConfiguration(Config.ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        // voice recognition
        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

        aiService = AIService.getService(this, config);
        micButton.setOnTouchListener(this);

        queryEditText.setOnEditorActionListener(keyboardListener);
        queryEditText.addTextChangedListener(textChangedListener);

        // Welcome message
        String username = getSharedPreferences(getString(R.string.preference_file),MODE_PRIVATE)
                .getString(getString(R.string.username),"user");
        addChat(getString(R.string.welcome)+" "+username+". "+getString(R.string.help),false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAudioRecordPermission();
    }

    public void onResult(final AIResponse response) {
        //We retrieve email information from SharedPreferences if they ask for that
        if(response.getResult().getAction().equals(getString(R.string.attendance_action)) &&
                !response.getResult().getParameters().containsKey(getString(R.string.usermail)) &&
                response.getResult().getFulfillment().getSpeech().contains("email")){
            // TODO: Look for a better way to do it
            String email = getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE).getString(getString(R.string.usermail),null);
            sendRequest(email,false);
        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Result result = response.getResult();
                    final String speech = result.getFulfillment().getSpeech();
                    //text
                    addChat(speech, false);
                    //audio
                    TTS.speak(speech);
                }

            });
        }
    }

    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addChat(error.toString(), false);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendMessageButton:
                final String queryString = String.valueOf(queryEditText.getText());
                sendRequest(queryString,true);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // use this method to disconnect from speech recognition service
        // Not destroying the SpeechRecognition object in onPause method would block other apps from using SpeechRecognition service
        if (aiService != null) {
            aiService.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // use this method to reinit connection to recognition service
        if (aiService != null) {
            aiService.resume();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //TODO make it work
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                startRecording();
                break;
            case MotionEvent.ACTION_UP:
                stopRecording();
                break;
        }
        return false;
    }

    private void startRecording(){
        // TODO
    }

    private void stopRecording(){
        aiService.stopListening();
    }

    private void sendRequest(final String queryString, boolean shown) {

        if (TextUtils.isEmpty(queryString)) {
            onError(new AIError(getString(R.string.non_empty_query)));
            return;
        }

        final AsyncTask<String, Void, AIResponse> task = new AsyncTask<String, Void, AIResponse>() {

            private AIError aiError;

            @Override
            protected AIResponse doInBackground(final String... params) {
                final AIRequest request = new AIRequest();
                String query = params[0];

                if (!TextUtils.isEmpty(query)) {
                    request.setQuery(query);
                }
                try {
                    return aiService.textRequest(request);
                } catch (final AIServiceException e) {
                    aiError = new AIError(e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final AIResponse response) {
                if (response != null) {
                    onResult(response);
                } else {
                    onError(aiError);
                }
            }
        };

        task.execute(queryString, null, null);
        if(shown)
            addChat(queryString, true);
        queryEditText.setText("");
    }

    private void addChat(String message, boolean isMine){
        final ChatMessage chatMessage = new ChatMessage(message, isMine);
        chatAdapter.add(chatMessage);
        chatAdapter.notifyDataSetChanged();
    }
}
