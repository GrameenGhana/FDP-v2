package org.grameen.fdp.utility;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.grameen.fdp.R;

/**
 * Created by aangjnr on 01/12/2017.
 */


public class CustomToast extends Toast {

    static Toast toast;
    Context context;

    public CustomToast(Context context) {
        super(context);
    }


    public static Toast makeToast(Context c, String message, int duration) {

        LayoutInflater inflater = LayoutInflater.from(c);
        View layout = inflater.inflate(R.layout.custom_toast, null, false);
        // set a message
        TextView text =  layout.findViewById(R.id.toast_text);
        text.setText(message);
        // Toast...
        toast = new Toast(c);
        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, (Utils.getScreenHeight(c) / 3));
        toast.setDuration(duration);
        toast.setView(layout);

        return toast;

    }


}
