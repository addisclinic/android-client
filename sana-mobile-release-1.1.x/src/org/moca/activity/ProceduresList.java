package org.moca.activity;

import org.moca.R;
import org.moca.db.MocaDB.ProcedureSQLFormat;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Displays a list of Procedures.
 * 
 * @author Sana Development Team
 */
public class ProceduresList extends ListActivity {
    private static final String TAG = ProceduresList.class.toString();
    private static final String[] PROJECTION = new String[] { 
    	ProcedureSQLFormat._ID,ProcedureSQLFormat.TITLE, 
    	ProcedureSQLFormat.AUTHOR };
    
    /** {@inheritDoc} */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        
        if(uri == null)
        	uri = ProcedureSQLFormat.CONTENT_URI;

        Cursor cursor = managedQuery(uri, PROJECTION, null, null, 
        		ProcedureSQLFormat.DEFAULT_SORT_ORDER);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.procedure_list_row, cursor,
                new String[] { ProcedureSQLFormat.TITLE, 
        					   ProcedureSQLFormat.AUTHOR },
                new int[] { R.id.toptext, R.id.bottomtext });
        setListAdapter(adapter);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || 
        		Intent.ACTION_GET_CONTENT.equals(action)) 
        {
            // The caller is waiting for us to return a note selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();
        } else {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
