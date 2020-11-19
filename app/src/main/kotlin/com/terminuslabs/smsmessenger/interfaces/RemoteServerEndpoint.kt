package com.terminuslabs.smsmessenger.interfaces

import com.terminuslabs.smsmessenger.models.RemoteModel
import retrofit2.Call
import retrofit2.http.*


interface RemoteServerEndpoint {


    @POST
    fun syncMessageStates(
        @Url fullUrl : String,
        @Header("Authorization") authorization: String?,
        @Body body: List<RemoteModel.RequestMessageInfo>
    ): Call<List<RemoteModel.ResponseMessage>>


}
