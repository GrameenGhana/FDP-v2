package org.grameen.fdp.viewholder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.grameen.fdp.R;
import org.grameen.fdp.activity.FamilyMembersActivity;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.utility.Callbacks;

import java.util.List;

/**
 * Created by aangjnr on 22/02/2018.
 */

public class SpinnerViewHolder extends AbstractViewHolder {

    static Callbacks.UpdateJsonArray updateJsonArrayListener;

    PopupWindow popupWindow;


    private static final int TEXT_SIZE = 10;
    private static final int TITLE_TEXT_SIZE = 14;
    public final LinearLayout cell_container;


    Spinner spinner = null;
    //TextView textView;

    public static void UpdateJsonArrayListener(Callbacks.UpdateJsonArray listener)
    {
        updateJsonArrayListener = listener;

    }


    public SpinnerViewHolder(View itemView, String type) {
        super(itemView);
        Log.i("CELL VIEW HOLDER TAG", itemView.getTag().toString());
        Log.i("CELL VIEW HOLDER TYPE", type);

        cell_container = itemView.findViewById(R.id.cell_container);


        //textView = itemView.findViewById(R.id.info);
        spinner = itemView.findViewById(R.id.cell_data);

    }

    public void setData(int rowPosition, Question data) {

        //cell_textview.setText(String.valueOf(data));


        if(spinner != null)
           bindSpinnerView(spinner, data, rowPosition);







    }





    void bindSpinnerView(final Spinner spinner, final Question q, final int rowPosition){
        final String defaultValue = FamilyMembersActivity.getValue(rowPosition, q.getId());


       /* if(updateJsonArrayListener != null)
            updateJsonArrayListener.onItemValueChanged(rowPosition, q.getId(), defaultValue);
*/



        Log.i("FMT ADAPTER", "TYPE SELECTABLE" );
        Log.i("SPINNER", "DEFAULT VALUE IS " + defaultValue);


        if(!q.getOptions__c().contains("-select-"))
            q.setOptions__c(q.getOptions__c() + ", -select-");

            final List<String> items = q.formatQuestionOptions();


        //spinner.setPrompt(defaultValue);
        spinner.setPrompt("-select-");
        spinner.setTag(q.getId());

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(spinner.getContext(), android.R.layout.simple_spinner_item, items) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    if (position == getCount()) {
                        TextView itemView = ((TextView) view.findViewById(android.R.id.text1));
                        itemView.setText("");
                        itemView.setHint(getItem(getCount()));
                    }
                    return view;
                }

                @Override
                public int getCount() {
                    return super.getCount(); // don't display last item (it's used for the prompt)
                }
            };
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                    Log.i("SPINNER VH", "SPINNER ITEM SELECTED " + items.get(pos));
                    Log.i("SPINNER VH", "POSITION " + pos);


                    if (pos == 0) {


                        if(defaultValue != null){
                            q.setDefault_value__c(items.get(pos));
                            if (updateJsonArrayListener != null)
                                updateJsonArrayListener.onItemValueChanged(rowPosition, q.getId(), items.get(pos));
                        }

                    } else {
                        // if something is selected, set the value on the model
                        q.setDefault_value__c(items.get(pos));
                        if (updateJsonArrayListener != null)
                            updateJsonArrayListener.onItemValueChanged(rowPosition, q.getId(), items.get(pos));

                    }





                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {


                }
            });

           // spinner.setSelection(items.size() - 1);

        refresh(spinner, defaultValue, items);


        //textView.setText(q.getHelp_Text__c());

/*
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    if (popupWindow != null && popupWindow.isShowing())
                        popupWindow.dismiss();

                    else if (spinner.getContext() != null) {

                        LayoutInflater layoutInflater = (LayoutInflater) spinner.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                        View popupView = layoutInflater.inflate(R.layout.popup_info_layout, null);
                        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                        TextView infoText = popupView.findViewById(R.id.info_text);
                        infoText.setText(q.getHelp_Text__c());

                        popupWindow.setBackgroundDrawable(new BitmapDrawable());
                        popupWindow.setOutsideTouchable(true);

                        popupWindow.setFocusable(false);
                        popupWindow.showAsDropDown(imageView, 10, 5);

                    }


                }
            });*/


        //cell_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        //cell_container.getLayoutParams().height = 70;
        spinner.requestLayout();


    }




    private void refresh(Spinner spinner, @Nullable String defValue, List<String>items ) {

        Log.i("FMTV", "REFRESH " + defValue);
        int selectionIndex = 0;    // index of last item shows the 'prompt'



        if(defValue != null && !defValue.equals("--") && !defValue.equals("-select-")) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).equals(defValue)) {
                    selectionIndex = i;
                    break;
                }

            }

            spinner.setSelection(selectionIndex);
        }
    }






}