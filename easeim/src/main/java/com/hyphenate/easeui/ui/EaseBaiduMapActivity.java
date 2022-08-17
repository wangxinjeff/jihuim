/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.easeui.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.common.BaiduMapSDKException;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.adapter.EaseBaiduMapAdapter;
import com.hyphenate.easeui.manager.EaseThreadManager;
import com.hyphenate.easeui.ui.base.EaseBaseActivity;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.PositionUtil;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class EaseBaiduMapActivity extends EaseBaseActivity implements EaseTitleBar.OnBackPressListener,
																		EaseTitleBar.OnRightClickListener,
																		OnGetPoiSearchResultListener, View.OnClickListener {
	private final static String TAG = "map";

	private EaseTitleBar titleBarMap;
	private View searchBar;
	private MapView mapView;
	private BaiduMap mBaiduMap;
	private BDLocation lastLocation;
	private UiSettings mUiSettings;

	protected double latitude;
	protected double longitude;
	protected String address;
	private BaiduSDKReceiver mBaiduReceiver;
	private LocationClient mLocClient;
	private RecyclerView mListView;
	private EaseBaiduMapAdapter adapter;
	public BDAbstractLocationListener myListener = new MyLocationListener();
	private List<PoiInfo> nearList = new ArrayList<>();
	private Marker mCurrentMarker;
	private PoiSearch mPoiSearch;

	private double mCurrentLatitude;
	private double mCurrentLongitude;
	private String mCurrentAddress;
	private String mCurrentBuildingName;

	private AppCompatEditText searchView;
	private AppCompatImageView searchEmpty;
	private AppCompatTextView searchClose;
	private AppCompatTextView searchStart;
	private AppCompatTextView searchTextView;
	private AppCompatTextView searchShow;
	private LinearLayout inputView;
	private LinearLayout searchIconView;
	private ConstraintLayout searchResultView;
	private boolean moveToPoi = false;

	public static void actionStartForResult(Fragment fragment, int requestCode) {
		Intent intent = new Intent(fragment.getContext(), EaseBaiduMapActivity.class);
		fragment.startActivityForResult(intent, requestCode);
	}


	public static void actionStart(Context context, double latitude, double longtitude, String address) {
		Intent intent = new Intent(context, EaseBaiduMapActivity.class);
		intent.putExtra("latitude", latitude);
		intent.putExtra("longtitude", longtitude);
		intent.putExtra("address", address);
		context.startActivity(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.AdminTheme);
//		try {
			// 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
			SDKInitializer.initialize(getApplicationContext());
//		} catch (BaiduMapSDKException e) {
//			ToastUtils.showCenterToast("", "百度地图初始化失败", 0, Toast.LENGTH_SHORT);
//			return;
//		}
		setContentView(R.layout.ease_activity_baidumap);
		setFitSystemForTheme(false, R.color.transparent, true);
		initIntent();
		initView();
		initListener();
		initData();
	}

	private void initIntent() {
		latitude = getIntent().getDoubleExtra("latitude", 0);
		longitude = getIntent().getDoubleExtra("longtitude", 0);
		address = getIntent().getStringExtra("address");
		EMLog.e(TAG, "initIntent:" + latitude + ", " + longitude);
	}

	private void initView() {
		titleBarMap = findViewById(R.id.title_bar_map);
		searchBar = findViewById(R.id.search_bar);
		searchView = searchBar.findViewById(R.id.search_et_view);
		searchEmpty = searchBar.findViewById(R.id.search_empty);
		searchClose = searchBar.findViewById(R.id.search_close);
		searchStart = searchBar.findViewById(R.id.search_start);
		searchShow = searchBar.findViewById(R.id.search_show);
		inputView = searchBar.findViewById(R.id.input_view);

		searchTextView = searchBar.findViewById(R.id.search_tv_view);
		searchIconView = searchBar.findViewById(R.id.search_icon_view);

			searchBar.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_float_bg));
			inputView.setBackground(ContextCompat.getDrawable(this, R.drawable.ease_search_bg_admin));
			searchTextView.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.ease_search_bg_admin));

		searchResultView = findViewById(R.id.result_view);


		mapView = findViewById(R.id.bmapView);
		titleBarMap.setRightTitleResource(R.string.button_send);
		mListView = findViewById(R.id.recyclerview);
		mListView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new EaseBaiduMapAdapter();
		mListView.setAdapter(adapter);
		double latitude = getIntent().getDoubleExtra("latitude", 0);
		if(latitude != 0) {
			titleBarMap.getRightLayout().setVisibility(GONE);
		}else {
			titleBarMap.getRightLayout().setVisibility(VISIBLE);
			titleBarMap.getRightLayout().setClickable(false);
		}
		ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) titleBarMap.getLayoutParams();
		params.topMargin = (int) EaseCommonUtils.dip2px(this, 24);
		titleBarMap.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
		titleBarMap.getRightText().setTextColor(ContextCompat.getColor(this, R.color.white));
		titleBarMap.getRightText().setBackgroundResource(R.drawable.ease_title_bar_right_selector);
		int left = (int) EaseCommonUtils.dip2px(this, 10);
		int top = (int) EaseCommonUtils.dip2px(this, 5);
		titleBarMap.getRightText().setPadding(left, top, left, top);
		ViewGroup.LayoutParams layoutParams = titleBarMap.getRightLayout().getLayoutParams();
		if(layoutParams instanceof ViewGroup.MarginLayoutParams) {
		    ((ViewGroup.MarginLayoutParams) layoutParams).setMargins(0, 0, left, 0);
		}

		mBaiduMap = mapView.getMap();
		mUiSettings = mBaiduMap.getUiSettings();
		mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(15.0f));
		mBaiduMap.setViewPadding(10, 10, 10, 10);
//		mUiSettings.setScrollGesturesEnabled(false);
//		mUiSettings.setZoomGesturesEnabled(false);
//		mUiSettings.setRotateGesturesEnabled(false);
//		mUiSettings.setAllGesturesEnabled(false);
//		mUiSettings.setEnlargeCenterWithDoubleClickEnable(true);

		nearList.clear();
	}

	private void initListener() {
		titleBarMap.setOnBackPressListener(this);
		titleBarMap.setOnRightClickListener(this);
		adapter.setOnItemClickListener(new EaseBaiduMapAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(PoiInfo info) {
				Log.e(TAG, info.toString());
				adapter.notifyDataSetChanged();
				if(info == null){
					return;
				}
				moveToPoi(info);
			}
		});

		searchEmpty.setOnClickListener(this);
		searchClose.setOnClickListener(this);
		searchStart.setOnClickListener(this);
		searchTextView.setOnClickListener(this);
		searchIconView.setOnClickListener(this);

		searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_SEARCH ||
						actionId == EditorInfo.IME_ACTION_DONE ||
						event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() &&
								KeyEvent.ACTION_DOWN == event.getAction()){
					moveToPoi = true;
					searchNearBy(searchView.getText().toString());
					return true;
				} else {
					return false;
				}
			}
		});

		searchView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
					if(TextUtils.isEmpty(s.toString())){
						searchStart.setVisibility(GONE);
						searchClose.setVisibility(VISIBLE);
					}else {
						searchStart.setVisibility(VISIBLE);
						searchClose.setVisibility(GONE);
					}
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});
	}

	private void initData() {
		if(latitude == 0) {
			mapView = new MapView(this, new BaiduMapOptions());
			mBaiduMap.setMyLocationConfiguration(
					new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null));
			showMapWithLocationClient();
		}else {
			searchResultView.setVisibility(GONE);
//			LatLng lng = new LatLng(latitude, longitude);
//			mapView = new MapView(this,
//					new BaiduMapOptions().mapStatus(new MapStatus.Builder().target(lng).build()));
			showMap(latitude, longitude);
		}
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
		iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
		mBaiduReceiver = new BaiduSDKReceiver();
		registerReceiver(mBaiduReceiver, iFilter);
		mPoiSearch = PoiSearch.newInstance();
		mPoiSearch.setOnGetPoiSearchResultListener(this);
	}

	protected void showMapWithLocationClient() {
		try {
			mLocClient = new LocationClient(getApplicationContext());
			mLocClient.registerLocationListener(myListener);
		} catch (Exception e) {
			e.printStackTrace();
		}
		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
		option.setCoorType("gcj02");//可选，默认gcj02，设置返回的定位结果坐标系
		//		option.setOnceLocation(true);
		int span = 3000;
		option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
		option.setOpenGps(true);//可选，默认false,设置是否使用gps
		option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
		option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
		option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
		option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
		option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
		if(mLocClient != null){
			mLocClient.setLocOption(option);
			if(!mLocClient.isStarted()) {
				mLocClient.start();
			}
		}
	}

	protected void showMap(double latitude, double longtitude) {
		LatLng lng = new LatLng(latitude, longtitude);
		CoordinateConverter converter = new CoordinateConverter();
		converter.coord(lng);
		converter.from(CoordinateConverter.CoordType.COMMON);
		LatLng convertLatLng = converter.convert();
		OverlayOptions ooA = new MarkerOptions().position(convertLatLng).icon(BitmapDescriptorFactory
				.fromResource(R.drawable.em_icon_map_location))
				.zIndex(4).draggable(true);
		mBaiduMap.addOverlay(ooA);
		MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(convertLatLng, 17.0f);
		mBaiduMap.animateMapStatus(u);
	}

	@Override
	public void onBackPress(View view) {
		onBackPressed();
	}

	@Override
	public void onRightClick(View view) {
		sendLocation();
	}

	private void sendLocation() {
		Intent intent = getIntent();
		intent.putExtra("latitude", mCurrentLatitude);
		intent.putExtra("longitude", mCurrentLongitude);
		intent.putExtra("address", mCurrentAddress);
		intent.putExtra("buildingName", mCurrentBuildingName);
		this.setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected void onResume() {
		mapView.onResume();
		if (mLocClient != null) {
			if(!mLocClient.isStarted()) {
				mLocClient.start();
			}
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		mapView.onPause();
		if (mLocClient != null) {
			mLocClient.stop();
		}
		super.onPause();
		lastLocation = null;
	}

	@Override
	protected void onDestroy() {
		if (mLocClient != null)
			mLocClient.stop();
		mapView.onDestroy();
		unregisterReceiver(mBaiduReceiver);
		super.onDestroy();
	}

	@Override
	public void onGetPoiResult(PoiResult poiResult) {
		if (poiResult.getAllPoi() != null && poiResult.getAllPoi().size() > 0) {
			if(nearList.size() > 1){
				nearList.clear();
			}
			nearList.addAll(poiResult.getAllPoi());
			if (isFinishing()) {
				return;
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					adapter.clearSelect();
					adapter.setData(nearList);
					if(moveToPoi){
						moveToPoi(nearList.get(0));
					}
				}
			});
		} else {
			adapter.clearData();
		}
	}

	private void moveToPoi(PoiInfo info){
		MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(info.location);
		PositionUtil.Gps gps = PositionUtil.bd09_To_Gcj02(info.location.latitude, info.location.longitude);
		mCurrentLatitude = gps.getWgLat();
		mCurrentLongitude = gps.getWgLon();
//		mCurrentLatitude = info.location.latitude;
//		mCurrentLongitude = info.location.longitude;
		mCurrentAddress = info.address;
		mCurrentBuildingName = info.name;
		Log.e(TAG, "moveToPoi:" + mCurrentLatitude + ", " + mCurrentLongitude + ", " + mCurrentAddress + ", " + mCurrentBuildingName);
		mBaiduMap.animateMapStatus(u);
		mCurrentMarker.setPosition(info.location);
	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

	}

	@Override
	public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

	}

	@Override
	public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.search_tv_view || id == R.id.search_icon_view) {
			searchTextView.setVisibility(View.GONE);
			searchIconView.setVisibility(View.GONE);
			EaseCommonUtils.showSoftKeyBoard(searchView);
		} else if (id == R.id.search_empty) {
			searchView.setText("");
		} else if (id == R.id.search_start) {
			moveToPoi = true;
			searchNearBy(searchView.getText().toString());
		} else if (id == R.id.search_close) {
			resetSearchBar();
			searchShow.setText(getString(R.string.search));
		}
	}

	public class BaiduSDKReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(TextUtils.equals(action, SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
				showErrorToast(getResources().getString(R.string.please_check));
			}else if(TextUtils.equals(action, SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
				showErrorToast(getResources().getString(R.string.Network_error));
			}
		}
	}

	/**
	 * show error message
	 * @param message
	 */
	protected void showErrorToast(String message) {
		Toast.makeText(EaseBaiduMapActivity.this, message, Toast.LENGTH_SHORT).show();
	}

	class MyLocationListener extends BDAbstractLocationListener {
		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null || "4.9E-324".equals(location.getLatitude())) {
				return;
			}

			if (lastLocation != null) {
				if (lastLocation.getLatitude() == location.getLatitude() && lastLocation.getLongitude() == location.getLongitude()) {
					Log.d(TAG, "same location, skip refresh");
					return;
				}
			}
			titleBarMap.getRightLayout().setClickable(true);
			lastLocation = location;
			mBaiduMap.clear();
			mCurrentLatitude = lastLocation.getLatitude();
			mCurrentLongitude = lastLocation.getLongitude();
			mCurrentAddress = lastLocation.getAddrStr();
			mCurrentBuildingName = lastLocation.getBuildingName();
			Log.e(TAG, mCurrentLatitude + "," + mCurrentLongitude + "," + mCurrentAddress + "," + mCurrentBuildingName);
			LatLng llA = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
			CoordinateConverter converter = new CoordinateConverter();
			converter.coord(llA);
			converter.from(CoordinateConverter.CoordType.COMMON);
			LatLng convertLatLng = converter.convert();
			OverlayOptions ooA = new MarkerOptions().position(convertLatLng).icon(BitmapDescriptorFactory
					.fromResource(R.drawable.em_icon_map_location))
					.zIndex(4).draggable(true);
			mCurrentMarker = (Marker) mBaiduMap.addOverlay(ooA);
			MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(convertLatLng, 16.0f);
			mBaiduMap.animateMapStatus(u);
			if (lastLocation != null){
				nearList.clear();
				PoiInfo poiInfo = new PoiInfo();
				PositionUtil.Gps gps = PositionUtil.gcj02_To_Bd09(lastLocation.getLatitude(), lastLocation.getLongitude());
				poiInfo.location = new LatLng(gps.getWgLat(), gps.getWgLon());
				poiInfo.name = lastLocation.getAddrStr();
				poiInfo.address = lastLocation.getAddrStr();
				Log.e(TAG, poiInfo.toString());
				nearList.add(poiInfo);
			}
			mLocClient.stop();
			mLocClient.unRegisterLocationListener(this);
			moveToPoi = false;
			searchNearBy("大厦");
		}
	}

	/*
	 * 搜索周边地理位置
	 */
	private void searchNearBy(String key) {
		searchRefreshUI();
		EaseThreadManager.getInstance().runOnIOThread(new Runnable() {
			@Override
			public void run() {
//				PoiNearbySearchOption option = new PoiNearbySearchOption();
//				option.keyword(key);
//				option.sortType(PoiSortType.distance_from_near_to_far);
//				option.location(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
//				option.radius(2000);
//				option.pageCapacity(30);
//				mPoiSearch.searchNearby(option);

				// 搜索城市Poi
				PoiCitySearchOption citySearchOption = new PoiCitySearchOption();
				citySearchOption.city(lastLocation.getCity()).keyword(key).pageNum(0);
				mPoiSearch.searchInCity(citySearchOption);
			}
		});
	}

	private void resetSearchBar(){
		searchView.setText("");
		searchTextView.setVisibility(View.VISIBLE);
		searchIconView.setVisibility(View.VISIBLE);
		EaseCommonUtils.hideSoftKeyBoard(searchView);
		searchClose.setVisibility(View.VISIBLE);
		searchStart.setVisibility(View.GONE);
	}

	public void searchRefreshUI(){
		String content = searchView.getText().toString();
		if(TextUtils.isEmpty(content)){
			searchShow.setText(getString(R.string.search));
		} else {
			searchShow.setText(searchView.getText().toString());
		}
		resetSearchBar();

	}
}
