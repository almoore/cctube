
package com.caseyburkhardt.cctube.android;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class VideoSearchActivity extends Activity {

    private static final String sAPI_SCHEME = "http";

    private static final String sAPI_AUTHORITY = "gdata.youtube.com";

    private static final String sAPI_PATH = "feeds/api/videos";

    private static final String sUSER_API_PATH = "feeds/api/users";

    private static final String sUSER_API_PATH_SUFFIX = "uploads";

    private static final String sAPI_KEY = "AI39si6apsbwn9ALLe9LQzeh_Zm1bXoWIVqChE5PT7j7ggVP7OzBIlXDsEPQXxXx6kINhBA8zP9YNBHXHNnDGyNt-JLnIPu3fQ";

    private static final String sAPI_CAPTION = "true";

    private static final String sAPI_ALT = "json";

    private static final String sAPI_V = "2";

    private VideoSearchActivity self = null;

    private EditText mSearchQuery = null;

    private String mSearchText = null;

    private ImageButton mSearchButton = null;

    private VideoSearchTask mSearchTask = null;

    private ChannelDataTask mChannelDataTask = null;

    private ProgressDialog mProgressDialog = null;

    private ArrayList<Video> mVideos = null;

    private VideoAdapter mVideoAdapter = null;

    private ListView mVideoList = null;

    private DecimalFormat mFormatter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_search);
        self = this;
        mSearchQuery = (EditText) findViewById(R.id.query);
        mSearchQuery.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.i("CaptionTube", "onKey" + keyCode);
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            beginSearch();
                            return true;
                    }
                }
                return false;
            }
        });
        mSearchButton = (ImageButton) findViewById(R.id.search);
        mSearchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                beginSearch();
            }
        });
        mVideoList = (ListView) findViewById(R.id.video_list);
        mVideoList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
                playVideo(mVideos.get(pos).mVideoId);
            }
        });
        mVideoList.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                menu.add(0, 0, 0, "Play Video");
                menu.add(0, 1, 1, "Share Video");
                menu.add(0, 2, 2, "View Channel Info");
            }
        });
    }

    public void beginSearch() {
        Log.d("CaptionTube", "Beginning Search");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchQuery.getWindowToken(), 0);
        mSearchText = mSearchQuery.getText().toString();
        if (mSearchText == null || mSearchText.length() != 0) {
            Video.clearAllocationCounts();
            mProgressDialog = ProgressDialog.show(self, "Searching", "Please wait...", false, true,
                    new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            mSearchTask.cancel(true);
                        }
                    });
            mSearchTask = new VideoSearchTask();
            URI searchUri = null;
            try {
                searchUri = new URI(new Uri.Builder().scheme(sAPI_SCHEME).authority(sAPI_AUTHORITY)
                        .path(sAPI_PATH).appendQueryParameter("key", sAPI_KEY)
                        .appendQueryParameter("q", mSearchText)
                        .appendQueryParameter("alt", sAPI_ALT).appendQueryParameter("v", sAPI_V)
                        .appendQueryParameter("caption", sAPI_CAPTION).build().toString());
            } catch (URISyntaxException e) {
                displaySimpleAlertDialog("Search Failed",
                        "Unable to encode request.  Please try again.");
                return;
            }
            mSearchTask.execute(searchUri);
        } else {
            displaySimpleAlertDialog("Invalid Request", "Please enter a larger search query.");
        }
    }

    public void playVideo(String videoId) {
        String videoUrl = "vnd.youtube:" + videoId;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)));
    }

    public void shareVideo(String title, String videoId) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "http://youtube.com/watch?v="
                + videoId);
        startActivity(Intent.createChooser(shareIntent, "Share Video Using..."));
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        switch (item.getItemId()) {
            case 0:
                playVideo(mVideos.get(menuInfo.position).mVideoId);
                break;
            case 1:
                shareVideo(mVideos.get(menuInfo.position).mTitle,
                        mVideos.get(menuInfo.position).mVideoId);
                break;
            case 2:
                displayChannelData(mVideos.get(menuInfo.position).mAuthor);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    protected void displayChannelData(String authorName) {
        Log.d("CaptionTube", "Beginning Channel Data Requests");
        if (authorName == null || authorName.length() != 0) {
            mProgressDialog = ProgressDialog.show(self, "Retrieving Channel Data",
                    "Please wait...", false, true, new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            mChannelDataTask.cancel(true);
                        }
                    });
            mChannelDataTask = new ChannelDataTask();
            URI channelDataUri, captionDataUri = null;
            try {
                channelDataUri = new URI(new Uri.Builder().scheme(sAPI_SCHEME)
                        .authority(sAPI_AUTHORITY).path(sUSER_API_PATH).appendPath(authorName)
                        .appendPath(sUSER_API_PATH_SUFFIX).appendQueryParameter("key", sAPI_KEY)
                        .appendQueryParameter("alt", sAPI_ALT).appendQueryParameter("v", sAPI_V)
                        .build().toString());
                captionDataUri = new URI(new Uri.Builder().scheme(sAPI_SCHEME)
                        .authority(sAPI_AUTHORITY).path(sAPI_PATH)
                        .appendQueryParameter("key", sAPI_KEY)
                        .appendQueryParameter("alt", sAPI_ALT).appendQueryParameter("v", sAPI_V)
                        .appendQueryParameter("author", authorName)
                        .appendQueryParameter("caption", sAPI_CAPTION).build().toString());
                Log.d("CaptionTube", "URI's Built: " + channelDataUri.toString() + " & "
                        + captionDataUri.toString());
            } catch (URISyntaxException e) {
                displaySimpleAlertDialog("Search Failed",
                        "Unable to encode request.  Please try again.");
                return;
            }
            URI[] request = {
                    channelDataUri, captionDataUri
            };
            mChannelDataTask.execute(request);
        } else {
            displaySimpleAlertDialog("Invalid Request", "Please enter a larger search query.");
        }
    }

    protected void refreshVideoList() {
        mVideoAdapter.notifyDataSetChanged();
    }

    protected void displaySimpleAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(message).setCancelable(true)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                }).create().show();
    }

    private class VideoSearchTask extends AsyncTask<URI, Void, String> {
        @Override
        protected String doInBackground(URI... requests) {
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(requests[0]);
                HttpResponse responseGet = client.execute(get);
                HttpEntity resEntityGet = responseGet.getEntity();
                if (resEntityGet != null) {
                    return EntityUtils.toString(resEntityGet);
                }
            } catch (Exception e) {/* Exception handled on UI thread. */
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                JSONObject jsonResult = null;
                int feedSize = 0;
                try {
                    jsonResult = new JSONObject(result).getJSONObject("feed");
                    feedSize = jsonResult.getJSONObject("openSearch$totalResults").getInt("$t");
                } catch (JSONException e) {
                    displaySimpleAlertDialog(
                            "Bad Response",
                            "The YouTube data server returned a malformed response.  Please check your network connection and try again.");
                }
                if (feedSize > 0) {
                    JSONArray jsonFeedEntries = null;
                    try {
                        jsonFeedEntries = jsonResult.getJSONArray("entry");
                        mVideos = new ArrayList<Video>();
                        for (int i = 0; i < jsonFeedEntries.length(); ++i) {
                            JSONObject thisEntry = jsonFeedEntries.getJSONObject(i);
                            String videoId = thisEntry.getJSONObject("media$group")
                                    .getJSONObject("yt$videoid").getString("$t");
                            String title = thisEntry.getJSONObject("title").getString("$t");
                            String author = thisEntry.getJSONArray("author").getJSONObject(0)
                                    .getJSONObject("name").getString("$t");
                            int viewCount = thisEntry.getJSONObject("yt$statistics").getInt(
                                    "viewCount");
                            double rating = -1;
                            if (thisEntry.has("gd$rating")) {
                                rating = thisEntry.getJSONObject("gd$rating").getDouble("average");
                            }
                            Uri imageUri = Uri.parse(thisEntry.getJSONObject("media$group")
                                    .getJSONArray("media$thumbnail").getJSONObject(0)
                                    .getString("url"));
                            Log.i("CaptionTube", "Title: " + title + " Id: " + videoId
                                    + " Author: " + author + " Views: " + viewCount + " Rating: "
                                    + rating + " Image: " + imageUri.toString());
                            mVideos.add(new Video(self, videoId, title, author, viewCount, rating,
                                    imageUri));
                        }
                        mVideoAdapter = new VideoAdapter(self, R.layout.video_row, mVideos);
                        mVideoList.setAdapter(mVideoAdapter);
                        mVideoAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        displaySimpleAlertDialog(
                                "Bad Response",
                                "The YouTube data server returned a malformed response.  Please check your network connection and try again.");
                    }
                } else {
                    displaySimpleAlertDialog("No Results",
                            "Could not locate any captioned YouTube videos matching your search terms.");
                }
            } else {
                displaySimpleAlertDialog("Network Connection Failure",
                        "Please check your network connection and try again.");
            }
            mProgressDialog.dismiss();
        }
    }

    private class VideoAdapter extends ArrayAdapter<Video> {

        private ArrayList<Video> items;

        public VideoAdapter(Context context, int textViewResourceId, ArrayList<Video> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.video_row, null);
            }
            Video thisVideo = items.get(position);
            if (thisVideo != null) {
                TextView topText = (TextView) view.findViewById(R.id.toptext);
                TextView middleText = (TextView) view.findViewById(R.id.middletext);
                TextView bottomText = (TextView) view.findViewById(R.id.bottomtext);
                ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
                if (topText != null) {
                    topText.setText(thisVideo.mTitle);
                }
                if (middleText != null) {
                    if (mFormatter == null) {
                        mFormatter = new DecimalFormat("###,###,###,###");
                    }
                    middleText.setText("By " + thisVideo.mAuthor + ", "
                            + mFormatter.format(thisVideo.mViewCount) + " views");
                }
                if (bottomText != null) {
                    if (thisVideo.mRating != -1) {
                        bottomText
                                .setText(Math.round(thisVideo.mRating * 20) + "% caption rating.");
                    } else {
                        bottomText.setText("Caption rating unavailable.");
                    }
                }
                if (thumbnail != null) {
                    if (thisVideo.mThumbnail != null) {
                        thumbnail.setImageBitmap(thisVideo.mThumbnail);
                    } else {
                        thumbnail.setImageResource(R.drawable.loader);
                    }
                }
            }
            return view;
        }
    }

    private class ChannelDataTask extends AsyncTask<URI, Void, String[]> {
        @Override
        protected String[] doInBackground(URI... requests) {
            String[] response = new String[2];
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(requests[0]);
                HttpResponse responseGet = client.execute(get);
                HttpEntity resEntityGet = responseGet.getEntity();
                if (resEntityGet != null) {
                    response[0] = EntityUtils.toString(resEntityGet);
                } else {
                    return null;
                }
                client = new DefaultHttpClient();
                get = new HttpGet(requests[1]);
                responseGet = client.execute(get);
                resEntityGet = responseGet.getEntity();
                if (resEntityGet != null) {
                    response[1] = EntityUtils.toString(resEntityGet);
                    return response;
                }
            } catch (Exception e) {/* Exception handled on UI thread. */
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] results) {
            if (results != null) {
                JSONObject fullResult, captionedResult = null;
                int uploadedVideos = 0, captionedVideos = 0;
                String authorName = "";
                try {
                    fullResult = new JSONObject(results[0]).getJSONObject("feed");
                    captionedResult = new JSONObject(results[1]).getJSONObject("feed");
                    uploadedVideos = fullResult.getJSONObject("openSearch$totalResults").getInt(
                            "$t");
                    captionedVideos = captionedResult.getJSONObject("openSearch$totalResults")
                            .getInt("$t");
                    authorName = fullResult.getJSONArray("author").getJSONObject(0)
                            .getJSONObject("name").getString("$t");
                } catch (JSONException e) {
                    displaySimpleAlertDialog(
                            "Bad Response",
                            "The YouTube data server returned a malformed response.  Please check your network connection and try again.");
                    return;
                }
                if (uploadedVideos > 0) {
                    displaySimpleAlertDialog("Channel Information",
                            authorName + " has uploaded " + uploadedVideos + " videos, "
                                    + captionedVideos + " of which have captions.  This gives "
                                    + authorName + "'s channel a caption friendliness rating of "
                                    + Math.round(((double) captionedVideos / uploadedVideos) * 100)
                                    + "%.");
                } else {
                    displaySimpleAlertDialog("Channel Information", authorName + " has no videos.");
                }
            } else {
                displaySimpleAlertDialog("Network Connection Failure",
                        "Please check your network connection and try again.");
            }
            mProgressDialog.dismiss();
        }
    }
}
