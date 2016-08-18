package com.bignerdranch.android.nerdranchphotogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TMiller on 8/17/2016.
 */
public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "10f867f016385159176e080a4beafcd3";
    private static final String REST_ENDPOINT = "https://api.flickr.com/services/rest/"; // ?method=%1$s&api_key=%2$s&format=json&nojsoncallback=1
    private static final String GET_PHOTOS_METHOD = "flickr.photos.getrecent";

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

    public List<GalleryItem> fetchItems() {

        ArrayList<GalleryItem> galleryItems = new ArrayList<>();

        try {
            String url = Uri.parse(REST_ENDPOINT).buildUpon()
                    .appendQueryParameter("method", GET_PHOTOS_METHOD)
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build()
                    .toString();

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);

            parseItems(galleryItems, jsonBody);

        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items: ", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON ", e);
        }

        return galleryItems;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photosJsonArray = photosJsonObject.getJSONArray("photo");
        Gson gson = new Gson();

        for (int i = 0; i < photosJsonArray.length(); i++) {
            // GalleryItem item = gson.fromJson(photosJsonArray.get(i).toString(), GalleryItem.class);
            // GalleryItem item = gson.fromJson(photosJsonArray.get(i).toString())

            String stopping = "stop here";
            List<String> toAdd = new ArrayList<>();
            toAdd.add(stopping);
            // JSONObject photoJsonObject = photosJsonArray.getJSONObject(i);
//
            // GalleryItem item = new GalleryItem();
            // item.setId(photoJsonObject.getString("id"));
            // item.setCaption(photoJsonObject.getString("title"));
//
            // if (!photoJsonObject.has("url_s")) {
            //     continue;
            // }
//
            // item.setUrl(photoJsonObject.getString("url_s"));
            // items.add(item);
        }
    }

}
