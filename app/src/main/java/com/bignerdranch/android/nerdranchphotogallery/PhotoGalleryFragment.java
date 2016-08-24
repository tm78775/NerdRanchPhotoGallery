package com.bignerdranch.android.nerdranchphotogallery;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

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
    private boolean      mLoadingPhotos;
    private int          mJsonPageNumber;
    private ProgressBar  mSpinner;
    private boolean      mUseSearchParameters;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        PollService.setServiceAlarm(getActivity(), true);

        mJsonPageNumber = 1;
        mUseSearchParameters = false;
        prepareFetchItemsTask();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view          = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getColumnCount()));

        mSpinner = (ProgressBar) view.findViewById(R.id.progressBar1);
        mSpinner.setVisibility(View.VISIBLE);

        setupAdapter();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem item = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) item.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                mJsonPageNumber = 1;
                mUseSearchParameters = true;

                // hide the keyboard.
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                }
                // collapse the searchView.
                searchView.onActionViewCollapsed();
                // clear the recyclerView.
                clearRecyclerView();
                // show the spinner while loading.
                mSpinner.setVisibility(View.VISIBLE);

                queryUsingSearchParams();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                mSpinner.setVisibility(View.VISIBLE);
                QueryPreferences.setStoredQuery(getActivity(), null);
                mJsonPageNumber = 1;
                mUseSearchParameters = false;
                clearRecyclerView();
                prepareFetchItemsTask();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearRecyclerView() {
        PhotoAdapter recyclerAdapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();
        recyclerAdapter.clearRecyclerView();
    }

    public void queryUsingSearchParams() {
        mLoadingPhotos = true;
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(mJsonPageNumber, query).execute();
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
            } else {
                mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
            }

            mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                    if (dy > 0) {
                        GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();

                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount   = layoutManager.getItemCount();
                        int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                        if (!mLoadingPhotos) {
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                Log.i(TAG, "Reached Bottom of screen! Loading more photos.");

                                if (mUseSearchParameters) {
                                    queryUsingSearchParams();
                                } else {
                                    prepareFetchItemsTask();
                                }
                            }
                        }
                    }

                }
            });
        }
    }

    private void prepareFetchItemsTask() {
        mLoadingPhotos = true;
        new FetchItemsTask(mJsonPageNumber, null).execute();
    }

    private void addItemsToAdapter(List<GalleryItem> items) {
        PhotoAdapter adapter = (PhotoAdapter) mPhotoRecyclerView.getAdapter();
        adapter.addItemsToAdapter(items);
    }

    /*
        This class initiates the collecting of data remotely. Utilizing a background thread.
     */
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>  {

        private int mPageNumber;
        private String mQuery;

        // for paging purposes, pass in the page number we want to receive.
        public FetchItemsTask(int pageNumber, String query) {
            mPageNumber = pageNumber;
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {

            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos(mPageNumber);
            } else {
                return new FlickrFetchr().searchPhotos(mQuery, mPageNumber);
            }

        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            addItemsToAdapter(items);
            mJsonPageNumber++;
            mLoadingPhotos = false;
            mSpinner.setVisibility(View.GONE);
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

        public void addItemsToAdapter(List<GalleryItem> galleryItems) {
            for (int i = 0; i < galleryItems.size(); i++) {
                mGalleryItems.add(galleryItems.get(i));
            }
            notifyDataSetChanged();
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
            holder.bindGalleryItem(galleryItem);

            if (!mLoadingPhotos && (position > 12 && position % 10 == 0)) {
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
                    Picasso.with(getActivity())
                            .load(url)
                            .fetch();
                }
            }
        }

        public void clearRecyclerView() {
            mGalleryItems = new ArrayList<>();
            notifyDataSetChanged();
        }
    }
}