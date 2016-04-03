package org.moca.activity;

import org.moca.R;
import org.moca.db.MocaDB.ProcedureSQLFormat;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
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
public class ProceduresList extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ProceduresList.class.toString();
    private static final String[] PROJECTION = new String[] { 
    	ProcedureSQLFormat._ID,
        ProcedureSQLFormat.TITLE,
    	ProcedureSQLFormat.AUTHOR };
    private static final int LOADER_ID = ProceduresList.class.hashCode();
    private static final String URI_KEY = TAG + ".URI_KEY";

    /** {@inheritDoc} */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.procedure_list);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                                            R.layout.procedure_list_row,
                                            null,  // assign Cursor when onLoadFinished() is called
                                            new String[] { PROJECTION[1],
                                                           PROJECTION[2] },
                                            new int[] { R.id.toptext, R.id.bottomtext }, 0);
        setListAdapter(adapter);
        LoaderManager loader = getLoaderManager();
        Bundle args = new Bundle();
        Uri uri = getIntent().getData();
        if(uri == null)
            uri = ProcedureSQLFormat.CONTENT_URI;

        args.putString(URI_KEY, uri.toString());
        loader.initLoader(LOADER_ID, args, this);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = Uri.parse(args.getString(URI_KEY));

        if(uri == null) {
            uri = ProcedureSQLFormat.CONTENT_URI;
        }
        return new CursorLoader(ProceduresList.this, uri, PROJECTION,
                                null, null, ProcedureSQLFormat.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_ID) {
            SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
            adapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        SimpleCursorAdapter adapter = (SimpleCursorAdapter)getListAdapter();
        // Loader's data is now unavailable, so remove any references to the old data
        adapter.swapCursor(null);
    }
}
