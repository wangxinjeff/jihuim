package com.hyphenate.easeui.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;

import com.baidu.mapapi.search.core.PoiInfo;
import com.hyphenate.easeim.EaseIMHelper;
import com.hyphenate.easeim.R;

public class EaseBaiduMapAdapter extends EaseBaseRecyclerViewAdapter<PoiInfo>{
    private int selectPosition = 0;
    private OnItemClickListener listener;

    public void clearSelect(){
        selectPosition = 0;
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }

    @Override
    public int getEmptyLayoutId() {
        return EaseIMHelper.getInstance().isAdmin() ? R.layout.ease_layout_no_data_admin : R.layout.ease_layout_no_data;
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent, int viewType) {
        return new EaseMapViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.map_result_item, parent, false));
    }

    class EaseMapViewHolder extends EaseBaseRecyclerViewAdapter.ViewHolder<PoiInfo> {
        private AppCompatTextView name;
        private AppCompatTextView address;
        private AppCompatImageView select;
        private boolean isSelect = false;

        public EaseMapViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void initView(View itemView) {
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
            select = itemView.findViewById(R.id.icon_select);
        }

        @Override
        public void setData(PoiInfo item, int position) {
            if(position == selectPosition){
                select.setVisibility(View.VISIBLE);
            } else {
                select.setVisibility(View.INVISIBLE);
            }
            name.setText(item.name);
            address.setText(item.address);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectPosition = position;
                    if(listener != null){
                        listener.onItemClick(item);
                    }
                }
            });
        }
    }

    public interface OnItemClickListener{
        void onItemClick(PoiInfo info);
    }
}
