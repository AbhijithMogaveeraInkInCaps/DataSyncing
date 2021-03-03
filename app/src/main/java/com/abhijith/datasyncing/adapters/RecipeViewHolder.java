package com.abhijith.datasyncing.adapters;


import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.abhijith.datasyncing.R;
import com.abhijith.datasyncing.models.Recipe;

public class RecipeViewHolder extends RecyclerView.ViewHolder {

    TextView title, publisher, socialScore;
    AppCompatImageView image;
    RequestManager requestManager;
    ViewPreloadSizeProvider viewPreloadSizeProvider;

    public RecipeViewHolder(
            @NonNull View itemView,
            RequestManager requestManager,
            ViewPreloadSizeProvider preloadSizeProvider
    ) {
        super(itemView);

        this.requestManager = requestManager;
        this.viewPreloadSizeProvider = preloadSizeProvider;

        title = itemView.findViewById(R.id.recipe_title);
        publisher = itemView.findViewById(R.id.recipe_publisher);
        socialScore = itemView.findViewById(R.id.recipe_social_score);
        image = itemView.findViewById(R.id.recipe_image);
    }

    public void onBind(Recipe recipe){

        requestManager
                .load(recipe.getImage_url())
                .into(image);

        title.setText(recipe.getTitle());
        publisher.setText(recipe.getPublisher());
        socialScore.setText(String.valueOf(Math.round(recipe.getSocial_rank())));

        viewPreloadSizeProvider.setView(image);
    }
}





