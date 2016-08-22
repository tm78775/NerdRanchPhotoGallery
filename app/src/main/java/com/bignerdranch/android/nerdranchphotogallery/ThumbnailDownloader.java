package com.bignerdranch.android.nerdranchphotogallery;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by TMiller on 8/18/2016.
 */
public class ThumbnailDownloader<T> extends HandlerThread {

    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int PRELOAD_IMAGE = 1;
    private static final String TAG = "ThumbnailDownloader";

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private Handler mPreloadImageHandler;
    private Context mContext;
    private LruCache<String,Bitmap> mCache;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private List<String> mImagePreloadList = Collections.synchronizedList(new ArrayList<String>());
    private boolean mReadyToPreloadImages = false;


    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setReadyToPreloadImages(boolean ready) {
        mReadyToPreloadImages = ready;
    }

    public ThumbnailDownloader(Handler responseHandler, Context context) {
        super(TAG);
        mResponseHandler = responseHandler;
        mContext = context;
        mCache = new LruCache(calculateMemoryAvailable());
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }

    }

    public void queueImageForPreload(String url) {

        if (url == null || url.isEmpty()) {
            mImagePreloadList.remove(url);
        } else {
            mImagePreloadList.add(url);
            Log.i(TAG, "URL added for image preload: " + url);
            Bitmap existingImage = mCache.get(url);
            if (existingImage == null) {
                mPreloadImageHandler.obtainMessage(PRELOAD_IMAGE, url)
                        .sendToTarget();
            }
        }

    }

    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };

        mPreloadImageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == PRELOAD_IMAGE) {
                    String target = (String) msg.obj;
                    int targetIndex = mImagePreloadList.indexOf(target);
                    Log.i(TAG, "Got a request for image preload URL: " + mImagePreloadList.get(targetIndex));
                    handlePreloadRequest(target);
                }
            }
        };
    }


    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }

            final Bitmap cachedBitmap = mCache.get(url);
            if (cachedBitmap == null) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                final Bitmap bitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mCache.put(url, bitmap);
                Log.i(TAG, "Bitmap added to Cache.");
                sendBitmapToUIResponseHandler(target, bitmap);
            } else {
                Log.i(TAG, "Using cached bitmap");
                sendBitmapToUIResponseHandler(target, cachedBitmap);
            }

        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    private void handlePreloadRequest(final String target) {
        final String url;
        if(mImagePreloadList.contains(target)) {
            int index = mImagePreloadList.indexOf(target);
            url = mImagePreloadList.get(index);
            mImagePreloadList.remove(index);

            if (url == null) {
                return;
            }

            Bitmap cachedImage = mCache.get(url);
            if (cachedImage == null) {
                try {
                    byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                    final Bitmap bitmap = BitmapFactory
                            .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                    mCache.put(url, bitmap);
                    Log.i(TAG, "Bitmap added to Cache.");
                } catch (IOException ioe) {
                    Log.e(TAG, "Error downloading image in preload request.");
                }
            }
        }


    }

    private void sendBitmapToUIResponseHandler(final T target, final Bitmap bitmap) {
        mResponseHandler.post(new Runnable() {
            public void run() {
                if (mHasQuit /*|| mRequestMap.get(target) != url*/) {
                    return;
                }
                mRequestMap.remove(target);
                mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
            }
        });
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    private int calculateMemoryAvailable() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        int availMemInBytes = am.getMemoryClass() * 1024 * 1024;
        availMemInBytes = availMemInBytes / 8;

        return availMemInBytes;
    }
}
