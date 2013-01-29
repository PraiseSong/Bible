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
import android.R.integer;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class Bible extends Activity {
	private final String HOST = "http://cross.hk/wp-admin/admin-ajax.php?";
	
	private static final String TAG = "Bible";
	
	private JSONArray bookTitle;
	private String chapterNum = "";
	
	private int tomeID = 0;//当前圣经书卷的id
	private int chapterID = 0;//当前的章数
	private int sectionFromID = 0;//当前开始节数
	private int sectionToID = 0;//当前结束节数
	
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
	
	/**
	 * 处理圣经书卷
	 */
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
					try {
						JSONObject item = new JSONObject(parent.getItemAtPosition(position).toString());
						int itemId = item.getInt("id");
						String itemId2 = null;
						if(itemId < 10){
							itemId2 = "0"+String.valueOf(itemId);
						}else{
							itemId2 = ""+itemId;
						}
						
						tomeID = Integer.parseInt(itemId2);
						
						processChapterNum();
					} catch (JSONException e) {
						e.printStackTrace();
					}
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
	
	/**
	 * 将一个json array转换为List对象
	 * @param jsonData
	 * @return List<JSONObject>
	 * @throws JSONException
	 */
	private List<JSONObject> jsonArray2ListJSONObject(JSONArray jsonData) throws JSONException{
		List<JSONObject> data = new ArrayList<JSONObject>();
		for(int i=0; i<jsonData.length(); i++){
			JSONObject item = jsonData.getJSONObject(i);;
			data.add(item);
		}
		return data;
	}
	
	private void processChapterNum(){
		String chapterQuery = "action=query_article_num&id="+tomeID;
		try{
			int chapterNum = Integer.parseInt(Json.getRequest(HOST+chapterQuery).trim());
			Integer[] chapterNumList = new Integer[chapterNum];
			for(int i=0; i<chapterNum; i++){
				chapterNumList[i] = i+1;
			}
			Spinner chapter = (Spinner) findViewById(R.id.chapterNum);
			ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(Bible.this, android.R.layout.simple_list_item_1, chapterNumList);
			chapter.setAdapter(adapter);
			
			OnItemSelectedListener oisl = new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					chapterID = (int) Integer.parseInt(parent.getItemAtPosition(position).toString());
					processSectionNum();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}};
			chapter.setOnItemSelectedListener(oisl);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void processSectionNum(){
		String sectionQuery = "action=query_verse_num&article="+chapterID+"&id="+tomeID+"";
		try{
			int sectionNum = Integer.parseInt(Json.getRequest(HOST+sectionQuery).trim());
			Integer[] sectionList = new Integer[sectionNum];
			for(int i=0; i<sectionNum; i++){
				sectionList[i] = i+1;
			}
			Spinner sectionFrom = (Spinner) findViewById(R.id.sectionFrom);
			Spinner sectionTo = (Spinner) findViewById(R.id.sectionTo);
			ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(Bible.this, android.R.layout.simple_list_item_1, sectionList);
			sectionFrom.setAdapter(adapter);
			sectionTo.setAdapter(adapter);
			
			OnItemSelectedListener listenerTo = new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					sectionToID = (int) Integer.parseInt(parent.getItemAtPosition(position).toString());
					queryBible();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}};
			OnItemSelectedListener listenerFrom = new OnItemSelectedListener(){
					@Override
					public void onItemSelected(AdapterView<?> parent, View view,
							int position, long id) {
						sectionFromID = (int) Integer.parseInt(parent.getItemAtPosition(position).toString());
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}};
			sectionFrom.setOnItemSelectedListener(listenerFrom);
			sectionTo.setOnItemSelectedListener(listenerTo);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void queryBible(){
		String queryBible = "action=query_bible&article="+chapterID+"&id="+tomeID+"&verse_start="+sectionFromID+"&verse_stop="+sectionToID+"";
		try{
			String bible = (Json.getRequest(HOST+queryBible)).trim();
			bible = android.text.Html.fromHtml(bible).toString();
			TextView bibleBox = (TextView) findViewById(R.id.bibleBox);
			bibleBox.setText(bible);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
