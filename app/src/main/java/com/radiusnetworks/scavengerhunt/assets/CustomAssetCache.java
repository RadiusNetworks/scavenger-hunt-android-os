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
import java.io.FileOutputStream;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by dyoung on 1/28/14.
 *
 * Keeps a cache in the Android filesystem of the images needed for the application.
 * Provides a mechanism to download them and retrieve them later.
 */
public class CustomAssetCache {
    private static final String TAG = "CustomAssetCache";
    private int customAssetsToDownload = 0;
    private int customFailureCount = 0;
    private Context context;
    private static final Pattern SIZE_PATTERN = Pattern.compile("^(.*)(_[0-9]+)(\\..*)");
    private AssetFetcherCallback callback;
    private Exception lastException;
    private Integer lastResponseCode;

    public CustomAssetCache(Context context) {
        this.context = context;
    }



    /**
     * Downloads a set of custom images from a web server, based on a passed map keyed off of
     * the desired local filename that points to the remote URL as a string. These are used
     * to display custom logos within the InstructionActivity and FinishedActivity.
     *
     * All files that were successfully downloaded are stored on the Android
     * file system in the home directory of the application under the filenames in the keys
     * to the assetUrls Map.  If all were downloaded successfully, the requestComplete callback
     * will be called, otherwise the requestFailed callback will be called.
     * @param assetUrls
     * @param callback
     */
    public void downloadCustomAssets(Map<String,String> assetUrls, AssetFetcherCallback callback) {
        if (customAssetsToDownload > 0) {
            throw new RuntimeException("already downloading assets");
        }
        Log.d(TAG, "downloadCustomAssets called with count of "+assetUrls.size());
        this.callback = callback;
        customAssetsToDownload = assetUrls.size();


        for (String standardizedFilename : assetUrls.keySet()) {
            final String filenameToSave = standardizedFilename;
            final String assetUrl = assetUrls.get(standardizedFilename);
            Log.d(TAG,"Trying to download asset at "+ assetUrl);
            Log.d(TAG,"Then saving asset at filenameToSave= "+ filenameToSave);
            AssetFetcher assetFetcher = new AssetFetcher(context, assetUrl, filenameToSave, new AssetFetcherCallback() {
                @Override
                public void requestComplete() {
                    Log.d(TAG, "Successfully downloaded "+assetUrl);
                    customAssetsToDownload--;

                    if (customFailureCount == 0) {
                        CustomAssetCache.this.callback.requestComplete();
                    } else {
                        CustomAssetCache.this.callback.requestFailed(lastResponseCode, lastException);
                    }
                }

                @Override
                public void requestFailed(Integer responseCode, Exception e) {
                    Log.w(TAG, "Failed to load "+ assetUrl);
                    CustomAssetCache.this.lastException = e;
                    CustomAssetCache.this.lastResponseCode = responseCode;
                    customFailureCount++;
                    customAssetsToDownload--;
                }
            });
            assetFetcher.execute();
        }

    }


    public Bitmap getBitmapByName(String name) {
        Bitmap bitmap = null;
        try {
            String fname = context.getFilesDir().getAbsolutePath()+"/"+name;
            bitmap = BitmapFactory.decodeFile(fname);
            if (bitmap == null) {
                Log.d(TAG, "Can't load image named "+name+".  Bitmap is null.");
                return null;
            }
            return bitmap;
        }
        catch (Exception e) {
            Log.d(TAG, "Can't load image named "+name, e);
        }
        return bitmap;
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
     * Returns an ImageView of a scaled image asset in the cache, keyed by the local filename
     * @param name
     * @param targetWidth   Width of the space this image will occupy
     * @return
     */
    public ImageView getImageByName(String name, double targetWidth) {
        ImageView imageView = null;
        try {

            String fname = context.getFilesDir().getAbsolutePath()+"/"+name;

            // Get the dimensions of the original image
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fname, bmOptions);

            double imageW = (double) bmOptions.outWidth;
            double imageH = (double) bmOptions.outHeight;
            double scaleFactor, targetHeight;

            // Determine how much to scale down the image
            if (imageW > imageH){
                //for wide images
                scaleFactor = targetWidth/imageW;
                targetHeight = imageH * scaleFactor;
            } else {
                //for tall images
                scaleFactor = targetWidth/imageH;
                targetHeight = imageH * scaleFactor;
                targetWidth = imageW * scaleFactor;
            }
            //Log.d(TAG, "scaleFactor = "+scaleFactor+", targetWidth = "+targetWidth+", targetHeight = "+targetHeight+ ", imageW = "+ imageW+", imageH = "+ imageH);


            Bitmap bitmap = BitmapFactory.decodeFile(fname);
            if (bitmap == null) {
                Log.d(TAG, "Can't load image named "+name+".  Bitmap is null..");
                return null;
            }

            //scale down at any amount, or scale up if it must be scaled up 200%
            if (((scaleFactor > 0) && (scaleFactor < 1)) || (scaleFactor > 1)) {
                Log.d(TAG,"scaling image "+name+"bitmap to "+scaleFactor);
                // Scaling image to fit tbe space within the gridview
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int)targetWidth, (int)targetHeight, true);
                imageView = new ImageView(context);
                imageView.setImageBitmap(scaledBitmap);
                saveScaledImageToFile(scaledBitmap, fname);
            } else {
                Log.d(TAG,"Scaling not implemented, using original image for "+name);
                imageView = new ImageView(context);
                imageView.setImageBitmap(bitmap);
            }
        }
        catch (Exception e) {
            Log.d(TAG, "Can't load image named "+name, e);
        }
        return imageView;
    }


    private void saveScaledImageToFile(Bitmap bmp, String fname){


        FileOutputStream out = null;
        try {
            out = new FileOutputStream(fname);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try{
                out.close();
            } catch(Throwable ignore) {}
        }

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



    private boolean saveImageToFile(Bitmap bitmap, String filenameToSave) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(context.getFilesDir().getAbsolutePath()+"/"+filenameToSave);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            Log.w(TAG, "Successfully saved file: "+ filenameToSave);

            return true;
        } catch (Exception e2) {
            e2.printStackTrace();
        } finally {
            try{
                out.close();
            } catch(Throwable ignore) {}
        }
        return false;
    }


}
