package com.hyphenate.easeim.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.support.v4.content.ContextCompat;

import com.hyphenate.easeim.R;
import com.hyphenate.easeui.widget.EaseImageView;

public class ArrowItemView extends ConstraintLayout {
    private EaseImageView avatar;
    private TextView tvTitle;
    private TextView tvContent;
    private ImageView ivArrow;
    private View viewDivider;
    private String title;
    private String content;
    private int titleColor;
    private int contentColor;
    private float titleSize;
    private float contentSize;
    private View root;
    private TextView belowContent;
    private String bContent;
    private int bContentColor;

    public ArrowItemView(Context context) {
        this(context, null);
    }

    public ArrowItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArrowItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs) {
        root = LayoutInflater.from(context).inflate(R.layout.demo_layout_item_arrow, this);
        avatar = findViewById(R.id.avatar);
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        ivArrow = findViewById(R.id.iv_arrow);
        viewDivider = findViewById(R.id.view_divider);
        belowContent = findViewById(R.id.tv_below_content);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArrowItemView);
        int titleResourceId = a.getResourceId(R.styleable.ArrowItemView_arrowItemTitle, -1);
        title = a.getString(R.styleable.ArrowItemView_arrowItemTitle);
        if(titleResourceId != -1) {
            title = getContext().getString(titleResourceId);
        }
        tvTitle.setText(title);

        int titleColorId = a.getResourceId(R.styleable.ArrowItemView_arrowItemTitleColor, -1);
        titleColor = a.getColor(R.styleable.ArrowItemView_arrowItemTitleColor, ContextCompat.getColor(getContext(), R.color.normal_text_color));
        if(titleColorId != -1) {
            titleColor = ContextCompat.getColor(getContext(), titleColorId);
        }
        tvTitle.setTextColor(titleColor);

        int titleSizeId = a.getResourceId(R.styleable.ArrowItemView_arrowItemTitleSize, -1);
        titleSize = a.getDimension(R.styleable.ArrowItemView_arrowItemTitleSize, sp2px(getContext(), 14));
        if(titleSizeId != -1) {
            titleSize = getResources().getDimension(titleSizeId);
        }
        tvTitle.getPaint().setTextSize(titleSize);

        int contentResourceId = a.getResourceId(R.styleable.ArrowItemView_arrowItemContent, -1);
        content = a.getString(R.styleable.ArrowItemView_arrowItemContent);
        if(contentResourceId != -1) {
            content = getContext().getString(contentResourceId);
        }
        tvContent.setText(content);

        int contentColorId = a.getResourceId(R.styleable.ArrowItemView_arrowItemContentColor, -1);
        contentColor = a.getColor(R.styleable.ArrowItemView_arrowItemContentColor, ContextCompat.getColor(getContext(), R.color.normal_text_color));
        if(contentColorId != -1) {
            contentColor = ContextCompat.getColor(getContext(), contentColorId);
        }
        tvContent.setTextColor(contentColor);

        int contentSizeId = a.getResourceId(R.styleable.ArrowItemView_arrowItemContentSize, -1);
        contentSize = a.getDimension(R.styleable.ArrowItemView_arrowItemContentSize, 14);
        if(contentSizeId != -1) {
            contentSize = getResources().getDimension(contentSizeId);
        }
        tvContent.setTextSize(contentSize);

        int bContentResourceId = a.getResourceId(R.styleable.ArrowItemView_arrowItemBContent, -1);
        bContent = a.getString(R.styleable.ArrowItemView_arrowItemBContent);
        if(bContentResourceId != -1) {
            bContent = getContext().getString(bContentResourceId);
        }
        belowContent.setText(bContent);

        int bContentColorId = a.getResourceId(R.styleable.ArrowItemView_arrowItemBContentColor, -1);
        bContentColor = a.getColor(R.styleable.ArrowItemView_arrowItemBContentColor, ContextCompat.getColor(getContext(), R.color.group_detail_mid_color));
        if(bContentColorId != -1) {
            bContentColor = ContextCompat.getColor(getContext(), bContentColorId);
        }
        belowContent.setTextColor(bContentColor);

        boolean showBContent = a.getBoolean(R.styleable.ArrowItemView_arrowItemBContentShow, false);
        belowContent.setVisibility(showBContent ? VISIBLE : GONE);

        boolean showDivider = a.getBoolean(R.styleable.ArrowItemView_arrowItemShowDivider, true);
        viewDivider.setVisibility(showDivider ? VISIBLE : GONE);

        boolean showArrow = a.getBoolean(R.styleable.ArrowItemView_arrowItemShowArrow, true);
        ivArrow.setVisibility(showArrow ? VISIBLE : GONE);

        boolean showAvatar = a.getBoolean(R.styleable.ArrowItemView_arrowItemShowAvatar, false);
        avatar.setVisibility(showAvatar ? VISIBLE : GONE);

        int avatarSrcResourceId = a.getResourceId(R.styleable.ArrowItemView_arrowItemAvatarSrc, -1);
        if(avatarSrcResourceId != -1) {
            avatar.setImageResource(avatarSrcResourceId);
        }

        int avatarHeightId = a.getResourceId(R.styleable.ArrowItemView_arrowItemAvatarHeight, -1);
        float height = a.getDimension(R.styleable.ArrowItemView_arrowItemAvatarHeight, 0);
        if(avatarHeightId != -1) {
            height = getResources().getDimension(avatarHeightId);
        }

        int avatarWidthId = a.getResourceId(R.styleable.ArrowItemView_arrowItemAvatarWidth, -1);
        float width = a.getDimension(R.styleable.ArrowItemView_arrowItemAvatarWidth, 0);
        if(avatarWidthId != -1) {
            width = getResources().getDimension(avatarWidthId);
        }

        a.recycle();

        ViewGroup.LayoutParams params = avatar.getLayoutParams();
        params.height = height == 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : (int)height;
        params.width = width == 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : (int)width;
    }

    public TextView getTvContent() {
        return tvContent;
    }

    public TextView getTvTitle() {
        return tvTitle;
    }

    public EaseImageView getAvatar() { return avatar; }

    public void setItemShowArrow(boolean show){
        ivArrow.setVisibility(show ? VISIBLE : GONE);
    }

    public void setItemShowDivider(boolean show){
        viewDivider.setVisibility(show ? VISIBLE : GONE);
    }

    public TextView getTvBContent() {
        return belowContent;
    }
    /**
     * sp to px
     * @param context
     * @param value
     * @return
     */
    public static float sp2px(Context context, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context.getResources().getDisplayMetrics());
    }
}
