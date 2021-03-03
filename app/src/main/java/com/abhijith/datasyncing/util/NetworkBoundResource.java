package com.abhijith.datasyncing.util;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.abhijith.datasyncing.AppExecutors;
import com.abhijith.datasyncing.requests.responses.ApiResponse;
import com.abhijith.datasyncing.requests.responses.ApiResponse.ApiErrorResponse;
import com.abhijith.datasyncing.requests.responses.ApiResponse.ApiSuccessResponse;

// CacheObject: Type for the Resource data. (database cache)
// RequestObject: Type for the API response. (network request)
public abstract class NetworkBoundResource<CacheObject, RequestObject> {

    private static final String TAG = "NetworkBoundResource";
    private final AppExecutors appExecutors;
    private final MediatorLiveData<Resource<CacheObject>> resourceMediatorLiveData = new MediatorLiveData<>();

    public NetworkBoundResource(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        init();
    }

    private void init(){
        resourceMediatorLiveData.setValue(Resource.loading(null));
        final LiveData<CacheObject> dbSource = loadFromDb();
        resourceMediatorLiveData.addSource(dbSource, cacheObject -> {
            resourceMediatorLiveData.removeSource(dbSource);
            if(shouldFetch(cacheObject)){
                fetchFromNetwork(dbSource);
            }
            else{
                resourceMediatorLiveData.addSource(dbSource, cacheObject1 -> setValue(Resource.success(cacheObject1)));
            }
        });
    }

    private void fetchFromNetwork(final LiveData<CacheObject> dbSource){

        Log.d(TAG, "fetchFromNetwork: called.");

        // update LiveData for loading status
        resourceMediatorLiveData.addSource(dbSource, cacheObject -> setValue(Resource.loading(cacheObject)));

        final LiveData<ApiResponse<RequestObject>> apiResponse = createCall();

        resourceMediatorLiveData.addSource(apiResponse, requestObjectApiResponse -> {
            getResult(dbSource, apiResponse, requestObjectApiResponse);
        });
    }

    private void getResult(
            LiveData<CacheObject> dbSource,
            LiveData<ApiResponse<RequestObject>> apiResponse,
            ApiResponse<RequestObject> requestObjectApiResponse
    ) {
        resourceMediatorLiveData.removeSource(dbSource);
        resourceMediatorLiveData.removeSource(apiResponse);
            /*
                3 cases:
                   1) ApiSuccessResponse
                   2) ApiErrorResponse
                   3) ApiEmptyResponse
             */

        if(requestObjectApiResponse instanceof ApiResponse.ApiSuccessResponse)
        {
            Log.d(TAG, "onChanged: ApiSuccessResponse.");

            appExecutors.diskIO().execute(new Runnable() {
                @Override
                public void run() {

                    // save the response to the local db
                    saveCallResult((RequestObject) processResponse((ApiSuccessResponse)requestObjectApiResponse));

                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            resourceMediatorLiveData.addSource(loadFromDb(), new Observer<CacheObject>() {
                                @Override
                                public void onChanged(@Nullable CacheObject cacheObject) {
                                    setValue(Resource.success(cacheObject));
                                }
                            });
                        }
                    });
                }
            });
        }
        else if(requestObjectApiResponse instanceof ApiResponse.ApiEmptyResponse)
        {
            Log.d(TAG, "onChanged: ApiEmptyResponse");
            appExecutors.mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    resourceMediatorLiveData.addSource(loadFromDb(), cacheObject -> setValue(Resource.success(cacheObject)));
                }
            });
        }
        else if(requestObjectApiResponse instanceof ApiResponse.ApiErrorResponse)
        {
            Log.d(TAG, "onChanged: ApiErrorResponse.");
            resourceMediatorLiveData.addSource(dbSource, new Observer<CacheObject>() {
                @Override
                public void onChanged(@Nullable CacheObject cacheObject) {
                    setValue(Resource.error(((ApiErrorResponse) requestObjectApiResponse).getErrorMessage(), cacheObject));
                }
            });
        }
    }

    private CacheObject processResponse(
            ApiSuccessResponse response
    ){
        return (CacheObject) response.getBody();
    }

    private void setValue(Resource<CacheObject> newValue){
        if(resourceMediatorLiveData.getValue() != newValue){
            resourceMediatorLiveData.setValue(newValue);
        }
    }

    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestObject item);

    @MainThread
    protected abstract boolean shouldFetch(@Nullable CacheObject data);

    @NonNull @MainThread
    protected abstract LiveData<CacheObject> loadFromDb();

    @NonNull
    @MainThread
    protected abstract LiveData<ApiResponse<RequestObject>> createCall();

    public final LiveData<Resource<CacheObject>> getAsLiveData(){
        return resourceMediatorLiveData;
    };
}




