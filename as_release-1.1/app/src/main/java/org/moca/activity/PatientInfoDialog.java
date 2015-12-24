package org.moca.activity;

import org.moca.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Alerts that patient information does no match database and captures whether 
 * user wants to override
 * 
 * @author Sana Development Team
 *
 */
public class PatientInfoDialog extends Activity {

	private static String errormessage;

	/** {@inheritDoc} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.patient_out_of_sync);
    	builder.setMessage(errormessage);
    	builder.setPositiveButton(getString(R.string.general_yes), 
    		new DialogInterface.OnClickListener(){
    		public void onClick(DialogInterface dialog, int id) {
    			setResult(RESULT_OK);
    			dialog.cancel();
    		}
    	});
    	builder.setNegativeButton(getString(R.string.general_no), 
    		new DialogInterface.OnClickListener(){
    		public void onClick(DialogInterface dialog, int id) {
    			setResult(RESULT_CANCELED);
    			dialog.cancel();
    		}
    	});
    	builder.show();
    }

    /**
     * Sets the error message to display
     * 
     * @param msg the new error message
     */
    public void setErrorMessage(String msg) {
    	errormessage = msg;
    }
} 