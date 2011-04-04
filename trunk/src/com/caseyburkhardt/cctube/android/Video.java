package com.caseyburkhardt.cctube.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Video {

    public String mVideoId, mTitle, mAuthor;
    public int mViewCount;
    public double mRating;
    public Bitmap mThumbnail;
    private static VideoSearchActivity sContext = null;
    
    private static int sImageDownloads = 0;
    private static int sAllocations = 0;
    
    public Video(VideoSearchActivity context, String videoId, String title, String author, int viewCount, double rating,
            Uri imageUri) {
        if (sContext == null) {
            sContext = context;
        }
        mVideoId = videoId;
        mTitle = title;
        mAuthor = author;
        mViewCount = viewCount;
        mRating = rating;
        mThumbnail = null;
        sAllocations++;
        new ImageDownloadTask().execute(imageUri);
    }
    
    public static void clearAllocationCounts() {
        sAllocations = sImageDownloads = 0;
    }
    
    private class ImageDownloadTask extends AsyncTask<Uri, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Uri... params) {
            URL fileUrl = null; 
            try {
                fileUrl= new URL(params[0].toString());
                HttpURLConnection conn= (HttpURLConnection) fileUrl.openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                return BitmapFactory.decodeStream(is);
            } catch (Exception e) {
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(Bitmap image) {
            mThumbnail = image;
            sImageDownloads++;
            if (sImageDownloads == sAllocations) {
                sContext.refreshVideoList();
            }
        }
    }
}
