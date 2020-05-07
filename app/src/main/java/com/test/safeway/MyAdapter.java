package com.test.safeway;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private List<SearchResults> searchResults;
    private View.OnClickListener OnItemClickListener;

    public TextView textViewName;
    public TextView textViewAddress;

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView textViewName;
        public TextView textViewAddress;


        public ViewHolder(View itemView) {
            super(itemView);
            textViewName = (TextView) itemView.findViewById(R.id.name);
            textViewAddress = (TextView) itemView.findViewById(R.id.address);

            itemView.setTag(this);
            itemView.setOnClickListener(OnItemClickListener);
        }
    }

    public MyAdapter(List<SearchResults> searchResults) {
        this.searchResults = searchResults;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyAdapter.ViewHolder holder, int position) {
        SearchResults searchResult = searchResults.get(position);

        holder.textViewName.setText(searchResult.getname());
        holder.textViewAddress.setText(searchResult.getaddress());
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public void setOnItemClickListener(View.OnClickListener itemClickListener) {
        OnItemClickListener = itemClickListener;
    }

    public void update(List<SearchResults> searchResults){
        this.searchResults = searchResults;
        notifyDataSetChanged();
    }

}

