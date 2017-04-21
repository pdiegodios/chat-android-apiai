package ai.api.demoapi.adapter;

import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import ai.api.demoapi.R;
import ai.api.demoapi.entities.ChatMessage;
import ai.api.demoapi.utilities.Draw;

/**
 * Created by pedro on 3/31/2017.
 */

public class ChatAdapter extends BaseAdapter {

    private static LayoutInflater inflater = null;
    private Bitmap userBitmap = null;
    private Context context;
    ArrayList<ChatMessage> chatMessageList;

    public ChatAdapter(Activity activity, ArrayList<ChatMessage> list) {
        chatMessageList = list;
        context=activity;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return chatMessageList.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage message = (ChatMessage) chatMessageList.get(position);
        View vi = convertView;
        if (message.isMine) {
            vi = inflater.inflate(R.layout.chatbubble_user, null);
            LinearLayout parent_layout = (LinearLayout) vi
                    .findViewById(R.id.bubble_layout_user_parent);
            parent_layout.setGravity(Gravity.RIGHT);
            ImageView avatar = (ImageView) vi.findViewById(R.id.avatar);
            String url = context.getSharedPreferences(context.getString(R.string.preference_file),Context.MODE_PRIVATE).getString("userphoto",null);
            if(url!=null && userBitmap==null){
                new DownloadImageTask(avatar)
                        .execute(url);
            }
            else if(userBitmap!=null){
                avatar.setImageBitmap(userBitmap);
            }
            //TODO: set user pic
        }
        else{
            vi = inflater.inflate(R.layout.chatbubble_bot, null);
        }

        TextView msg = (TextView) vi.findViewById(R.id.message_text);
        msg.setText(message.text);
        msg.setTextColor(Color.BLACK);
        return vi;
    }

    public void add(ChatMessage object) {
        chatMessageList.add(object);
    }



    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urlDisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            userBitmap = Draw.clippedCircle(result);
            bmImage.setImageBitmap(userBitmap);
        }
    }
}
