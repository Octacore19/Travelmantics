package com.chegsmania.travelmantics;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.chegsmania.travelmantics.model.TravelDeal;
import com.chegsmania.travelmantics.utils.FirebaseUtils;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class DealAdapter extends RecyclerView.Adapter<DealAdapter.DealViewHolder> {

    private ArrayList<TravelDeal> deals;

    DealAdapter(){
        DatabaseReference mDatabaseReference = FirebaseUtils.mDatabaseReference;
        deals = FirebaseUtils.deals;
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                TravelDeal deal = dataSnapshot.getValue(TravelDeal.class);
                if (deal != null) {
                    deal.setDealId(dataSnapshot.getKey());
                    deals.add(deal);
                    notifyItemInserted(deals.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mDatabaseReference.addChildEventListener(listener);
    }

    @NonNull
    @Override
    public DealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View itemView = LayoutInflater.from(context).inflate(R.layout.deals_list_item, parent, false);
        return new DealViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DealViewHolder holder, int position) {
        TravelDeal deal = deals.get(position);
        holder.bind(deal);
    }

    @Override
    public int getItemCount() {
        return deals.size();
    }

    class DealViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private MaterialTextView dealTitleText, dealPriceText, dealDescriptionText;
        private ImageView imageView;

        DealViewHolder(@NonNull View itemView) {
            super(itemView);
            dealTitleText = itemView.findViewById(R.id.deal_title);
            dealPriceText = itemView.findViewById(R.id.deal_price);
            dealDescriptionText = itemView.findViewById(R.id.deal_description);
            imageView = itemView.findViewById(R.id.deal_image);
            itemView.setOnClickListener(this);
        }
        void bind(TravelDeal deal){
            dealTitleText.setText(deal.getTitle());
            dealPriceText.setText(deal.getPrice());
            dealDescriptionText.setText(deal.getDescription());
            showImage(deal.getImageUrl());
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            TravelDeal deal = deals.get(position);
            Log.d("Click", String.valueOf(position));
            Intent intent = new Intent(view.getContext(), DealActivity.class);
            intent.putExtra("Deal", deal);
            view.getContext().startActivity(intent);
        }

        private void showImage(String url) {
            Picasso.get()
                    .load(url)
                    .into(imageView);

        }
    }
}
