package application.greyhats.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;

    static ArrayList<String> titles = new ArrayList<String>();
    static ArrayList<Integer> newsId = new ArrayList<Integer>();
    static ArrayList<String> newsUrl = new ArrayList<String>();

    static ArrayAdapter arrayAdapter;

    SQLiteDatabase sqLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sqLiteDatabase = this.openOrCreateDatabase("News", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS  Articles (id INTEGER PRIMARY KEY, title VARCHAR, url VARCHAR)");

        listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        updateListView();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent myIntent = new Intent(getApplicationContext(), NewsActivity.class);
                myIntent.putExtra("url", newsUrl.get(position));
                startActivity(myIntent);
            }
        });

        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

    }

    public void updateListView () {

        Log.i("updateListVIew", "called");

        Cursor c = sqLiteDatabase.rawQuery("SELECT * FROM Articles ", null);
        int titleIndex = c.getColumnIndex("title");
        int urlIndex = c.getColumnIndex("url");
        Log.i("title index", Integer.toString(titleIndex));

        if (c.moveToFirst()) {
            titles.clear();
            newsUrl.clear();

            do {
                titles.add(c.getString(titleIndex));
                newsUrl.add(c.getString(urlIndex));
                Log.i("db", c.getString(titleIndex) + c.getString(urlIndex));
                arrayAdapter.notifyDataSetChanged();
            } while ( c.moveToNext());
        }
    }

    public class DownloadTask extends AsyncTask <String, Void, String> {

        URL url;
        HttpURLConnection urlConnection = null;
        String result = "";

        @Override
        protected String doInBackground(String... urls) {
            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);

                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                Log.i("result", result);

                JSONArray newsIdjson = new JSONArray(result);

                int max_length = 40;
                if (newsIdjson.length() < 40 ) {
                    max_length = newsIdjson.length();
                }

                sqLiteDatabase.execSQL("DELETE FROM Articles");

                for ( int i = 0 ; i < max_length; i++ ) {
                    String id = newsIdjson.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+id+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);

                    String sql = "INSERT INTO Articles (title, url) VALUES (?, ?)";


                    String newResult = "";

                    int newData = reader.read();

                    while (newData != -1) {
                        char current = (char) newData;
                        newResult += current;
                        newData = reader.read();
                    }

                    Log.i("newResult", newResult);

                    JSONObject jsonObject = new JSONObject(newResult);

                    if ( !jsonObject.isNull("title") &&  !jsonObject.isNull("url")) {
                        SQLiteStatement statement = sqLiteDatabase.compileStatement(sql);
                        statement.bindString(1, jsonObject.getString("title"));
                        statement.bindString(2, jsonObject.getString("url"));
                        statement.execute();
                        Log.i("sql", String.valueOf(statement));
                    }
                }

                return result;

            } catch ( Exception e ) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}