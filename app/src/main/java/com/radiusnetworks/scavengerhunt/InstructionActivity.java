package com.radiusnetworks.scavengerhunt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;


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

        //setting custom logo
        ImageView imageView =  (ImageView) this.findViewById(R.id.imageView);
        Bitmap bitmap = application.getCustomAssetCache().getBitmapByName("instruction_image");
        if (bitmap != null)
            imageView.setImageBitmap(bitmap);
        else Log.e(TAG, "custom splash image == null when pulled from file.");


        //setting custom text and background color
        Map<String,String> customStartScreenData = application.getHunt().getCustomStartScreenData();

        String colorHex = application.getHunt().getCustomStartScreenData().get("instruction_background_color");
        colorHex = colorHex.replaceAll("0x", "#");
        int color = Color.parseColor(colorHex);
        getWindow().getDecorView().setBackgroundColor(color);

        ((TextView) this.findViewById(R.id.title1)).setText(customStartScreenData.get("instruction_title"));
        ((TextView) this.findViewById(R.id.instructions)).setText(customStartScreenData.get("instruction_text_1"));
        ((Button) this.findViewById(R.id.sh_instruction_button)).setText(customStartScreenData.get("instruction_start_button_name"));

    }


    private void triggerSplashAnimation(){
        //fading out splash screen
        ImageView splash = (ImageView) this.findViewById(R.id.splash);
        Bitmap bitmap = application.getCustomAssetCache().getBitmapByName("splash");
        if (bitmap != null)
            splash.setImageBitmap(bitmap);
        else Log.e(TAG, "custom splash image == null when pulled from file.");

        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        splash.startAnimation(fadeOut);
    }

    private void hideSplashImage() {
        //fading out splash screen
        ImageView imageView = (ImageView) this.findViewById(R.id.splash);
        imageView.setVisibility(View.INVISIBLE);
    }


}
