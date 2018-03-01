package org.grameen.fdp.utility;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.evrencoskun.tableview.listener.ITableViewListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.grameen.fdp.R;

/**
 * Created by aangjnr on 22/02/2018.
 */

public class TableViewListener implements ITableViewListener {

    String TAG = TableViewListener.class.getSimpleName();
    /**
     * Called when user click any cell item.
     *
     * @param cellView  : Clicked Cell ViewHolder.
     * @param columnPosition : X (Column) position of Clicked Cell item.
     * @param rowPosition : Y (Row) position of Clicked Cell item.
     */
    @Override
    public void onCellClicked(@NonNull RecyclerView.ViewHolder cellView, int columnPosition, int
            rowPosition) {
        // Do what you want.


        Log.i(TAG, "CELL CLICKED");
        View view = cellView.itemView;

        View childView = view.findViewById(R.id.cell_data);

        if(childView instanceof MaterialEditText){
            Log.i(TAG, "EDIT TEXT");


            childView.requestFocus();
            InputMethodManager imm = (InputMethodManager) childView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(childView, InputMethodManager.SHOW_IMPLICIT);



        }else if(childView instanceof CheckBox){
            Log.i(TAG, "CHECK BOX");

            CheckBox c = (CheckBox) childView;

             c.setChecked(!c.isChecked());
        }else if(childView instanceof Spinner){
            Log.i(TAG, "SPINNER");

            Spinner spinner = (Spinner) childView;
            spinner.performClick();

        }


    }

    /**
     * Called when user long press any cell item.
     *
     * @param cellView : Long Pressed Cell ViewHolder.
     * @param column   : X (Column) position of Long Pressed Cell item.
     * @param row      : Y (Row) position of Long Pressed Cell item.
     */
    @Override
    public void onCellLongPressed(@NonNull RecyclerView.ViewHolder cellView, int column, int row) {
        // Do What you want

        Log.i(TAG, "CELL LONG CLICKED");

    }

    /**
     * Called when user click any column header item.
     *
     * @param columnHeaderView : Clicked Column Header ViewHolder.
     * @param columnPosition        : X (Column) position of Clicked Column Header item.
     */
    @Override
    public void onColumnHeaderClicked(@NonNull RecyclerView.ViewHolder columnHeaderView, int
            columnPosition) {
        // Do what you want.
    }

    /**
     * Called when user click any column header item.
     *
     * @param columnHeaderView : Long pressed Column Header ViewHolder.
     * @param columnPosition        : X (Column) position of Clicked Column Header item.
     * @version 0.8.5.1
     */
    @Override
    public void onColumnHeaderLongPressed(@NonNull RecyclerView.ViewHolder columnHeaderView, int
            columnPosition) {
        // Do what you want.
    }

    /**
     * Called when user click any Row Header item.
     *
     * @param rowHeaderView : Clicked Row Header ViewHolder.
     * @param rowPosition     : Y (Row) position of Clicked Row Header item.
     */
    @Override
    public void onRowHeaderClicked(@NonNull RecyclerView.ViewHolder rowHeaderView, int
            rowPosition) {
        // Do what you want.

    }

    /**
     * Called when user click any Row Header item.
     *
     * @param rowHeaderView : Long pressed Row Header ViewHolder.
     * @param rowPosition     : Y (Row) position of Clicked Row Header item.
     * @version 0.8.5.1
     */
    @Override
    public void onRowHeaderLongPressed(@NonNull RecyclerView.ViewHolder rowHeaderView, int
            rowPosition) {
        // Do what you want.

    }
}