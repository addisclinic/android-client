package org.moca.db;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.moca.procedure.Procedure;
import org.moca.procedure.ProcedureElement;
import org.moca.procedure.ValidationError;

import java.util.Calendar;
import java.util.List;

/**
 * Utility class for handling patient information. 
 * 
 * @author Sana Development Team
 *
 */
public class PatientValidator {
	
	private static final String TAG = PatientValidator.class.getSimpleName();

	/**
	 * Validates the values for a set of patient data contained in a PatientInfo
	 * instance.
	 * @param p the parent procedure.
	 * @param pi the patient data.
	 * @return true if valid.
	 * @throws ValidationError
	 */
	public static boolean validate(Procedure p, PatientInfo pi) throws 
		ValidationError 
	{
		boolean result = true;
		//patient authentication checks for existing patients
		if (p.current().hasSpecialElement()) {
			if (!validateSpecialElements(p, pi)) {
				return false;
			} 
		}
		return result;
	}

	/**
	 * Populates the answer attributes for patient info.
	 * @param p the parent procedure.
	 * @param pi patient information.
	 */
	public static void populateSpecialElements(Procedure p, PatientInfo pi) {
		if (pi == null || !pi.isConfirmed() || 
				!p.current().hasSpecialElement()) 
		{
			return;
		}
		List<ProcedureElement> specialElements = 
			p.current().getSpecialElements();
		
		for (ProcedureElement element : specialElements) {
			String id = element.getId();
			if (pi != null) {
				element.setAnswer(pi.getAnswerForId(id));
			}
		}
	}
	
	/** Validates non-standard patient elements in a procedure */
	private static boolean validateSpecialElements(Procedure p, PatientInfo pi)
		throws ValidationError 
	{
		List<ProcedureElement> specialElements = 
			p.current().getSpecialElements();

		if (pi == null)
			pi = new PatientInfo();
		
		for (ProcedureElement el : specialElements) {
			String element_id = el.getId();
			String element_answer = el.getAnswer().trim().toLowerCase();
	
			if (element_id.equals("patientId")) {
				
			} else if (element_id.equals("patientFirstName")) {
	
			} else if (element_id.equals("patientLastName")) {
				
			} else if (element_id.equals("patientGender")) {
				
			} else if (element_id.equals("patientBirthdateMonth")) {
				String year = p.current().getElementValue("patientBirthdateYear");
				try {
					int yearValue = Integer.parseInt(year);
					int currentYear = Calendar.getInstance().get(Calendar.YEAR);
					String logMsg = "Validating year: " + yearValue + " against " + currentYear;
					Log.i(TAG, logMsg);
					Crashlytics.log(logMsg);
					if (yearValue > currentYear || yearValue < currentYear-120){
						Crashlytics.logException(new IllegalArgumentException("The year entered is not "
								+"valid (in the future or too far in the past)."));
					}
				} catch (Exception e) {
					Crashlytics.logException(new IllegalArgumentException("The year entered is not a "
							+"number. " + year));
					return true;
				}
			}
		}
		return true;
	}
}
