package org.grameen.fdp.adapter;

/**
 * Created by aangjnr on 30/04/2018.
 */

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;


import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import org.grameen.fdp.R;
import org.grameen.fdp.object.RealFarmer;

import java.util.ArrayList;

/**
 * Created by mancj on 27.01.17.
 */

public class CustomSuggestionsAdapter extends SuggestionsAdapter<RealFarmer, CustomSuggestionsAdapter.SuggestionHolder> {

    static CustomSuggestionsAdapter.OnItemClickListener mItemClickListener;


    public CustomSuggestionsAdapter(LayoutInflater inflater) {
        super(inflater);
    }

    @Override
    public int getSingleViewHeight() {
        return 80;
    }

    @Override
    public SuggestionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = getLayoutInflater().inflate(R.layout.item_custom_suggestion, parent, false);
        return new SuggestionHolder(view);
    }

    @Override
    public void onBindSuggestionHolder(RealFarmer suggestion, SuggestionHolder holder, int position) {
        holder.title.setText(suggestion.getFarmerName());
        holder.code.setText(suggestion.getCode());

        //holder.itemView.setTag(suggestion.getId());


    }

    @Override
    public Filter getFilter() {


        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                String term = constraint.toString();
                if (term.isEmpty())
                    suggestions = suggestions_clone;
                else {
                    suggestions = new ArrayList<>();
                    for (RealFarmer item : suggestions_clone)
                        if (item.getFarmerName().toLowerCase().contains(term.toLowerCase()))
                            suggestions.add(item);
                }
                results.values = suggestions;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                suggestions = (ArrayList<RealFarmer>) results.values;
                notifyDataSetChanged();
            }

        };


    }

    public void setOnItemClickListener(final CustomSuggestionsAdapter.OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    static class SuggestionHolder extends RecyclerView.ViewHolder {
        protected TextView title;
        protected TextView code;

        public SuggestionHolder(final View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.name);
            code = itemView.findViewById(R.id.code);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClick(itemView, getAdapterPosition());


                    }


                }
            });

        }
    }

}