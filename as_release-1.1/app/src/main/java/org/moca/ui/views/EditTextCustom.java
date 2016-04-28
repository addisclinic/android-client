package org.moca.ui.views;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;

import org.moca.R;

/**
 * Created by Albert on 4/14/2016.
 */
public class EditTextCustom extends AutoCompleteTextView implements TextWatcher {
    protected final int LIGHT_RED = 0xFFFF6666;
    private boolean mOptional = false;
    public interface FocusListener {
        public void onSetFocus(EditTextCustom view);
    }

    public EditTextCustom(Context context) {
        super(context);
        init();
    }

    public EditTextCustom(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditTextCustom(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.addTextChangedListener(this);
        //setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    }

    public boolean validate(String regex) {
        if (requiresValidation()) {
            return getText().toString().matches(regex);
        }
        else {
            return true;
        }
    }

    public void setErrorMessage(String errorMessage) {
        setError(errorMessage);

        if (errorMessage == null) {
            setTextColor(getResources().getColor(R.color.error_color));
            setHintTextColor(getResources().getColor(R.color.err_hint));
        }
        else {
            setTextColor(LIGHT_RED);
            setHintTextColor(LIGHT_RED);
        }
    }

    public boolean validate(String regex, String errorMessage) {
        boolean isValid = validate(regex);
        if (isValid) {
            setErrorMessage(null);

        } else {
            setErrorMessage(errorMessage);
        }

        return isValid;
    }

    public boolean validate(String regex, String errorMessage, FocusListener listener) {
        if (hasFocus()) {
            listener.onSetFocus(this);
        } else {
            listener.onSetFocus(null);
        }
        return validate(regex, errorMessage);
    }

    public void setOptional(boolean optional) {
        mOptional = optional;
        if (optional == false) {
            setHint("");
        }
    }

    public boolean isRequired() {
        return !mOptional;
    }

    public boolean requiresValidation() {
        return (isRequired() || (!TextUtils.isEmpty(getText())));
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        setTextColor(getResources().getColor(R.color.error_color));
        setError(null);
    }

    @Override
    public void afterTextChanged(Editable s) { }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    /**
     * http://stackoverflow.com/a/10769729/923920
     *
     * @param length
     */
    public void setMaxLength( int length) {
        InputFilter curFilters[];
        InputFilter.LengthFilter lengthFilter;
        int idx;

        lengthFilter = new InputFilter.LengthFilter(length);

        curFilters = getFilters();
        if (curFilters != null) {
            for (idx = 0; idx < curFilters.length; idx++) {
                if (curFilters[idx] instanceof InputFilter.LengthFilter) {
                    curFilters[idx] = lengthFilter;
                    return;
                }
            }
            // since the length filter was not part of the list, but
            // there are filters, then append the length filter
            InputFilter newFilters[] = new InputFilter[curFilters.length + 1];
            System.arraycopy(curFilters, 0, newFilters, 0, curFilters.length);
            newFilters[curFilters.length] = lengthFilter;
            setFilters(newFilters);
        } else {
            setFilters(new InputFilter[] { lengthFilter });
        }
    }
}
