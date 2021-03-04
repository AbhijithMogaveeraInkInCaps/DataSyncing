package com.abhijith.datasyncing.repositories;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.abhijith.datasyncing.AppExecutors;
import com.abhijith.datasyncing.models.Recipe;
import com.abhijith.datasyncing.persistence.RecipeDao;
import com.abhijith.datasyncing.persistence.RecipeDatabase;
import com.abhijith.datasyncing.requests.ServiceGenerator;
import com.abhijith.datasyncing.requests.responses.ApiResponse;
import com.abhijith.datasyncing.requests.responses.RecipeSearchResponse;
import com.abhijith.datasyncing.util.Constants;
import com.abhijith.datasyncing.util.NetworkBoundResource;
import com.abhijith.datasyncing.util.Resource;

import java.util.List;

public class RecipeRepository {

    private static final String TAG = "RecipeRepository";

    private static RecipeRepository instance;

    private RecipeDao recipeDao;

    int lastFetch = 0;

    //make singleton
    public static RecipeRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeRepository(context);
        }
        return instance;
    }

    //init local db ref
    private RecipeRepository(Context context) {
        recipeDao = RecipeDatabase.getInstance(context).getRecipeDao();
    }

    public LiveData<Resource<List<Recipe>>> searchRecipesApi(final String query, final int pageNumber) {
        return new NetworkBoundResource<List<Recipe>, RecipeSearchResponse>(AppExecutors.getInstance()) {

            @Override
            protected void saveCallResult(@NonNull RecipeSearchResponse item) {

                if (item.getRecipes() != null) {
                    Recipe[] recipes = new Recipe[item.getRecipes().size()];
                    int index = 0;
                    for (long rowid : recipeDao.insertRecipes((Recipe[]) (item.getRecipes().toArray(recipes)))) {
                        if (rowid == -1) {
                            recipeDao.updateRecipe(
                                    recipes[index].getRecipe_id(),
                                    recipes[index].getTitle(),
                                    recipes[index].getPublisher(),
                                    recipes[index].getImage_url(),
                                    recipes[index].getSocial_rank()
                            );
                        }
                        index++;
                    }
                }
            }


            @Override
            protected boolean shouldFetch(@Nullable List<Recipe> data) {
                //decide if u want to fetch new data or use cached one
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Recipe>> loadFromDb() {
                return recipeDao.searchRecipes(query, pageNumber);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<RecipeSearchResponse>> createCall() {
                return ServiceGenerator.getRecipeApi()
                        .searchRecipe(Constants.API_KEY, query, String.valueOf(pageNumber));
            }
        }.getAsLiveData();
    }

}












