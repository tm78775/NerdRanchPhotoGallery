package com.bignerdranch.android.nerdranchphotogallery;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Timothy on 8/16/16.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private List<GalleryItem> mItems = new ArrayList<>();
    private RecyclerView mPhotoRecyclerView;
    private boolean mLoadingPhotos;
    private int mJsonPageNumber;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mLoadingPhotos = true;
        mJsonPageNumber = 1;
        new FetchItemsTask(mJsonPageNumber).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getColumnCount()));

        setupAdapter();

        return view;
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
                                mLoadingPhotos = true;
                                Log.i(TAG, "Reached Bottom of screen! Loading more photos.");
                                new FetchItemsTask(mJsonPageNumber).execute();
                            }
                        }
                    }

                }
            });
        }
    }



    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>  {

        private int mPageNumber;

        public FetchItemsTask(int pageNumber) {
            mPageNumber = pageNumber;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            return new FlickrFetchr().fetchItems(mPageNumber);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();
            mJsonPageNumber++;
            mLoadingPhotos = false;
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private TextView mTitleTextView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mTitleTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item) {
            mTitleTextView.setText(item.toString());
        }

    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
}