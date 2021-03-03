package com.abhijith.datasyncing.requests;

import androidx.lifecycle.LiveData;

import com.abhijith.datasyncing.requests.responses.ApiResponse;
import com.abhijith.datasyncing.requests.responses.RecipeSearchResponse;

import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RecipeApi {
    @GET("api/search")
    LiveData<ApiResponse<RecipeSearchResponse>> searchRecipe(
            @Query("key") String key,
            @Query("q") String query,
            @Query("page") String page
    );
}
