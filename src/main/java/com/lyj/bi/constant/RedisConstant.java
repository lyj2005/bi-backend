package com.lyj.bi.constant;

public interface RedisConstant {
    String CHART_LIST_CACHE_KEY = "MoonBI:chartlist";
    //缓存时间为30分钟
    Long CHART_LIST_CACHE_TIME = 30L;
    String CHART_CHCHE_ID="MoonBI:Chart:id:";
    Long CHART_CACHE_TIME = 30L;
    String CHART_LIST_CHCHE_KEY="MoonBI:chartlist:page";

    //用户签到记录的Redis Key
    String USER_SIGN_IN_KEY = "MoonBI:user:sign:in";

    /**
     * 获取用户签到记录的Redis Key
     * @param month
     * @param userId
     * @return
     */
    static String getUserSignInKey(int month,long userId){
        return String.format("%s:%s:%s",USER_SIGN_IN_KEY,month,userId);
    }
}
