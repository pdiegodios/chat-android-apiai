package ai.api.demoapi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by pedro on 3/23/2017.
 */

public class SplashActivity extends BaseActivity{
    //currently an ANTI-PATTERN --> Developed thinking about when there are things to charge in background

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread timer = new Thread(){
            public void run(){
                try{
                    sleep(1000);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }finally{
                    Intent intent = new Intent("ai.api.demoapi.SignIn");
                    startActivity(intent);
                    finish();
                }
            }
        };
        timer.start();
    }
}
