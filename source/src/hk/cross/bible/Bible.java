package hk.cross.bible;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

public class Bible extends Activity {
	private final String HOST = "http://cross.hk/wp-admin/admin-ajax.php?";
	
	private static final String TAG = "Bible";
	
	private JSONArray bookTitle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bible);
		
		/**
		 * 解决android NetworkOnMainThreadException报错
		 */
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
		
		processBookTitle();
	}
	
	private void processBookTitle(){
		String bookTitleQuery = "action=query_bookTitle";
		try{
			bookTitle = Json.getJSONArray(HOST+bookTitleQuery);
			List<JSONObject> tome = jsonArray2ListJSONObject(bookTitle);
			List<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
			Spinner tomeList = (Spinner) findViewById(R.id.tome);
			for(int i=0; i<tome.size(); i++){
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("name", tome.get(i).getString("BookTitle"));
				map.put("id", tome.get(i).getString("Book"));
				data.add(map);
			}
			SimpleAdapter _Adapter = new SimpleAdapter(Bible.this, data, R.layout.list_item, new String[] {
					"id", "name"
			}, new int[] {
					R.id.id,
					R.id.name
			});
			tomeList.setAdapter(_Adapter);
			
			OnItemSelectedListener oisl = new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					Log.d(TAG,parent.getItemAtPosition(position).toString());
					Log.d(TAG,""+id);
					Log.d(TAG,""+position);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}};
			tomeList.setOnItemSelectedListener(oisl);
		}catch(JSONException e){
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private List<JSONObject> jsonArray2ListJSONObject(JSONArray jsonData) throws JSONException{
		List<JSONObject> data = new ArrayList<JSONObject>();
		for(int i=0; i<jsonData.length(); i++){
			JSONObject item = jsonData.getJSONObject(i);;
			data.add(item);
		}
		return data;
	}
}
