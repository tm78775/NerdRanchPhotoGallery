package com.bignerdranch.android.nerdranchphotogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by TMiller on 8/17/2016.
 */
public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "10f867f016385159176e080a4beafcd3";
    private static final String REST_ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getrecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri    ENDPOINT = Uri.parse(REST_ENDPOINT).buildUpon()
            .appendQueryParameter("method", FETCH_RECENTS_METHOD)
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }

    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    //public List<GalleryItem> fetchItems(int pageNumber) {
//
    //    ArrayList<GalleryItem> galleryItems = new ArrayList<>();
//
    //    try {
    //        String url = Uri.parse(ENDPOINT.toString()).buildUpon()
    //                        .appendQueryParameter("page", Integer.toString(pageNumber))
    //                        .build()
    //                        .toString();
//
    //        String jsonString = getUrlString(url);
    //        Log.i(TAG, "Received JSON: " + jsonString);
    //        JSONObject jsonBody = new JSONObject(jsonString);
//
    //        parseItems(galleryItems, jsonBody);
//
    //    } catch (IOException e) {
    //        Log.e(TAG, "Failed to fetch items: ", e);
    //    } catch (JSONException e) {
    //        Log.e(TAG, "Failed to parse JSON ", e);
    //    }
//
    //    return galleryItems;
    //}

    public List<GalleryItem> fetchRecentPhotos(int pageNumber) {
        String url = buildUrl(FETCH_RECENTS_METHOD, null, pageNumber);
        Log.i(TAG, "URL generated to fetch recent photos: " + url);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query, int pageNumber) {
        String url = buildUrl(SEARCH_METHOD, query, pageNumber);
        Log.i(TAG, "URL generated to search photos: " + url);
        return downloadGalleryItems(url);
    }

    private String buildUrl(String method, String query, int pageNumber) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);

        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
            if (pageNumber > 0) {
                uriBuilder.appendQueryParameter("page", Integer.toString(pageNumber));
            }
        }

        if (method.equals(FETCH_RECENTS_METHOD)) {
            if (pageNumber > 0) {
                uriBuilder.appendQueryParameter("page", Integer.toString(pageNumber));
            }
        }

        return uriBuilder.build().toString();
    }

    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items");
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse json");
        }
        return items;
    }

    /*
     *  Helper method using Gson library to parse Json string to GalleryItem objects.
     */
    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photosJsonArray = photosJsonObject.getJSONArray("photo");

        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<GalleryItem>>(){}.getType();
        Collection<GalleryItem> photoCollection = gson.fromJson(photosJsonArray.toString(), collectionType);
        GalleryItem[] jsonGalleryItems = photoCollection.toArray(new GalleryItem[photoCollection.size()]);

        for (int i = 0; i < jsonGalleryItems.length; i++) {
            GalleryItem item = jsonGalleryItems[i];
            // only add items if it has a url to a photo.
            if (!item.getUrl_s().isEmpty()) {
                items.add(item);
            }
        }

    }

}
