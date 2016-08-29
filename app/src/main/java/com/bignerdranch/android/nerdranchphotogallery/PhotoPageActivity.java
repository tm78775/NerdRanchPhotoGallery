package com.bignerdranch.android.nerdranchphotogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;


/**
 * Created by TMiller on 8/26/2016.
 */
public class PhotoPageActivity extends SingleFragmentActivity implements PhotoPageFragment.Callbacks {

    public static Intent newInstance(Context context, Uri uri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(uri);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    @Override
    public void onBackPressed() {
        if (onBackButtonCalled()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onBackButtonCalled() {
        PhotoPageFragment fragment = (PhotoPageFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        return fragment.onBackButtonCalled();
    }
}
