package com.huewu.pla.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.dodola.model.DuitangInfo;
import com.dodowaterfall.Helper;
import com.dodowaterfall.widget.ScaleImageView;
import com.example.android.bitmapfun.util.ImageCache.ImageCacheParams;
import com.example.android.bitmapfun.util.ImageFetcher;
import com.huewu.pla.lib.internal.PLA_AdapterView;

public class PullToRefreshSampleActivity extends FragmentActivity {
	private ImageFetcher mImageFetcher;
	private PLA_AdapterView<ListAdapter> mAdapterView = null;
	private StaggeredAdapter mAdapter = null;
	private static final String IMAGE_CACHE_DIR = "thumbs";
	ContentTask task = new ContentTask(this);

	private class ContentTask extends AsyncTask<String, Integer, List<DuitangInfo>> {

		private Context mContext;

		public ContentTask(Context context) {
			super();
			mContext = context;
		}

		@Override
		protected List<DuitangInfo> doInBackground(String... params) {
			try {
				return parseNewsJSON(params[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<DuitangInfo> result) {
			mAdapter.addItem(result);
			mAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onPreExecute() {
		}

		public List<DuitangInfo> parseNewsJSON(String url) throws IOException {
			List<DuitangInfo> duitangs = new ArrayList<DuitangInfo>();
			String json = "";
			if (Helper.checkConnection(mContext)) {
				try {
					json = Helper.getStringFromUrl(url);

				} catch (IOException e) {
					Log.e("IOException is : ", e.toString());
					e.printStackTrace();
					return duitangs;
				}
			}
			Log.d("MainActiivty", "json:" + json);

			try {
				if (null != json) {
					JSONObject newsObject = new JSONObject(json);
					JSONObject jsonObject = newsObject.getJSONObject("data");
					JSONArray blogsJson = jsonObject.getJSONArray("blogs");

					for (int i = 0; i < blogsJson.length(); i++) {
						JSONObject newsInfoLeftObject = blogsJson.getJSONObject(i);
						DuitangInfo newsInfo1 = new DuitangInfo();
						newsInfo1.setAlbid(newsInfoLeftObject.isNull("albid") ? "" : newsInfoLeftObject.getString("albid"));
						newsInfo1.setIsrc(newsInfoLeftObject.isNull("isrc") ? "" : newsInfoLeftObject.getString("isrc"));
						newsInfo1.setMsg(newsInfoLeftObject.isNull("msg") ? "" : newsInfoLeftObject.getString("msg"));
						newsInfo1.setHeight(newsInfoLeftObject.getInt("iht"));
						duitangs.add(newsInfo1);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return duitangs;
		}
	}

	private void AddItemToContainer(int pageindex) {
		if (task.getStatus() != Status.RUNNING) {

			String url = "http://www.duitang.com/album/1733789/masn/p/" + pageindex + "/24/";
			Log.d("MainActivity", "current url:" + url);
			ContentTask task = new ContentTask(this);
			task.execute(url);

		}
	}

	public class StaggeredAdapter extends BaseAdapter {
		private Context mContext;
		private LinkedList<DuitangInfo> mInfos;

		public StaggeredAdapter(Context context) {
			mContext = context;
			mInfos = new LinkedList<DuitangInfo>();

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;
			DuitangInfo duitangInfo = mInfos.get(position);

			if (convertView == null) {
				LayoutInflater layoutInflator = LayoutInflater.from(parent.getContext());
				convertView = layoutInflator.inflate(R.layout.infos_list, null);
				holder = new ViewHolder();
				holder.imageView = (ImageView) convertView.findViewById(R.id.news_pic);
				holder.contentView = (TextView) convertView.findViewById(R.id.news_title);
				convertView.setTag(holder);
			}

			holder = (ViewHolder) convertView.getTag();
			double height = duitangInfo.getWidth() / 240.0 * duitangInfo.getHeight();
			holder.imageView.setLayoutParams(new LinearLayout.LayoutParams(240, (int) height));
			holder.contentView.setText(duitangInfo.getMsg());
			mImageFetcher.loadImage(duitangInfo.getIsrc(), holder.imageView);
			return convertView;
		}

		class ViewHolder {
			ImageView imageView;
			TextView contentView;
			TextView timeView;
		}

		@Override
		public int getCount() {
			return mInfos.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mInfos.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		public void addItem(List<DuitangInfo> datas) {
			mInfos.addAll(datas);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_pull_to_refresh_sample);
		mAdapter = new StaggeredAdapter(this);
		mAdapterView = (PLA_AdapterView<ListAdapter>) findViewById(R.id.list);

		ImageCacheParams cacheParams = new ImageCacheParams(this, IMAGE_CACHE_DIR);

		cacheParams.setMemCacheSizePercent(0.25f);
		mImageFetcher = new ImageFetcher(this, 100);
		mImageFetcher.setLoadingImage(R.drawable.empty_photo);
		mImageFetcher.addImageCache(this.getSupportFragmentManager(), cacheParams);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 1001, 0, "Load More Contents");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mImageFetcher.setExitTasksEarly(false);
		mAdapterView.setAdapter(mAdapter);
		AddItemToContainer(0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();

	}

	private Random mRand = new Random();

}// end of class
