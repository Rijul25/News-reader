package com.example.dell.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles=new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase ArticleDb;
    ArrayList<String> content=new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.listView);
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });
        ArticleDb=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        ArticleDb.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,content VARCHAR)");
        updateListView();

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");//ab kriyo
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateListView();
    }

    public void updateListView(){
        Cursor c=ArticleDb.rawQuery("SELECT * FROM articles",null);
        int contentindex=c.getColumnIndex("content");
        int titleindex=c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            content.clear();
            do{
                titles.add(c.getString(titleindex));
                content.add(c.getString(contentindex));
                Log.i("titleindex",Integer.toString(titleindex)); //ye log pe print ku nhi ho rhe?
                Log.i("contentindex",Integer.toString(contentindex));
            }while(c.moveToNext());

           arrayAdapter.notifyDataSetChanged();

        }


    }
    public class DownloadTask extends AsyncTask<String,Void,String>{


        @Override
        protected String doInBackground(String... strings) {
            String result="";
            URL url;
            HttpURLConnection urlConnection=null;
            try {
                url=new URL(strings[0]);
                urlConnection= (HttpURLConnection) url.openConnection();
                InputStream inputStream=urlConnection.getInputStream();
                InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
                int data=inputStreamReader.read();
                while(data!=-1){
                    char current= (char) data;
                    result+=current;
                    data=inputStreamReader.read();
                }
                JSONArray jsonArray=new JSONArray(result);
                int numberofitems=20;
                if(jsonArray.length()<=20){
                    numberofitems=jsonArray.length();

                }
                ArticleDb.execSQL("DELETE FROM articles ");
                for(int i=0;i<numberofitems;i++) {
                    String ArticleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + ArticleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    data = inputStreamReader.read();
                    String ArtcileInfo = "";
                    while (data != -1) {
                        char current = (char) data;
                        ArtcileInfo += current;
                        data = inputStreamReader.read();

                    }
                    JSONObject jsonObject = new JSONObject(ArtcileInfo);
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String ArticleTitle = jsonObject.getString("title");
                        String ArticleUrl = jsonObject.getString("url");
                        url = new URL(ArticleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String ArtcileContent = "";
                        while (data != -1) {
                            char current = (char) data;
                            ArtcileContent += current;
                            data = inputStreamReader.read();

                        }
                        Log.i("ArticleContent",ArtcileContent);//yha tk toh av thik kaam kr rha hai
                        String sql="INSERT INTO articles(articleId,title,content) VALUES(?, ?, ?)";
                        SQLiteStatement statement=ArticleDb.compileStatement(sql);
                        statement.bindString(1,ArticleId);
                        statement.bindString(2,ArticleTitle);
                        statement.bindString(3,ArtcileContent); //hmm isko acces kaha krrha hai ? chala? nahi bhai blank screen :(
                        statement.execute();
                    }
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
            Log.i("updated",":)");
        }
    }

}
