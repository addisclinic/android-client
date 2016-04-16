package org.moca.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.moca.R;

/**
 * Created by Albert on 4/14/2016.
 */
public class ObfuscatedPasswordView extends RelativeLayout {

    private EditTextCustom passwordView;
    private TextView obfuscate;
    private boolean show = false;

    public ObfuscatedPasswordView(Context context) {
        super(context);
        inflate();
    }

    public ObfuscatedPasswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate();
    }

    public ObfuscatedPasswordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate();
    }

    private void inflate() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.account_password, null);

        passwordView = (EditTextCustom) view.findViewById(R.id.passwordView);
        obfuscate = (TextView) view.findViewById(R.id.obfuscatePassword);
        addView(view);
        setListeners();
    }

    public String getText() {
        return passwordView.getText().toString();
    }

    public void setText(String password) {
        passwordView.setText(password);
    }

    public void setError(String error) {
        passwordView.setError(error);
    }

    private void setListeners () {
        obfuscate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Typeface typeface = passwordView.getTypeface();
                if (show) {
                    passwordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
                    passwordView.setTransformationMethod(new PasswordTransformationMethod());
                    obfuscate.setText(getResources().getString(R.string.show));
                } else {
                    passwordView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    passwordView.setTransformationMethod(null);
                    obfuscate.setText(getResources().getString(R.string.hide));
                }

                show = !show;
                passwordView.setTypeface(typeface);
                passwordView.setSelection(passwordView.getText().length());
            }
        });
    }

    public void setShow(boolean show) {
        this.show = show;
        Typeface typeface = passwordView.getTypeface();
        if(this.show) {
            passwordView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_CLASS_TEXT);
            passwordView.setTypeface(typeface);
            obfuscate.setText(getResources().getString(R.string.hide));
        }
    }

    public boolean getShow() {
        return show;
    }

    public void setOptional(boolean optional) {
        passwordView.setOptional(optional);
    }

    public boolean validate(String passwordRegex, String errorMessage) {
        return passwordView.validate(passwordRegex, errorMessage);
    }
}
