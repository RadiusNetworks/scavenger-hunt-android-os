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

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dyoung on 1/28/14.
 *
 * Keeps a cache in the Android filesystem of the images needed for the application.
 * Provides a mechanism to download them and retrieve them later.
 */
public class RemoteAssetCache {
    private static final String TAG = "RemoteAssetCache";
    private int assetsToDownload = 0;
    private int failureCount = 0;
    private Context context;
    private static final Pattern SIZE_PATTERN = Pattern.compile("^(.*)(_[0-9]+)(\\..*)");
    private AssetFetcherCallback callback;
    private Exception lastException;
    private Integer lastResponseCode;

    public RemoteAssetCache(Context context) {
        this.context = context;
    }

    /**
     * Downloads a set of images from a web server, based on a passed map keyed off of
     * the desired local filename that points to the remote URL as a string.
     *
     * If the URLString has an image width modifier on it (e.g. _312), and that image
     * cannot be downloaded, this class will retry downloading it from the default url.
     * When complete, all files that were successfully downloaded are stored on the Android
     * file system in the home directory of the application under the filenames in the keys
     * to the assetUrls Map.  If all were downloaded successfully, the requestComplete callback
     * will be called, otherwise the requestFailed callback will be called.
     * @param assetUrls
     * @param callback
     */
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

                    // If this was a specfic size url, fallback to the base url
                    Matcher matcher = SIZE_PATTERN.matcher(assetUrl);
                    // only try to get the mdpi version if this wasn't the mdpi version
                    if (matcher.matches() && matcher.group(2) != null ) {
                        final String standardImageUrl = matcher.group(1)  + matcher.group(3);
                        AssetFetcher assetFetcher = new AssetFetcher(context, standardImageUrl, filenameToSave, new AssetFetcherCallback() {

                            @Override
                            public void requestComplete() {
                                Log.d(TAG, "Successfully downloaded "+standardImageUrl);
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
                                Log.w(TAG, "Failed to load "+ standardImageUrl);
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

    /**
     * Returns an ImageView of an image asset in the cache, keyed by the local filename
     * @param name
     * @return
     */
    public ImageView getImageByName(String name) {
        ImageView imageView = null;
        try {
            String fname = context.getFilesDir().getAbsolutePath()+"/"+name;
            Bitmap bitmap = BitmapFactory.decodeFile(fname);
            if (bitmap == null) {
                Log.d(TAG, "Can't load image named "+name+".  Bitmap is null.");
                return null;
            }
            imageView = new ImageView(context);
            imageView.setImageBitmap(bitmap);
        }
        catch (Exception e) {
            Log.d(TAG, "Can't load image named "+name, e);
        }
        return imageView;
    }

    /**
     * Deletes all cached files
     */
    public void clear() {
        File file = new File(context.getFilesDir().getAbsolutePath());
        String[] files;

        files = file.list();
        for (int i=0; i < files.length; i++) {
            Log.d(TAG, "deleting "+files[i]);
            new File(file, files[i]).delete();
        }
    }



}
