package com.hdpolover.ybbproject.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
        "Content-Type:application/json",
        "Authorization:key=AAAAHCfZbMc:APA91bGG35D3gpXz5SNzjsMsWl7gGWrSgIPiqL19VLw7jmy4_5Id6lztrGymqwnpiJiVyZ_-KBZxMbjKMrjrq1ZdcBNCe-04QEH_0t1RvO5JYIC8ExZzJFC3nC4QtptiY4Rzx45F3eDE"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
