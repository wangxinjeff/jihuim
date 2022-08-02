package com.hyphenate.easeim.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.hyphenate.easeim.R;
import com.hyphenate.easeui.utils.EaseCommonUtils;

public class SearchBar extends LinearLayout implements View.OnClickListener {

    private Context mContext;

    private RelativeLayout rootView;
    private AppCompatEditText searchView;
    private AppCompatImageView searchEmpty;
    private AppCompatTextView searchClose;
    private AppCompatTextView searchStart;
    private AppCompatTextView searchTextView;
    private LinearLayout searchIconView;
    private AppCompatTextView searchShow;
    private LinearLayout inputView;

    private OnSearchBarListener listener;

    private boolean showSearchText;

    private int searchBgColor;
    private int inputBgColor;
    private int inputTextColor;

    public SearchBar(Context context) {
        this(context, null);

    }

    public SearchBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public SearchBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);

    }

    public SearchBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        if(attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SearchBar, defStyleAttr, 0);
            searchBgColor = typedArray.getColor(R.styleable.SearchBar_searchBarBgColor, 0);
            inputBgColor = typedArray.getColor(R.styleable.SearchBar_inputBgColor, 0);
            inputTextColor = typedArray.getColor(R.styleable.SearchBar_inputContentColor, 0);
        }

    }

    public void init(boolean showSearchText){
        this.showSearchText = showSearchText;
        setLayout();
        initView();
        initListener();
    }

    private void setLayout(){
        LayoutInflater.from(mContext).inflate(R.layout.ease_layout_search, this);
    }

    private void initView(){
        rootView = findViewById(R.id.root);
        searchView = findViewById(R.id.search_et_view);
        searchEmpty = findViewById(R.id.search_empty);
        searchClose = findViewById(R.id.search_close);
        searchStart = findViewById(R.id.search_start);
        searchTextView = findViewById(R.id.search_tv_view);
        searchIconView = findViewById(R.id.search_icon_view);
        searchShow = findViewById(R.id.search_show);
        inputView = findViewById(R.id.input_view);
    }

    private void initListener(){
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
                    if(listener != null){
                        listener.onSearchContent(searchView.getText().toString());
                    }
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
                if(showSearchText){
                    if(TextUtils.isEmpty(s.toString())){
                        searchStart.setVisibility(GONE);
                        searchClose.setVisibility(VISIBLE);
                    }else {
                        searchStart.setVisibility(VISIBLE);
                        searchClose.setVisibility(GONE);
                    }
                } else {
                    searchClose.setVisibility(VISIBLE);
                    searchStart.setVisibility(GONE);
                    if(listener != null){
                        listener.onSearchContent(s.toString());
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
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
            if (showSearchText) {
                if (listener != null) {
                    listener.onSearchContent(searchView.getText().toString());
                }
            }
        } else if (id == R.id.search_close) {
            searchView.setText("");
            searchTextView.setVisibility(View.VISIBLE);
            searchIconView.setVisibility(View.VISIBLE);
            EaseCommonUtils.hideSoftKeyBoard(searchView);
            searchClose.setVisibility(View.VISIBLE);
            searchStart.setVisibility(View.GONE);
        }
    }

    public void setOnSearchBarListener(OnSearchBarListener listener){
        this.listener = listener;
    }

    public interface OnSearchBarListener{
        void onSearchContent(String text);
    }

    public void setSearchBarBgColor(int color){
        rootView.setBackgroundColor(color);
    }
    public void setInputViewDrawable(Drawable drawable){
        inputView.setBackground(drawable);
        searchTextView.setBackgroundDrawable(drawable);
    }
    public void setInputTextColor(int color){
        searchView.setTextColor(color);
        searchShow.setTextColor(color);
    }
}
