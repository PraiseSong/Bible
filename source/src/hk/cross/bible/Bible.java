package hk.cross.bible;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.StrictMode;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class Bible extends Activity {
	private final String HOST = "http://cross.hk/wp-admin/admin-ajax.php?";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bible);
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads()
        .detectDiskWrites()
        .detectNetwork()   
        .penaltyLog()
        .build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
		.detectLeakedSqlLiteObjects() //探测SQLite数据库操作
		.penaltyLog() //打印logcat
		.penaltyDeath()
		.build());
		
		getBookTitle();
	}
	
	private void getBookTitle(){
		String bookTitleQuery = "action=query_bookTitle";
		try{
			JSONArray bookTitle = Json.getJSONArray(HOST+bookTitleQuery);
		}catch(JSONException e){
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
