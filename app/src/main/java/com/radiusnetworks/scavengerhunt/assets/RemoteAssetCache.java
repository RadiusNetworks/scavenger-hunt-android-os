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
package com.radiusnetworks.scavengerhunt.assets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dyoung on 1/28/14.
 */
public class RemoteAssetCache {
    private static final String TAG = "RemoteAssetCache";
    private int assetsToDownload = 0;
    private int failureCount = 0;
    private Context context;
    private static final Pattern DENSITY_PATTERN = Pattern.compile("^(.*_)([hxl]+)(dpi\\..*)");
    private AssetFetcherCallback callback;
    private Exception lastException;
    private Integer lastResponseCode;

    public RemoteAssetCache(Context context) {
        this.context = context;
    }

    public void downloadAssets(Map<String,String> assetUrls, AssetFetcherCallback callback) {
        if (assetsToDownload > 0) {
            throw new RuntimeException("already downloading assets");
        }
        Log.d(TAG, "downloadAssets called with count of "+assetUrls.size());
        this.callback = callback;
        assetsToDownload = assetUrls.size();

        for (String standardizedFilename : assetUrls.keySet()) {
            final String filenameToSave = standardizedFilename;
            final String assetUrl = assetUrls.get(standardizedFilename);
            Log.d(TAG,"Trying to download asset at "+ assetUrl);
            AssetFetcher assetFetcher = new AssetFetcher(context, assetUrl, filenameToSave, new AssetFetcherCallback() {
                @Override
                public void requestComplete() {
                    Log.d(TAG, "Successfully downloaded "+assetUrl);
                    assetsToDownload--;
                    if (assetsToDownload == 0) {
                        if (failureCount == 0) {
                            RemoteAssetCache.this.callback.requestComplete();
                        }
                        else {
                            RemoteAssetCache.this.callback.requestFailed(lastResponseCode, lastException);
                        }

                    }
                }

                @Override
                public void requestFailed(Integer responseCode, Exception e) {
                    Log.w(TAG, "Failed to load "+ assetUrl);
                    RemoteAssetCache.this.lastException = e;
                    RemoteAssetCache.this.lastResponseCode = responseCode;

                    // If this was a specfic dpi url, fallback to the mdpi url
                    Matcher matcher = DENSITY_PATTERN.matcher(assetUrl);
                    // only try to get the mdpi version if this wasn't the mdpi version
                    if (matcher.matches() && matcher.group(2) != null && !matcher.group(2).equals("m")) {
                        final String mdpiUrl = matcher.group(1) + "m" + matcher.group(3);
                        AssetFetcher assetFetcher = new AssetFetcher(context, mdpiUrl, filenameToSave, new AssetFetcherCallback() {

                            @Override
                            public void requestComplete() {
                                Log.d(TAG, "Successfully downloaded "+mdpiUrl);
                                assetsToDownload--;
                                if (assetsToDownload == 0) {
                                    if (failureCount == 0) {
                                        RemoteAssetCache.this.callback.requestComplete();
                                    }
                                    else {
                                        RemoteAssetCache.this.callback.requestFailed(lastResponseCode, lastException);
                                    }

                                }
                            }

                            @Override
                            public void requestFailed(Integer responseCode, Exception e) {
                                Log.w(TAG, "Failed to load "+ mdpiUrl);
                                RemoteAssetCache.this.lastException = e;
                                RemoteAssetCache.this.lastResponseCode = responseCode;
                                assetsToDownload--;
                                failureCount++;

                            }
                        });
                        assetFetcher.execute();

                    }
                    else {
                        failureCount++;
                        assetsToDownload--;
                    }
                    if (assetsToDownload == 0) {
                        if (failureCount == 0) {
                            RemoteAssetCache.this.callback.requestComplete();
                        }
                        else {
                            RemoteAssetCache.this.callback.requestFailed(lastResponseCode, lastException);
                        }
                    }

                }
            });
            assetFetcher.execute();
        }

    }

    public ImageView getImageByName(String name) {
        String fname = context.getFilesDir().getAbsolutePath()+"/"+name;
        Bitmap bitmap = BitmapFactory.decodeFile(fname);
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(bitmap);
        return imageView;
    }



}
