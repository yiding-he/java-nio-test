package com.hyd.niocomm;

import com.alibaba.fastjson.JSONObject;

/**
 * @author yidin
 */
public class Request {

    private long sequence;

    private String path;

    private JSONObject params;

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
}
