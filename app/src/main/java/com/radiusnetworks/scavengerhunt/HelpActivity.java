/**
 *
 * Copyright (c) 2013,2014 RadiusNetworks. All rights reserved.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 *
 * Licensed to the Attribution Assurance License (AAL)
 * (adapted from the original BSD license) See the LICENSE file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 */
package com.radiusnetworks.scavengerhunt;

import android.app.Activity;
import android.content.Intent;
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
        webView.loadUrl("http://developer.radiusnetworks.com/scavenger_hunt/help.html");
    }

}