package com.abhijith.datasyncing.viewmodels;


import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.abhijith.datasyncing.models.Recipe;
import com.abhijith.datasyncing.repositories.RecipeRepository;
import com.abhijith.datasyncing.util.Resource;

import java.util.List;

public class RecipeListViewModel extends AndroidViewModel {

    public static final String QUERY_EXHAUSTED = "No more results.";
    public enum ViewState {
        RECIPES
    }

    private MutableLiveData<ViewState> viewState;
    private MediatorLiveData<Resource<List<Recipe>>> recipes = new MediatorLiveData<>();
    private RecipeRepository recipeRepository;

    // query extras
    private boolean isQueryExhausted;
    private boolean isPerformingQuery;

    private int pageNumber;
    private String query;

    private boolean cancelRequest;

    private long requestStartTime;

    public RecipeListViewModel(@NonNull Application application) {
        super(application);
        recipeRepository = RecipeRepository.getInstance(application);
        init();
    }

    private void init(){
        if(viewState == null){
            viewState = new MutableLiveData<>();
            viewState.setValue(ViewState.RECIPES);
        }
    }

    //Cat or recipes of cat
    public LiveData<ViewState> getViewState(){
        return viewState;
    }

    public LiveData<Resource<List<Recipe>>> getRecipes(){
        return recipes;
    }

    public int getPageNumber(){
        return pageNumber;
    }

    public void searchRecipesApi(String query, int pageNumber){
        if(!isPerformingQuery){
            if(pageNumber == 0){
                pageNumber = 1;
            }
            this.pageNumber = pageNumber;
            this.query = query;
            isQueryExhausted = false;
            executeSearch();
        }
    }

    public void searchNextPage(){
        if(!isQueryExhausted && !isPerformingQuery){
            pageNumber++;
            executeSearch();
        }
    }

    private void executeSearch(){

        requestStartTime = System.currentTimeMillis();

        cancelRequest = false;

        isPerformingQuery = true;

        viewState.setValue(ViewState.RECIPES);

        final LiveData<Resource<List<Recipe>>> repositorySource = recipeRepository.searchRecipesApi(query, pageNumber);

        recipes.addSource(repositorySource, listResource -> {
            if(!cancelRequest){
                if(listResource != null){
                    if(listResource.status == Resource.Status.SUCCESS){
                       isPerformingQuery = false;
                        if(listResource.data != null){
                            if(listResource.data.size() == 0 ){
                                recipes.setValue(
                                        new Resource<>(
                                                Resource.Status.ERROR,
                                                listResource.data,
                                                QUERY_EXHAUSTED
                                        )
                                );
                                isQueryExhausted = true;
                            }
                        }
                        recipes.removeSource(repositorySource);
                    }
                    else if(listResource.status == Resource.Status.ERROR){
                        isPerformingQuery = false;
                        if(listResource.message.equals(QUERY_EXHAUSTED)){
                            isQueryExhausted = true;
                        }
                        recipes.removeSource(repositorySource);
                    }
                    recipes.setValue(listResource);
                }
                else{
                    recipes.removeSource(repositorySource);
                }
            }
            else{
                recipes.removeSource(repositorySource);
            }
        });
    }
}