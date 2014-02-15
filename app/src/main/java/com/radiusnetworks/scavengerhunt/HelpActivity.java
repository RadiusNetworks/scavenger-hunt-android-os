package com.radiusnetworks.scavengerhunt;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Created by dyoung on 2/14/14.
 */
public class HelpActivity extends Activity {
    public static final String TAG = "HelpActivity";
    private ScavengerHuntApplication application = null;
    private boolean validatingCode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sh_activity_help);

        application = (ScavengerHuntApplication) this.getApplication();
        WebView webView = (WebView) this.findViewById(R.id.webView);
       // webView.setFocusableInTouchMode(false);
        //webView.setFocusable(false);
        webView.loadUrl("http://developer.radiusnetworks.com/scavenger_hunt/help.html");
    }
}