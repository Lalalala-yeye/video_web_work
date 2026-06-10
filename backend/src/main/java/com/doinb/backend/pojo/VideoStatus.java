package com.doinb.backend.pojo;

/** 视频状态：0待审核 1已发布 2举报待复审 3仅自己可见 */
public final class VideoStatus {
    public static final int PENDING = 0;
    public static final int PUBLISHED = 1;
    public static final int REPORT_REVIEW = 2;
    public static final int PRIVATE = 3;

    /** 举报达到该次数后进入复审 */
    public static final int REPORT_THRESHOLD = 3;

    private VideoStatus() {
    }
}
