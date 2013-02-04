package hk.cross.bible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
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
	
	private JSONArray bookTitle;
	
	private String tome = "";//当前书卷的名称
	private int tomeID = 0;//当前圣经书卷的id
	private int chapterID = 0;//当前的章数
	private int sectionFromID = 0;//当前开始节数
	private int sectionToID = 0;//当前结束节数
	
	private QueryTome queryTome;
	private QueryChapterNum queryChapterNum;
	private QuerySectionNum querySectionNum;
	private QueryBibleDetail queryBibleDetail;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_bible);
		
		queryTome = new QueryTome(Bible.this);
		queryTome.execute(0);
		
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
   
	/**
	 * 查询所有圣经书卷
	 * @author qizhuq
	 */
	class QueryTome extends AsyncTask<Integer, Integer, SimpleAdapter>{
		private Activity activity;
		private ProgressDialog dialog;
		private Context context;
		private String logpreFix = "查询书卷";

		public QueryTome(Activity activity){
			Log.d(TAG,"开始查询圣经书卷");
			this.activity = activity;
			context = activity;
			dialog = new ProgressDialog(context);
		}
		
		protected void onPreExecute(){
			Log.d(TAG,"onPreExecute");
			this.dialog.setTitle("查询所有"+TAG+"书卷");
			if(!this.dialog.isShowing()){
				this.dialog.show();
			}
		}
		
		protected void onPostExecute(SimpleAdapter adapter){
			super.onPostExecute(adapter);
			Log.d(TAG,"onPostExecute");
			
			Spinner tomeList = (Spinner) findViewById(R.id.tome);
			tomeList.setAdapter(adapter);
			
			final ProgressDialog dialog = this.dialog;
			
			OnItemSelectedListener listener = new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					try {
						JSONObject item = new JSONObject(parent.getItemAtPosition(position).toString());
						int itemId = item.getInt("id");
						tome = item.getString("name");
						String itemId2 = null;
						if(itemId < 10){
							itemId2 = "0"+String.valueOf(itemId);
						}else{
							itemId2 = ""+itemId;
						}
						
						tomeID = Integer.parseInt(itemId2);
						dialog.dismiss();
						
						queryChapterNum = (QueryChapterNum) new QueryChapterNum(Bible.this).execute(0);
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
    
	/**
	 * 查询当前书卷下的所有章数
	 * @author qizhuq
	 */
	class QueryChapterNum extends AsyncTask<Integer, Integer, ArrayAdapter>{
		private Activity activity;
		private ProgressDialog dialog;
		private Context context;
		private String logpreFix = "查询章数";
		
		@Override
		protected ArrayAdapter doInBackground(Integer... params) {
			Log.d(TAG,logpreFix+"doInBackground");
			publishProgress(params[0]);
			String chapterQuery = "action=query_article_num&id="+tomeID;
			
			try{
				int chapterNum = Integer.parseInt(Json.getRequest(HOST+chapterQuery).trim());
				Integer[] chapterNumList = new Integer[chapterNum];
				for(int i=0; i<chapterNum; i++){
					chapterNumList[i] = i+1;
				}
				ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(Bible.this, android.R.layout.simple_list_item_1, chapterNumList);
				return adapter;
			}catch(Exception e){
				e.printStackTrace();
			}
			return null;
		}
		
		public QueryChapterNum(Activity activity){
			Log.d(TAG,"开始查询 "+tome+" 章数");
			this.activity = activity;
			this.context = activity;
			this.dialog = new ProgressDialog(activity);
		}
		
		@Override
		protected void onPreExecute(){
			Log.d(TAG,logpreFix+"onPreExecute");
			this.dialog.setTitle("查询 "+tome+" 所有章数");
			if(!this.dialog.isShowing()){
				this.dialog.show();
			}
		}
		
		@Override
		
		protected void onPostExecute(ArrayAdapter adapter){
			Log.d(TAG,"章数onPostExecute");
			Spinner chapter = (Spinner) findViewById(R.id.chapterNum);
			chapter.setAdapter(adapter);
			
			final ProgressDialog dialog = this.dialog;
			
			OnItemSelectedListener listener = new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					chapterID = (int) Integer.parseInt(parent.getItemAtPosition(position).toString());
					dialog.dismiss();
					querySectionNum = (QuerySectionNum) new  QuerySectionNum(Bible.this).execute(0);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}};
			chapter.setOnItemSelectedListener(listener);
		}
	}

   /**
    * 查询当前书卷下，当前章数下的所有节数
    */
	class QuerySectionNum extends AsyncTask<Integer, Integer, ArrayAdapter>{
		private Activity activity;
		private ProgressDialog dialog;
		private Context context;
		private String logpreFix = "查询节数";
		
		public QuerySectionNum(Activity activity){
			Log.d(TAG,"开始查询第 "+chapterID+" 章的所有节数");
			this.activity = activity;
			this.context = activity;
			this.dialog = new ProgressDialog(activity);
		}
		
		@Override
		protected void onPreExecute(){
			this.dialog.setTitle("查询 "+tome+" 第 "+chapterID+" 章的节数");
			if(!this.dialog.isShowing()){
				this.dialog.show();
			}
		}
		
		@Override
		protected ArrayAdapter doInBackground(Integer... params) {
			Log.d(TAG,logpreFix+"doInBackground");
			
			publishProgress(params[0]);
			String sectionQuery = "action=query_verse_num&article="+chapterID+"&id="+tomeID+"";
			try{
				int sectionNum = Integer.parseInt(Json.getRequest(HOST+sectionQuery).trim());
				Integer[] sectionList = new Integer[sectionNum];
				for(int i=0; i<sectionNum; i++){
					sectionList[i] = i+1;
				}
				
				ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(Bible.this, android.R.layout.simple_list_item_1, sectionList);
			    return adapter;
			}catch(Exception e){
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(ArrayAdapter adapter){
			Log.d(TAG,logpreFix+"onPostExecute");
			
			Spinner sectionFrom = (Spinner) findViewById(R.id.sectionFrom);
			Spinner sectionTo = (Spinner) findViewById(R.id.sectionTo);
			sectionFrom.setAdapter(adapter);
			sectionTo.setAdapter(adapter);
			
			final ProgressDialog dialog = this.dialog;
			
			OnItemSelectedListener listenerTo = new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					sectionToID = (int) Integer.parseInt(parent.getItemAtPosition(position).toString());
					dialog.dismiss();
					queryBibleDetail = (QueryBibleDetail) new QueryBibleDetail(Bible.this).execute(0);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}};
			OnItemSelectedListener listenerFrom = new OnItemSelectedListener(){
					@Override
					public void onItemSelected(AdapterView<?> parent, View view,
							int position, long id) {
						sectionFromID = (int) Integer.parseInt(parent.getItemAtPosition(position).toString());
						dialog.dismiss();
						queryBibleDetail = (QueryBibleDetail) new QueryBibleDetail(Bible.this).execute(0);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}};
			sectionFrom.setOnItemSelectedListener(listenerFrom);
			sectionTo.setOnItemSelectedListener(listenerTo);
		}
	}
   
	/**
	 * 根据书卷，章数，节数查询具体的经文
	 */
	class QueryBibleDetail extends AsyncTask<Integer, Integer, String>{
		private Activity activity;
		private ProgressDialog dialog;
		private Context context;
		private String logpreFix = "查询经文"; 
		
		public QueryBibleDetail(Activity activity){
			this.activity = activity;
			this.context = activity;
			this.dialog = new ProgressDialog(activity);
		}
		
		@Override
		protected void onPreExecute(){
			this.dialog.setTitle(logpreFix);
			this.dialog.setMessage("正在查询："+tome+chapterID+":"+sectionFromID+"-"+sectionToID+" 节");
			if(!this.dialog.isShowing()){
				this.dialog.show();
			}
		}
		
		@Override
		protected String doInBackground(Integer... params) {
			Log.d(TAG,logpreFix+"doInBackground");
			publishProgress(params[0]);
			
			if(sectionFromID > sectionToID){
				return "";
			}
			
			String queryBible = "action=query_bible&article="+chapterID+"&id="+tomeID+"&verse_start="+sectionFromID+"&verse_stop="+sectionToID+"";
			try{
				String bible = (Json.getRequest(HOST+queryBible)).trim();
				bible = android.text.Html.fromHtml(bible).toString();
				return bible;
			}catch(Exception e){
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String bible){
			Log.d(TAG,logpreFix+"onPostExecute");
			TextView bibleBox = (TextView) findViewById(R.id.bibleBox);
			bibleBox.setText(bible);
			this.dialog.dismiss();
		}
	}
}
