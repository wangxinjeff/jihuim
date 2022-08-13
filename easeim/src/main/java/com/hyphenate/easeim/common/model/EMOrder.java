package com.hyphenate.easeim.common.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class EMOrder implements Parcelable {
    private String id;
    private String type;
    private String typeName;
    private String goodsName;
    private String date;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public EMOrder(){}

    protected EMOrder(Parcel in) {
        id = in.readString();
        type = in.readString();
        typeName = in.readString();
        goodsName = in.readString();
        date = in.readString();
    }

    public static final Creator<EMOrder> CREATOR = new Creator<EMOrder>() {
        @Override
        public EMOrder createFromParcel(Parcel in) {
            return new EMOrder(in);
        }

        @Override
        public EMOrder[] newArray(int size) {
            return new EMOrder[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        if(TextUtils.equals("MAIN", type)){
            typeName = "维保订单";
        } else if(TextUtils.equals("PICKCAR", type)){
            typeName = "取送订单";
        } else if(TextUtils.equals("FINE", type)){
            typeName = "精品订单";
        } else if(TextUtils.equals("PACKAGE", type)){
            typeName = "服务订单";
        }
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(type);
        dest.writeString(typeName);
        dest.writeString(goodsName);
        dest.writeString(date);
    }
}
