package com.bignerdranch.android.nerdranchphotogallery;

/**
 * Created by TMiller on 8/17/2016.
 */
public class GalleryItem {
    private String title;
    private String id;
    private String url_s;

    @Override
    public String toString() {
        return title;
    }


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl_s() { return url_s; }
    public void setUrl_s(String url_s) { this.url_s = url_s; }
}
