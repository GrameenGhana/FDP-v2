package org.grameen.fdp.viewholder;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.grameen.fdp.R;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.utility.Callbacks;

/**
 * Created by aangjnr on 22/02/2018.
 */

public class TextViewHolder extends AbstractViewHolder {
    static Callbacks.UpdateJsonArray updateJsonArrayListener;

    public final LinearLayout cell_container;

    private static final int TEXT_SIZE = 10;
    private static final int TITLE_TEXT_SIZE = 14;


    TextView editText = null;


    public static void UpdateJsonArrayListener(Callbacks.UpdateJsonArray listener) {
        updateJsonArrayListener = listener;

    }


    public TextViewHolder(View itemView, String type) {
        super(itemView);
        Log.i("CELL VIEW HOLDER TAG", itemView.getTag().toString());
        Log.i("CELL VIEW HOLDER TYPE", type);

        cell_container = itemView.findViewById(R.id.cell_container);


        editText = itemView.findViewById(R.id.cell_data);


    }

    public void setData(int rowPosition, Question data) {

        //cell_textview.setText(String.valueOf(data));


        if (editText != null)
            bindEditTextView(editText, data, rowPosition);


    }


    void bindEditTextView(TextView editText, final Question q, final int rowPosition) {

        Log.i("TEXT Cell VH", "NOT NULL");


        if (q.getHelp_Text__c() != null && !q.getHelp_Text__c().equalsIgnoreCase(""))

            editText.setText(q.getHelp_Text__c());
        else
            editText.setText("--");

        editText.setTextSize(12);

        //cell_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        //cell_container.getLayoutParams().height = 70;

        editText.requestLayout();


    }


}