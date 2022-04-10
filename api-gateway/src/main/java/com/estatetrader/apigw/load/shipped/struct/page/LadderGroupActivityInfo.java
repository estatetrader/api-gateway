package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("LadderGroupActivityInfo")
public class LadderGroupActivityInfo implements ActivityInfo, Serializable {
    @Description("activityId")
    public int activityId;
    @Description("depositStartTime")
    public long depositStartTime;
    @Description("depositEndTime")
    public long depositEndTime;

    public LadderGroupActivityInfo(int activityId, long depositStartTime, long depositEndTime) {
        this.activityId = activityId;
        this.depositStartTime = depositStartTime;
        this.depositEndTime = depositEndTime;
    }
}
