/**
 * ImagePrint for printing
 *
 * @author Brother Industries, Ltd.
 * @version 2.2
 */
package org.grameen.fdp.printer.printprocess;

import android.content.Context;

import org.grameen.fdp.printer.common.MsgDialog;
import org.grameen.fdp.printer.common.MsgHandle;

import java.util.ArrayList;

public class MultiImagePrint extends BasePrint {

    private ArrayList<String> mImageFiles;

    public MultiImagePrint(Context context, MsgHandle mHandle, MsgDialog mDialog) {
        super(context, mHandle, mDialog);
    }

    /**
     * set print data
     */
    public ArrayList<String> getFiles() {
        return mImageFiles;
    }

    /**
     * set print data
     */
    public void setFiles(ArrayList<String> files) {
        mImageFiles = files;
    }

    /**
     * do the particular print
     */
    @Override
    protected void doPrint() {
        mPrintResult = mPrinter.printFileList(mImageFiles);

    }

}