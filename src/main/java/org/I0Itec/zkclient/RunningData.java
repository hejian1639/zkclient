package org.I0Itec.zkclient;

import java.io.Serializable;

/**
 * Created by nevermore on 16/6/22.
 */
public class RunningData implements Serializable {

    private static final long serialVersionUID = 4260577459043203630L;


    //������id
    private long cid;
    //����������
    private String name;


    public long getCid() {
        return cid;
    }

    public void setCid(long cid) {
        this.cid = cid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}