package org.grameen.fdp.adapter;

/**
 * Created by AangJnr on 9/21/16.
 */

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.grameen.fdp.R;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.utility.Constants;

import java.util.List;


/**
 * Created by AangJnr on 6/27/16.
 */


public class PlotsListAdapter extends RecyclerView.Adapter<PlotsListAdapter.ViewHolder> {

    OnItemClickListener mItemClickListener;
    OnLongClickListener longClickListener;
    private List<RealPlot> plots;
    private Context context;


    /**
     * Constructor
     *
     * @param plots
     **/

    public PlotsListAdapter(Context context, List<RealPlot> plots) {
        this.plots = plots;
        this.context = context;

    }


    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return plots.size();

    }

    @Override
    public PlotsListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.plot_item_view, viewGroup, false);

        return new PlotsListAdapter.ViewHolder(v);


    }


    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {


        RealPlot plot = plots.get(position);
        viewHolder.name.setText(plot.getName());


    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    public void setOnItemClickListener(final PlotsListAdapter.OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public void OnLongClickListener(final PlotsListAdapter.OnLongClickListener mLongClickListener) {
        this.longClickListener = mLongClickListener;
    }


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }


    public interface OnLongClickListener {
        void onLongClick(View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {


        RelativeLayout mainLayout;
        TextView name;


        ViewHolder(final View itemView) {
            super(itemView);

            mainLayout = (RelativeLayout) itemView.findViewById(R.id.mainLayout);
            name = (TextView) itemView.findViewById(R.id.name);

            mainLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClick(itemView, getAdapterPosition());


                    }


                }
            });


            if (!PreferenceManager.getDefaultSharedPreferences(context).getString("flag", "").equals(Constants.MONITORING))

                mainLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        longClickListener.onLongClick(view, getAdapterPosition());

                        return true;
                    }
                });


        }


    }

}
