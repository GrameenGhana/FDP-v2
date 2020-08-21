package org.grameen.fdp.viewholder;

import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.grameen.fdp.R;
import org.grameen.fdp.activity.FamilyMembersActivity;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.utility.Callbacks;

/**
 * Created by aangjnr on 22/02/2018.
 */

public class CheckBoxViewHolder extends AbstractViewHolder {
    static Callbacks.UpdateJsonArray updateJsonArrayListener;


    private static final int TEXT_SIZE = 10;
    private static final int TITLE_TEXT_SIZE = 14;

    public final LinearLayout cell_container;




    CheckBox checkBox;



    public static void UpdateJsonArrayListener(Callbacks.UpdateJsonArray listener)
    {
        updateJsonArrayListener = listener;

    }


    public CheckBoxViewHolder(View itemView, String type) {
        super(itemView);
        Log.i("CELL VIEW HOLDER TAG", itemView.getTag().toString());
        Log.i("CELL VIEW HOLDER TYPE", type);

        cell_container = itemView.findViewById(R.id.cell_container);


         checkBox = itemView.findViewById(R.id.cell_data);


    }

    public void setData(int rowPosition, Question data) {

        //cell_textview.setText(String.valueOf(data));


        if(checkBox != null)
           bindCheckBox(checkBox, data, rowPosition);





    }







    void bindCheckBox(final CheckBox checkBox, final Question q, final int rowPosition){


        Log.i("FMT ADAPTER", "TYPE CHECKBOX");

        checkBox.setText("YES");

        String defVal = FamilyMembersActivity.getValue(rowPosition, q.getId());

                 if(defVal != null && defVal.equalsIgnoreCase("yes")) {
                     checkBox.setChecked(true);
                     //if(updateJsonArrayListener != null)
                       //  updateJsonArrayListener.onItemValueChanged(rowPosition, q.getId(), "YES");

                 }

                 else  {
                     checkBox.setChecked(false);
                    // if(updateJsonArrayListener != null)
                      //   updateJsonArrayListener.onItemValueChanged(rowPosition, q.getId(), "NO");

                 }






        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                String value = "";
                if(isChecked) value = "YES";
                else value = "NO";

                //q.setDefault_value__c(value);

                if(updateJsonArrayListener != null)
                    updateJsonArrayListener.onItemValueChanged(rowPosition, q.getId(), value);


            }
        });

        //cell_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        //cell_container.getLayoutParams().height = 70;

        checkBox.requestLayout();


    }






}