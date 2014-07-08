package com.radiusnetworks.scavengerhunt;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * this Activity displays a shows the custom splash and custom instruction
 * screen if configured
 *
 * Created by fnguyen on 07/08/14.
 */
public class InstructionActivity extends Activity {
    public static final String TAG = "InstructionActivity";
    private ScavengerHuntApplication application = null;

    Handler splashHandler = new Handler();
    Runnable splashRunnable = new Runnable() {

        @Override
        public void run() {
            triggerSplashAnimation();
        }
    };

    Runnable splashHide = new Runnable() {

        @Override
        public void run() {
            hideSplashImage();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (ScavengerHuntApplication) this.getApplication();
        application.setInstructionActivity(this);

        setupInstructionView();
        splashHandler.postDelayed(splashRunnable, 2000);


    }


    private void setupInstructionView() {
        setContentView(R.layout.sh_activity_instruction);

        View startButton = (TextView) this.findViewById(R.id.sh_instruction_button);
        if (startButton != null) {
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(InstructionActivity.this, TargetCollectionActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(i);
                    InstructionActivity.this.finish();
                    return;

                }
            });
        }
    }


    private void triggerSplashAnimation(){
        //fading out splash screen
        ImageView imageView = (ImageView) this.findViewById(R.id.splash);
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        imageView.startAnimation(fadeOut);
    }

    private void hideSplashImage() {
        //fading out splash screen
        ImageView imageView = (ImageView) this.findViewById(R.id.splash);
        imageView.setVisibility(View.INVISIBLE);
    }


}
