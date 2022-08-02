package com.hyphenate.easeim.common.model;

import android.os.Parcel;
import android.os.Parcelable;

public class EMOrder implements Parcelable {
    private String id;
    private String type;
    private String name;
    private String date;

    public EMOrder(){}

    protected EMOrder(Parcel in) {
        id = in.readString();
        type = in.readString();
        name = in.readString();
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
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        dest.writeString(name);
        dest.writeString(date);
    }
}
