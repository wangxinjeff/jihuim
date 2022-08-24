package com.hyphenate.easeim.section.search.adapter;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.v7.widget.AppCompatImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMVideoMessageBody;
import com.hyphenate.easeim.R;
import com.hyphenate.easeui.adapter.EaseBaseRecyclerViewAdapter;
import com.hyphenate.easeui.utils.EaseFileUtils;
import com.hyphenate.easeui.widget.EaseImageView;

public class MultiMediaListAdapter extends EaseBaseRecyclerViewAdapter<EMMessage> {
    @Override
    public int getEmptyLayoutId() {
        return R.layout.ease_layout_no_data_admin;
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent, int viewType) {
        return new SearchAllViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.em_multimedia_item, parent, false));
    }

    private class SearchAllViewHolder extends ViewHolder<EMMessage>{
        private EaseImageView image;
        private Context context;
        private AppCompatImageView iconVideo;

        public SearchAllViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
        }

        @Override
        public void initView(View itemView) {
            image = findViewById(R.id.image);
            iconVideo = findViewById(R.id.icon_video);
        }

        @Override
        public void setData(EMMessage item, int position) {
            if(item.getType() == EMMessage.Type.IMAGE){
                iconVideo.setVisibility(View.GONE);
                EMImageMessageBody body = (EMImageMessageBody) item.getBody();
                Uri imageUri = body.getLocalUri();
                // 获取Uri的读权限
                EaseFileUtils.takePersistableUriPermission(context, imageUri);
                if(!EaseFileUtils.isFileExistByUri(context, imageUri)) {
                    imageUri = ((EMImageMessageBody) body).thumbnailLocalUri();
                    EaseFileUtils.takePersistableUriPermission(context, imageUri);
                    if(!EaseFileUtils.isFileExistByUri(context, imageUri)) {
                        imageUri = null;
                    }
                }
                //获取图片服务器地址
                String thumbnailUrl = null;
                thumbnailUrl = ((EMImageMessageBody) body).getThumbnailUrl();
                if(TextUtils.isEmpty(thumbnailUrl)) {
                    thumbnailUrl = ((EMImageMessageBody) body).getRemoteUrl();
                }
                RequestOptions options = new RequestOptions().error(R.drawable.ease_default_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL);
                Glide.with(context)
                        .load(imageUri == null ? thumbnailUrl : imageUri)
                        .apply(options)
                        .into(image);
            } else if(item.getType() == EMMessage.Type.VIDEO){
                iconVideo.setVisibility(View.VISIBLE);
                EMVideoMessageBody body = (EMVideoMessageBody) item.getBody();
                //获取视频封面本地资源路径
                Uri localThumbUri = ((EMVideoMessageBody) body).getLocalThumbUri();
                //检查Uri读权限
                EaseFileUtils.takePersistableUriPermission(context, localThumbUri);
                //获取视频封面服务器地址
                String thumbnailUrl = ((EMVideoMessageBody) body).getThumbnailUrl();
                if(!EaseFileUtils.isFileExistByUri(context, localThumbUri)) {
                    localThumbUri = null;
                }
                RequestOptions options = new RequestOptions().error(R.drawable.ease_default_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL);

                Glide.with(context).load(localThumbUri == null ? thumbnailUrl : localThumbUri).apply(options).into(image);
            }
        }
    }
}
