package org.moca.db;

import org.moca.db.MocaDB.ProcedureSQLFormat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Utility class which provides access to Procedure xml text content handled by 
 * the ProcedureProvider.
 * 
 * @author Sana Development Team
 *
 */
public class ProcedureDAO {
	
	/**
	 * Fetches the xml text content for a procedure
	 * 
	 * @param context the current Context
	 * @param procedure the Uri of the Procedure which will be retrieved
	 * @return the xml text content of the procedure 
	 */
	public static String getXMLForProcedure(Context context, Uri procedure) {
		Cursor cursor = null;
		String procedureXml = "";
		try {
			cursor = context.getContentResolver().query(procedure, 
					new String [] { ProcedureSQLFormat.PROCEDURE }, 
					null, null, null);        
			cursor.moveToFirst();
			procedureXml = cursor.getString(cursor.getColumnIndex(
					ProcedureSQLFormat.PROCEDURE));
			cursor.deactivate();
		} catch (Exception e) {
			EventDAO.logException(context, e);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return procedureXml;
	}
}
