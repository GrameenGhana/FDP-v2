package org.grameen.fdp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grameen.fdp.R;
import org.grameen.fdp.activity.PlotMonitoringActivity;
import org.grameen.fdp.object.Data2;
import org.grameen.fdp.object.PlotMonitoringTableData;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.utility.Constants;

import java.util.ArrayList;
import java.util.List;

import de.codecrafters.tableview.TableView;
import de.codecrafters.tableview.model.TableColumnWeightModel;

/**
 * Created by aangjnr on 08/02/2018.
 */

public class PlotMonitoringTablePagerAdapter extends PagerAdapter {

    private Context mContext;
    List<PlotMonitoringTableData> plotMonitoringTableDataList;

    public PlotMonitoringTablePagerAdapter(Context context, List<PlotMonitoringTableData> _plotMonitoringTableDataList) {
        mContext = context;
        this.plotMonitoringTableDataList = _plotMonitoringTableDataList;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {





        PlotMonitoringTableData dataList = plotMonitoringTableDataList.get(position);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.viewpager_table_view_item, collection, false);
        TableView tableView = layout.findViewById(R.id.tableView);
        /*TextView titleTextView = layout.findViewById(R.id.title_view);
        titleTextView.setText(dataList.getTitle());*/
        layout.setTag(dataList.getTitle());
        setAdapter(tableView, dataList);
        collection.addView(layout);



        return layout;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return plotMonitoringTableDataList.size();
    }


    @Override
    public int getItemPosition(@NonNull Object object) {
        //return super.getItemPosition(object);


//        PlotMonitoringTableData plotMonitoringTableData = (PlotMonitoringTableData) object;
        //int position = plotMonitoringTableDataList.indexOf(plotMonitoringTableData);
/*
        if (position >= 0) {
            // The current data matches the data in this active fragment, so let it be as it is.
            return position;
        } else {
            // Returning POSITION_NONE means the current data does not matches the data this fragment is showing right now.  Returning POSITION_NONE constant will force the fragment to redraw its view layout all over again and show new data.
            return POSITION_NONE;
        }*/

        return POSITION_NONE;



    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        PlotMonitoringTableData dataList = plotMonitoringTableDataList.get(position);
        return dataList.getTitle();
    }


    void setAdapter(TableView tableView, PlotMonitoringTableData data){



        tableView.setColumnCount(3);

        String [] TABLE_HEADERS = {"AO's", "Farmer Competence", "Reason for Fail"};


        TableColumnWeightModel columnModel = new TableColumnWeightModel(tableView.getColumnCount());
        columnModel.setColumnWeight(0, 1);
        columnModel.setColumnWeight(1, 2);
        columnModel.setColumnWeight(2, 3);

        tableView.setColumnModel(columnModel);


        PlotMonitoringTableHearderAdapter headerAdapter = new PlotMonitoringTableHearderAdapter(mContext, TABLE_HEADERS);
        tableView.setHeaderAdapter(headerAdapter);


        List<Data2> GENERAL_AO_MONITORING = new ArrayList<>();


        for(Data2 q : data.getTableData()){

            //Todo get results
            GENERAL_AO_MONITORING.add(new Data2("--", q.getV1(), q.getV2(), q.getV3(), q.getTag()));

        }


        PlotMonitoringTableViewAdapter plotMonitoringTableViewAdapter = new PlotMonitoringTableViewAdapter(mContext, GENERAL_AO_MONITORING, tableView);
        tableView.setDataAdapter(plotMonitoringTableViewAdapter);



    }



    public void setData(List<PlotMonitoringTableData> _plotMonitoringTableData) {
        this.plotMonitoringTableDataList = _plotMonitoringTableData;
    }

    public List<PlotMonitoringTableData> getData() {
        return plotMonitoringTableDataList;
    }



}