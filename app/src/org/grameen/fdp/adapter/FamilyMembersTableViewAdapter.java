package org.grameen.fdp.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.jaredrummler.materialspinner.MaterialSpinner;

import org.grameen.fdp.object.FamilyMembersData;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.utility.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.codecrafters.tableview.TableDataAdapter;

/**
 * Created by aangjnr on 17/01/2018.
 */

public class FamilyMembersTableViewAdapter extends TableDataAdapter<FamilyMembersData> {

    private static final int TEXT_SIZE = 10;
    private static final int TITLE_TEXT_SIZE = 14;
    static MaterialSpinner.OnItemSelectedListener itemSelectedListener;
    static View.OnClickListener mOnClickListener;

    Context context;
    int POSTITION = 0;

    JSONArray answersArray;
    StringBuilder stringBuilder;
    List<JSONObject> jsonObjectList = new ArrayList<>();

    JSONArray jsonArray = new JSONArray();


    public FamilyMembersTableViewAdapter(final Context context, final List<FamilyMembersData> datas, @Nullable JSONArray array) {
        super(context, datas);

        this.context = context;
        this.answersArray = array;


    }

    public void setItemSelectedListener(final MaterialSpinner.OnItemSelectedListener mItemSelectedListener) {
        this.itemSelectedListener = mItemSelectedListener;
    }

    public void setClickistener(final View.OnClickListener listener) {
        this.mOnClickListener = listener;
    }



    @Override
    public View getCellView(int i, int i1, ViewGroup viewGroup) {
        Log.i("FMT ADAPTER", "i = " + i );
        Log.i("FMT ADAPTER", "i1 = " + i1 );

        Log.i("Render View", "Json Array is " + answersArray);




        FamilyMembersData familyMembersData = getRowData(i);

        POSTITION = i;

         View renderedView;

        if (i == 0) {
            renderedView = renderHelperTextView(familyMembersData.getQuestionList().get(i1).getHelp_Text__c(), i);

        } else {

            renderedView = renderView(familyMembersData.getQuestionList().get(i1), i);
        }



        return renderedView;
    }


    private View renderView(final Question q, final int pos) {


        JSONObject savedValuesObject = null;
        try {

            if(answersArray != null)
                savedValuesObject =  answersArray.getJSONObject(pos);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Log.i("FMT ADAPTER", "RENDERING" );
        Log.i("FMT ADAPTER", "POSITION IS " + pos );

        Log.i("Render View", "Json Object is " + savedValuesObject);


        View view = null;

        final JSONObject jsonObject =  getRowData(pos).getJsonObject();


        if(q.getType__c().equalsIgnoreCase(Constants.TYPE_SELECTABLE)) {
            Log.i("FMT ADAPTER", "TYPE SELECTABLE" );

            view = getReasonForFailureView(q, savedValuesObject, jsonObject);


        }else if(q.getType__c().equalsIgnoreCase(Constants.TYPE_CHECKBOX)) {

            Log.i("FMT ADAPTER", "TYPE CHECKBOX");

            CheckBox checkBox;
            checkBox = new CheckBox(getContext());
            checkBox.setPadding(10, 10, 10, 10);
            checkBox.setTextSize(TEXT_SIZE);
            checkBox.setText("YES");

            try {

                if(savedValuesObject != null)
                    if(savedValuesObject.get(q.getId()).toString() != null &&
                            savedValuesObject.get(q.getId()).toString().equalsIgnoreCase("yes"))

                        checkBox.setChecked(true);
                    else
                        jsonArray.put(POSTITION).put(q.getId()).put("0");


            } catch (JSONException e) {
                e.printStackTrace();
                jsonArray.put(POSTITION).put(q.getId()).put("0");

            }

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    String value = "";
                    if(isChecked) value = "YES";
                    else value = "NO";


                    addToList(POSTITION, q.getId(), value);


                }
            });

            view = checkBox;
        } else if(q.getType__c().equalsIgnoreCase(Constants.TYPE_NUMBER_DECIMAL)) {

            Log.i("FMT ADAPTER", "TYPE TEXT" );

            EditText editText;
            editText = new EditText(getContext());
            editText.setPadding(10, 10, 10, 10);
            editText.setTextSize(TEXT_SIZE);
            editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);


            try {

                if(savedValuesObject != null)
                    if(savedValuesObject.get(q.getId()).toString() != null)
                        editText.setText(savedValuesObject.get(q.getId()).toString());
                    else
                        jsonArray.put(POSTITION).put(q.getId()).put("0");


            } catch (JSONException e) {
                e.printStackTrace();
                jsonArray.put(POSTITION).put(q.getId()).put("0");


            }
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                    addToList(POSTITION, q.getId(), s.toString());


                }
            });
            view = editText;


        }else if(q.getType__c().equalsIgnoreCase(Constants.TYPE_NUMBER)) {

            Log.i("FMT ADAPTER", "TYPE TEXT" );

            EditText editText;
            editText = new EditText(getContext());
            editText.setPadding(10, 10, 10, 10);
            editText.setTextSize(TEXT_SIZE);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);


            try {

                if(savedValuesObject != null)
                    if(savedValuesObject.get(q.getId()).toString() != null)
                        editText.setText(savedValuesObject.get(q.getId()).toString());
                    else
                        jsonArray.put(POSTITION).put(q.getId()).put("0");


            } catch (JSONException e) {
                e.printStackTrace();
                jsonArray.put(POSTITION).put(q.getId()).put("0");

            }


            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {


                    addToList(POSTITION, q.getId(), s.toString());


                }
            });
            view = editText;


        }else if(q.getType__c().equalsIgnoreCase(Constants.TYPE_TEXT)) {

            Log.i("FMT ADAPTER", "TYPE TEXT" );

            final EditText editText;
            editText = new EditText(getContext());
            editText.setPadding(10, 10, 10, 10);
            editText.setTextSize(TEXT_SIZE);


            try {

                if(savedValuesObject != null)
                    if(savedValuesObject.get(q.getId()).toString() != null)
                        editText.setText(savedValuesObject.get(q.getId()).toString());
                    else
                        jsonArray.put(POSTITION).put(q.getId()).put("0");


            } catch (JSONException e) {
                e.printStackTrace();
                jsonArray.put(POSTITION).put(q.getId()).put("0");
            }


            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {


                    addToList(POSTITION, q.getId(), s.toString());


                }
            });
            view = editText;

        }

        //getRowData(pos).setJsonObject(jsonObject);


        return view;
    }

    private View renderHelperTextView(final String helperText, final int pos) {


        View view;

        Log.i("FMT ADAPTER", "TYPE TEXT");

        final TextView editText;
        editText = new TextView(getContext());
        editText.setPadding(10, 10, 10, 10);
        editText.setTextSize(TEXT_SIZE);
        editText.setText(helperText);
        view = editText;
        return view;
    }



    public JSONArray getAllAnswers(){
        int i = 0;


         Log.i("FAMMEMBERSADAP", "SIZE OF JSONARRAY IS " + jsonArray.length());


        return jsonArray;

    }



    int value;


private void refresh(Spinner spinner, @Nullable String defValue, List<String>items ) {

        Log.i("FMTV", "REFRESH");
         int selectionIndex = items.size() -1;    // index of last item shows the 'prompt'



        if(defValue != null && !defValue.equals("--") && !defValue.equals("Select"))
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).equals(defValue)) {
                    selectionIndex = i;
                    break;
                }

        }

        spinner.setSelection(selectionIndex);
    }




View getReasonForFailureView(final Question q, final JSONObject savedValuesObject, final JSONObject jsonObject) {

            View view;
            q.setOptions__c(q.getOptions__c() + ", Select");

            final List<String> items = q.formatQuestionOptions();

            final Spinner spinner = new Spinner(context);
            spinner.setPrompt("Select");
            spinner.setTag(q.getId());

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);

                    if (position == getCount()) {
                        TextView itemView = ((TextView) view.findViewById(android.R.id.text1));
                        itemView.setText("");
                        itemView.setHint(getItem(getCount()));


                        try {

                            if (savedValuesObject != null)
                                if (savedValuesObject.get(q.getId()).toString() != null) {

                                    for (int i = 0; i < items.size(); i++) {

                                        if (items.get(i).equals(savedValuesObject.get(q.getId()).toString())) {
                                            spinner.setSelection(i);
                                            break;
                                        }
                                    }
                                } else
                                    jsonArray.put(POSTITION).put(q.getId()).put("0");


                        } catch (JSONException e) {
                            e.printStackTrace();
                            jsonArray.put(POSTITION).put(q.getId()).put("0");


                        }

                    }
                    return view;
                }

                @Override
                public int getCount() {
                    return super.getCount() - 1; // don't display last item (it's used for the prompt)
                }
            };
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {


                    Log.i("TABLE ADAPTER", "Spinner item selected at " + POSTITION);

                    //if (itemSelectedListener != null)
                    //   itemSelectedListener.onItemSelected(view, position, id, item);


                    addToList(POSTITION, q.getId(), items.get(pos));



                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {


                }
            });

            spinner.setSelection(items.size() - 1);

            try {

                if (savedValuesObject != null)
                    if (savedValuesObject.get(q.getId()).toString() != null) {

                        for (int i = 0; i < items.size(); i++) {

                            if (items.get(i).equals(savedValuesObject.get(q.getId()).toString())) {
                                //spinner.setSelection(i);
                                refresh(spinner, items.get(i), items);

                                break;
                            }
                        }
                    } else
                        jsonObject.put(q.getId(), items.get(0));


            } catch (JSONException e) {
                e.printStackTrace();
                try {
                    jsonObject.put(q.getId(), items.get(0));
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }



            }

            view = spinner;


            return view;

        }








        void addToList(int position, String id, String value){

             try{



            }catch (Exception e){

            e.printStackTrace();

    }

        }




}
