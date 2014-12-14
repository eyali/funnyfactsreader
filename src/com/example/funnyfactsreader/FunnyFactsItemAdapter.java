package com.example.funnyfactsreader;

import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * FunnyFactsItemAdapter
 * 
 * Manages the model for the ListView of the MainActivity.
 * 
 * @author eyali
 * 
 */
public class FunnyFactsItemAdapter extends ArrayAdapter<FunnyFactsItem> {

    protected static final String TAG = "FunnyFactsItemAdapter";
    protected static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    protected MainActivity m_activity = null;
    private ConcurrentLinkedQueue<Pair<ImageView, FunnyFactsItem>> m_downloads = null;
    private Thread m_downloadThread = null;
    public HttpGet m_getRequest = null;

    private SwipeGestureListener m_swipeGestureListener = null;

    /**
     * FunnyFactsItemAdapter
     * 
     * @param activity
     *            - MainActivity that hosts the adapter which provides context
     *            and call to refresh the data
     * @param resource
     *            - required by superclass ArrayAdapter but is ignored
     */
    public FunnyFactsItemAdapter(MainActivity activity, int resource) {
        super(activity, resource);
        m_activity = activity;
        m_swipeGestureListener = new SwipeGestureListener(m_activity, new OnSwipeGesture() {

            @Override
            public void onSwipe(View swipedView) {
                refreshImage(swipedView);
            }
        }, new OnSwipeGesture() {

            @Override
            public void onSwipe(View swipedView) {
                m_activity.refresh();
            }
        });
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        FunnyFactsItem item = getItem(position);
        final View itemView;
        if (convertView != null) {
            itemView = convertView;
        } else {
            itemView = inflater.inflate(R.layout.list_item, parent, false);
        }
        final TextView title = (TextView) itemView.findViewById(R.id.funny_facts_item_title);
        final TextView description = (TextView) itemView.findViewById(R.id.funny_facts_item_description);
        final ImageView image = (ImageView) itemView.findViewById(R.id.funny_facts_item_image);

        itemView.setTag(image);
        itemView.setOnTouchListener(m_swipeGestureListener);

        if (item.title != null) {
            title.setText(item.title);
            title.setVisibility(View.VISIBLE);
        } else {
            title.setText("");
            title.setVisibility(View.GONE);
        }

        if (item.description != null) {
            description.setText(item.description);
            description.setVisibility(View.VISIBLE);
        } else {
            description.setText("");
            description.setVisibility(View.GONE);
        }

        image.setTag(item);
        if (item.image == null) {
            loadImage(image, item);
            image.setVisibility(View.GONE);
        } else {
            image.setImageBitmap(item.image);
            image.setVisibility(View.VISIBLE);
        }

        return itemView;
    }

    /**
     * refreshImage - clear and reload the image into the ImageView of the given
     * ListView item view
     * 
     * @param swipedView
     *            - the containing ListView item view
     */
    protected void refreshImage(View itemView) {
        ImageView image = (ImageView) itemView.getTag();
        FunnyFactsItem item = (FunnyFactsItem) image.getTag();
        item.imageRequested = false;
        if (item.image != null) {
            item.image.recycle();
        }
        item.image = null;
        image.setImageBitmap(item.image);
        image.setVisibility(View.GONE);
        loadImage(image, item);
    }

    /**
     * loadImage - load the ImageView with the image referred by the
     * FunnyFactsItem
     * 
     * @param view
     *            - the ImageView to load the image into
     * @param item
     *            - FunnyFactsItem with the image data to load
     */
    private void loadImage(ImageView view, FunnyFactsItem item) {
        // if image already requested do nothing
        if (item.imageRequested) {
            return;
        }

        // set item as requested
        item.imageRequested = true;

        // if item Url is null then do nothing
        if (item.imageUrl == null) {
            return;
        }

        // create queue of download requests
        if (m_downloads == null) {
            m_downloads = new ConcurrentLinkedQueue<Pair<ImageView, FunnyFactsItem>>();
        }

        // add download request to queue
        m_downloads.add(new Pair<ImageView, FunnyFactsItem>(view, item));
        if (m_downloadThread == null) {
            m_downloadThread = new Thread("Downloader") {

                @Override
                public void run() {
                    final AndroidHttpClient client = AndroidHttpClient.newInstance("FunnyFactsReader", m_activity);
                    Pair<ImageView, FunnyFactsItem> entry = null;

                    while (true) {
                        entry = m_downloads.poll();
                        if (entry != null) {
                            try {
                                final ImageView view = entry.first;
                                final FunnyFactsItem item = entry.second;

                                // create GET request for image Url
                                m_getRequest = new HttpGet(item.imageUrl);

                                // set connection timeout as default behaviour
                                // is too slow to respond to errors
                                HttpParams params = m_getRequest.getParams();
                                HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);

                                // perform request
                                HttpResponse response = client.execute(m_getRequest);

                                // check returned status code
                                final int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode != HttpStatus.SC_OK) {
                                    continue;
                                }

                                // check returned body
                                final HttpEntity entity = response.getEntity();
                                if (entity != null) {
                                    InputStream inputStream = null;
                                    try {
                                        // getting contents from the stream
                                        inputStream = entity.getContent();

                                        // decoding stream data into image
                                        // Bitmap
                                        item.image = BitmapFactory.decodeStream(inputStream);

                                        // updating target ImageView ensuring it
                                        // was not recycled
                                        if (((FunnyFactsItem) view.getTag()).imageUrl.equals(item.imageUrl)) {
                                            m_activity.runOnUiThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    view.setImageBitmap(item.image);
                                                    view.setVisibility(View.VISIBLE);
                                                    view.invalidate();
                                                }
                                            });
                                        }
                                    } finally {
                                        if (inputStream != null) {
                                            inputStream.close();
                                        }
                                        entity.consumeContent();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                m_getRequest.abort();
                            }
                        }
                    }
                };
            };
        }

        // execute download thread if not running already
        if (!m_downloadThread.isAlive()) {
            m_downloadThread.start();
        }
    }

    @Override
    public void clear() {
        // stop download thread if running
        if (m_downloadThread != null) {
            m_downloadThread.interrupt();
        }

        // clear any pending downloads
        if (m_downloads != null) {
            m_downloads.clear();
        }

        // abort any current download
        if (m_getRequest != null) {
            m_getRequest.abort();
        }
        super.clear();
    }
}
