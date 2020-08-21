package org.grameen.fdp.utility;

/**
 * Created by aangjnr on 05/08/2017.
 */

public class Callbacks {

    public interface AnItemSelectedListener {

        void onItemSelected(String item);

    }

    public interface UpdateJsonArray {

        void onItemValueChanged(int id, String uid, String value);

    }

    public interface NetworkActivityCompleteListener {

        void taskComplete(int response);

    }
}
