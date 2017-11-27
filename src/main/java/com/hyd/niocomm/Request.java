package com.hyd.niocomm;

import com.alibaba.fastjson.JSONObject;

/**
 * @author yidin
 */
public class Request {

    private long sequence;

    private String path;

    private JSONObject params = new JSONObject();

    public Request(String path) {
        this.path = path;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public JSONObject getParams() {
        return params;
    }

    public void setParams(JSONObject params) {
        this.params = params;
    }

    public Request setParam(String paramName, Object paramValue) {
        this.params.put(paramName, paramValue);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParam(String paramName) {
        return (T) this.params.get(paramName);
    }
}
