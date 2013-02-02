package hk.cross.bible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class Bible extends Activity {
	//接口地址
	private final String HOST = "http://cross.hk/wp-admin/admin-ajax.php?";
	
	private static final String TAG = "圣经";
	
	private ProgressDialog progressinitLoading;
	
	private JSONArray bookTitle;
	
	private int tomeID = 0;//当前圣经书卷的id
	private int chapterID = 0;//当前的章数
	private int sectionFromID = 0;//当前开始节数
	private int sectionToID = 0;//当前结束节数
	
	private boolean initQuery = false;//初始化查询是否完成 
	
	private QueryTask queryTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_bible);
		
		queryTask = new QueryTask(Bible.this);
		queryTask.execute(0);
		
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
		.penaltyLog() 
		.penaltyDeath()
		.build());	
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
			
			OnItemSelectedListener listener = new OnItemSelectedListener(){
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
			tomeList.setOnItemSelectedListener(listener);
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
			
			OnItemSelectedListener listener = new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					chapterID = (int) Integer.parseInt(parent.getItemAtPosition(position).toString());
					processSectionNum();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}};
			chapter.setOnItemSelectedListener(listener);
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
						if(initQuery){
							queryBible();
						}
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
		if(sectionFromID > sectionToID){
			return;
		}
		
		String queryBible = "action=query_bible&article="+chapterID+"&id="+tomeID+"&verse_start="+sectionFromID+"&verse_stop="+sectionToID+"";
		try{
			String bible = (Json.getRequest(HOST+queryBible)).trim();
			bible = android.text.Html.fromHtml(bible).toString();
			TextView bibleBox = (TextView) findViewById(R.id.bibleBox);
			bibleBox.setText(bible);
			
			initQuery = true;
			queryTask.dialog.dismiss();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void loading(){
		setProgressBarIndeterminateVisibility(true);
	}
   
	class QueryTask extends AsyncTask<Integer, Integer, SimpleAdapter>{
		private Activity activity;
		private ProgressDialog dialog;
		private Context context;

		public QueryTask(Activity activity){
			Log.d(TAG,"开始一个新的查询任务");
			this.activity = activity;
			context = activity;
			dialog = new ProgressDialog(context);
		}
		
		protected void onPreExecute(){
			Log.d(TAG,"onPreExecute");
			this.dialog.setTitle(TAG);
			this.dialog.setMessage("查询中...");
			if(!this.dialog.isShowing()){
				this.dialog.show();
			}
		}
		
		protected void onPostExecute(SimpleAdapter adapter){
			super.onPostExecute(adapter);
			Log.d(TAG,"onPostExecute");
			
			Spinner tomeList = (Spinner) findViewById(R.id.tome);
			tomeList.setAdapter(adapter);
			
			OnItemSelectedListener listener = new OnItemSelectedListener(){
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
			tomeList.setOnItemSelectedListener(listener);
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress){
			Log.d(TAG,"onProgressUpdate"+progress[0]);
		}

		@Override
		protected SimpleAdapter doInBackground(Integer... params) {
			Log.d(TAG,"doInBackground");
			publishProgress(params);
			
			String bookTitleQuery = "action=query_bookTitle";
			
			try{
				bookTitle = Json.getJSONArray(HOST+bookTitleQuery);
				List<JSONObject> tome = jsonArray2ListJSONObject(bookTitle);
				List<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
				for(int i=0; i<tome.size(); i++){
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("name", tome.get(i).getString("BookTitle"));
					map.put("id", tome.get(i).getString("Book"));
					data.add(map);
				}
				SimpleAdapter adapter = new SimpleAdapter(Bible.this, data, R.layout.list_item, new String[] {
						"id", "name"
				}, new int[] {
						R.id.id,
						R.id.name
				});
				return adapter;
			}catch(JSONException e){
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
			}
			return null;
		}
	}
}
