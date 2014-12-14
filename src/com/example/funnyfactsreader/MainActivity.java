package com.example.funnyfactsreader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * MainActivity
 * 
 * The main Activity which displays the content of the Json file: - title in the
 * ActionBar - rows in a ListView
 * 
 * @author eyali
 * 
 */
public class MainActivity extends ListActivity {

    protected static final String TAG = "FunnyFactsReader";

    private FunnyFactsItemAdapter m_adapter = null;
    private AlertDialog.Builder m_errorDialogBuilder = null;
    private AsyncTask<Void, Void, Integer> m_task = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_adapter = new FunnyFactsItemAdapter(this, R.layout.list_item);
        setListAdapter(m_adapter);
        m_errorDialogBuilder = new AlertDialog.Builder(this);
        fetchJson();
    }

    /**
     * fetchJson - fetch the Json file and process it to provide the model for
     * the FunnyFactsItemAdapter
     */
    public void fetchJson() {
        // if there is already a task running cancel it
        if (m_task != null) {
            m_task.cancel(true);
        }
        // create and execute new fetching task
        m_task = new AsyncTask<Void, Void, Integer>() {

            @Override
            protected void onPreExecute() {
                Log.d(TAG, "fetchJson: onPreExecute");
            }

            private String nextNullOrString(JsonReader newsJsonReader) throws IOException {
                String value = null;
                try {
                    newsJsonReader.nextNull();
                } catch (IllegalStateException e) {
                    value = newsJsonReader.nextString();
                }
                return value;
            }

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    final URL url = new URL(getString(R.string.json_url));
                    JsonReader jsonReader = null;
                    try {
                        InputStreamReader content = new InputStreamReader(url.openStream(), "ISO-8859-1");
                        if (content != null) {
                            // start reading content
                            jsonReader = new JsonReader(content);
                            jsonReader.beginObject();
                            // read title field
                            if (jsonReader.nextName().equals("title")) {
                                final String title = jsonReader.nextString();
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        // set ActionBar title to read string
                                        getActionBar().setTitle(title);
                                    }
                                });
                                // read rows field as array
                                if (jsonReader.nextName().equals("rows")) {
                                    jsonReader.beginArray();
                                    do {
                                        // read next row object
                                        try {
                                            jsonReader.beginObject();
                                        } catch (IllegalStateException e) {
                                            // no more row objects leave loop
                                            break;
                                        }
                                        final FunnyFactsItem item = new FunnyFactsItem();
                                        // read title field. May be null!
                                        if (jsonReader.nextName().equals("title")) {
                                            item.title = nextNullOrString(jsonReader);
                                            // read description field. May be
                                            // null!
                                            if (jsonReader.nextName().equals("description")) {
                                                item.description = nextNullOrString(jsonReader);
                                                // read imageHref field. May be
                                                // null!
                                                if (jsonReader.nextName().equals("imageHref")) {
                                                    item.imageUrl = nextNullOrString(jsonReader);
                                                }
                                            }
                                        }
                                        jsonReader.endObject();
                                        // add non empty items to the model and
                                        // notify renderer
                                        if (item.title != null || item.description != null || item.imageUrl != null) {
                                            runOnUiThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    m_adapter.add(item);
                                                    m_adapter.notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    } while (true);
                                    jsonReader.endArray();
                                }
                            }
                            jsonReader.endObject();
                        }
                    } catch (UnknownHostException uhe) {
                        return R.string.no_server;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return R.string.no_data;
                    } finally {
                        if (jsonReader != null) {
                            try {
                                jsonReader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Integer result) {
                Log.d(TAG, "fetchJson: onPostExecute");
                if (result != null) {
                    m_errorDialogBuilder.setTitle(result.intValue()).setNeutralButton(R.string.action_refresh, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            refresh();
                        }
                    });
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            m_errorDialogBuilder.show();
                            getListView().getEmptyView().setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }
        }.execute();
    }

    /**
     * refresh - clear and re-fetch the data
     */
    public void refresh() {
        m_adapter.clear();
        m_adapter.notifyDataSetChanged();
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                getListView().getEmptyView().setVisibility(View.VISIBLE);
                fetchJson();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            refresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
