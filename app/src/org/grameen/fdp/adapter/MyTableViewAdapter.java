package org.grameen.fdp.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jaredrummler.materialspinner.MaterialSpinner;

import org.grameen.fdp.R;
import org.grameen.fdp.object.Data;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.Recommendation;
import org.grameen.fdp.utility.DatabaseHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import de.codecrafters.tableview.TableView;
import de.codecrafters.tableview.toolkit.LongPressAwareTableDataAdapter;

import static org.grameen.fdp.utility.Constants.BUTTON_VIEW;
import static org.grameen.fdp.utility.Constants.TAG_OTHER_TEXT_VIEW;
import static org.grameen.fdp.utility.Constants.TAG_RESULTS;
import static org.grameen.fdp.utility.Constants.TAG_TITLE_TEXT_VIEW;
import static org.grameen.fdp.utility.Constants.TAG_VIEW;

/**
 * Created by aangjnr on 17/01/2018.
 */

public class MyTableViewAdapter extends LongPressAwareTableDataAdapter<Data> {

    private static final int TEXT_SIZE = 11;
    private static final int TITLE_TEXT_SIZE = 14;
    private static MaterialSpinner.OnItemSelectedListener itemSelectedListener;
    private static View.OnClickListener mOnClickListener;

    Context context;
   // String[] YEARS = {"YEAR 1", "YEAR 2", "YEAR 3", "YEAR 4", "YEAR 5"};


    public MyTableViewAdapter(final Context context, final List<Data> data, final TableView<Data> tableView) {
        super(context, data, tableView);
        this.context = context;
    }

    public void setItemSelectedListener(final MaterialSpinner.OnItemSelectedListener mItemSelectedListener) {
        this.itemSelectedListener = mItemSelectedListener;
    }

    public void setClickistener(final View.OnClickListener listener) {
        this.mOnClickListener = listener;
    }

    @Override
    public View getDefaultCellView(int i, int i1, ViewGroup viewGroup) {
        final Data myTableData = getRowData(i);
        View renderedView;
        if (i1 == 0) {
            //Todo set questions here
            renderedView = renderColumn0Values(myTableData);
        } else renderedView = renderCalculatedValuesForYear(myTableData, i1 - 1);
        return renderedView;
    }

    @Override
    public View getLongPressCellView(int i, int i1, ViewGroup viewGroup) {
        return null;
    }


    private View renderCalculatedValuesForYear(final Data data, int year) {
        TextView textView = null;
        List<String> calculationsForTheYears = data.getYearsDataFormula();

        if (calculationsForTheYears != null) {
            textView = new TextView(getContext());
            textView.setPadding(20, 10, 20, 10);
            if (data.getTag().equals(TAG_RESULTS))
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setTextSize(TEXT_SIZE);

            try {
                Double value = Double.parseDouble(calculationsForTheYears.get(year));

                NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                DecimalFormat formatter = (DecimalFormat) nf;
                formatter.applyPattern("#,###,###.##");

                textView.setText(formatter.format(value));
                if (value < 0.0) {
                    textView.setTextColor(ContextCompat.getColor(getContext(), R.color.cpb_red));
                } else if (value > 0) {
                    if (data.getTag().equals(TAG_RESULTS))
                        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                }
            } catch (Exception e) {
                Log.i("MY TABLE ADAPTER", e.getMessage());

            }


        } else {

            if (year == 0) {

                textView = new TextView(getContext());
                textView.setText(data.getSingleValue());
                textView.setPadding(20, 10, 20, 10);
                textView.setTextSize(TEXT_SIZE);
            }
        }

        return textView;
    }


    private View renderColumn0Values(final Data data) {

        View view = null;

        if (data.getTag() != null) {

            if (data.getTag().equals(TAG_TITLE_TEXT_VIEW)) {
                TextView textView = new TextView(getContext());
                textView.setText(data.getLabel());
                textView.setPadding(20, 10, 20, 10);
                textView.setTextSize(TITLE_TEXT_SIZE);
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));

                view = textView;

            } else if (data.getTag().equals(TAG_OTHER_TEXT_VIEW)) {

                TextView textView = new TextView(getContext());
                textView.setText(data.getLabel());
                textView.setPadding(20, 10, 20, 10);
                textView.setTextSize(TEXT_SIZE);
                view = textView;


            } else if (data.getTag().equals(BUTTON_VIEW)) {

                @SuppressLint("RestrictedApi") ContextThemeWrapper newContext = new ContextThemeWrapper(context, R.style.PrimaryButton);

                final Button button = new Button(newContext);
                // button.setBackgroundResource(R.drawable.button_background_accent);

                String name = "";
                try {
                    Recommendation recommendation = DatabaseHelper.getInstance(context).getRecommendationBasedOnName(data.getLabel().split("_")[1]);
                    name = (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("toggleTranslation", false)) ? recommendation.getName() : recommendation.getTranslation();
                    name = getResources().getString(R.string.change_to) + " " + name;

                } catch (Exception ignored) {

                    name = getResources().getString(R.string.change_to) + " " + data.getLabel().split("_")[1];

                }
                // String text = getResources().getString(R.string.change_to) + " " + data.getLabel().split("_")[1];

                button.setText(name);
                // button.setTextColor(ContextCompat.getColor(context, R.color.white));
                button.setTag(data.getLabel());
                button.setPadding(20, 10, 20, 10);
                button.setTextSize(9);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (mOnClickListener != null)
                            mOnClickListener.onClick(button);


                    }
                });
                view = button;


            } else if (data.getTag().equals(TAG_VIEW)) {
                Question startYearQuestion = DatabaseHelper.getInstance(context).getQuestionByTranslation("Start year");

                final MaterialSpinner spinner = new MaterialSpinner(getContext());
                spinner.setItems(startYearQuestion.formatQuestionOptions());
                spinner.setTag(data.getLabel());
                spinner.setPadding(20, 10, 20, 10);
                spinner.setTextSize(TEXT_SIZE);
                spinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                        if (itemSelectedListener != null)
                            itemSelectedListener.onItemSelected(view, position, id, item);
                    }
                });
                try {
                    spinner.setSelectedIndex(Integer.parseInt(data.getLabel().split("_")[1]));
                } catch (Exception ignored) {
                    ignored.getMessage();
                }


                view = spinner;



            } else if (data.getTag().equals(TAG_RESULTS)) {
                TextView textView = new TextView(getContext());

                textView.setText(data.getLabel());
                textView.setPadding(20, 10, 20, 10);
                textView.setTextSize(TITLE_TEXT_SIZE);
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
                view = textView;

            }
        } else {

            TextView textView = new TextView(getContext());
            textView.setText(data.getLabel());
            textView.setPadding(20, 10, 20, 10);
            textView.setTextSize(TEXT_SIZE);
            view = textView;
        }


        return view;
    }


}
