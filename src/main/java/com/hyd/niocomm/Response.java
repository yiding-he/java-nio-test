package com.hyd.niocomm;

import com.alibaba.fastjson.JSONObject;

/**
 * @author yidin
 */
public class Response {

    private long sequence;

    private int resultCode;

    private String message;

    private JSONObject data = new JSONObject();

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public Response set(String propName, Object propValue) {
        this.data.put(propName, propValue);
        return this;
    }
}
