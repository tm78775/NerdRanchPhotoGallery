package com.bignerdranch.android.nerdranchphotogallery;

import android.net.Uri;

/**
 * Created by TMiller on 8/17/2016.
 */
public class GalleryItem {
    private String title;
    private String id;
    private String url_s;
    private String owner;
    private Uri link;

    @Override
    public String toString() {
        return title;
    }


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl_s() {
        if (url_s == null) { return ""; }
        return url_s;
    }
    public void setUrl_s(String url_s) {
        // todo: this link is coming back null.
        this.url_s = url_s;
        link = Uri.parse("http://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(owner)
                .appendPath(id)
                .build();
    }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public Uri getPhotoPageUri() {
        return link;
    }
}
