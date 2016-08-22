package com.bignerdranch.android.nerdranchphotogallery;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Timothy on 8/16/16.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private List<GalleryItem> mItems = new ArrayList<>();
    private RecyclerView mPhotoRecyclerView;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private boolean mLoadingPhotos;
    private int mJsonPageNumber;
    private boolean mReadyToPreload;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mJsonPageNumber = 1;
        prepareFetchItemsTask();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler, getActivity());
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                photoHolder.bindDrawable(drawable);
            }
        });

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background looper thread started.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getColumnCount()));

        setupAdapter();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background looper thread destroyed.");
    }

    private int getColumnCount() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return 5;
        } else {
            return 3;
        }
    }

    private void setupAdapter() {
        if (isAdded()) {
            PhotoAdapter adapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();
            if (adapter != null && adapter.mGalleryItems.size() > 0) {
                adapter.mGalleryItems.addAll(mItems);
                mPhotoRecyclerView.setAdapter(adapter);
                mPhotoRecyclerView.getLayoutManager().scrollToPosition( (mJsonPageNumber - 1) * 100);

            } else {
                mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
            }

            mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                    if (dy > 0) {
                        GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                        if (!mLoadingPhotos) {
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                Log.i(TAG, "Reached Bottom of screen! Loading more photos.");
                                prepareFetchItemsTask();
                            }
                        }
                    }

                }
            });
        }
    }

    private void prepareFetchItemsTask() {
        mLoadingPhotos = true;
        mReadyToPreload = false;
        new FetchItemsTask(mJsonPageNumber).execute();
    }


    /*
        This class initiates the collecting of data remotely. Utilizing a background thread.
     */
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>  {

        private int mPageNumber;

        // for paging purposes, pass in the page number we want to receive.
        public FetchItemsTask(int pageNumber) {
            mPageNumber = pageNumber;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            return new FlickrFetchr().fetchItems(mPageNumber);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = null;
            mItems = items;
            setupAdapter();
            mJsonPageNumber++;
            mLoadingPhotos = false;
            mReadyToPreload = true;
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            Picasso.with(getActivity())
                .load(galleryItem.getUrl_s())
                .placeholder(R.mipmap.eli)
                .into(mItemImageView);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeHolder = getResources().getDrawable(R.mipmap.eli);
            holder.bindDrawable(placeHolder);
            mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl_s());
            if ( mReadyToPreload && (position > 12 && position % 10 == 0) ) {
                preloadImages(position);
            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        private void preloadImages(int startPosition) {
            if (startPosition > 3) {
                int endPosition = startPosition + 20;
                if (endPosition > mGalleryItems.size() - 1) {
                    endPosition = startPosition + ((mGalleryItems.size() - 1) - startPosition);
                }

                for (int i = startPosition; i < endPosition; i++) {
                    String url = mGalleryItems.get(i).getUrl_s();
                    mThumbnailDownloader.queueImageForPreload(url);
                }
            }
        }
    }
}