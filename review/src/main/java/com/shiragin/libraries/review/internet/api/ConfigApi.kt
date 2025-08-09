package com.shiragin.libraries.review.internet.api

import com.shiragin.libraries.review.internet.model.IpInfo
import com.shiragin.libraries.review.internet.model.Review
import com.shiragin.libraries.review.internet.model.ServerConfig
import kotlinx.serialization.InternalSerializationApi
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

internal interface ConfigApi {

    @POST
    suspend fun leaveComment(
        @Url url: String = "https://wiadevelopers.com/api/insert_data.php",
        @Body review: Review
    ): ResponseBody

    @GET
    suspend fun getIp(@Url url: String = "http://ip-api.com/json/"): IpInfo

    @GET
    suspend fun getServerConfig(@Url url: String): ServerConfig
}