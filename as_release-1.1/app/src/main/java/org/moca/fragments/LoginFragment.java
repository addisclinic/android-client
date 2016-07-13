package org.moca.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;

import org.moca.AddisApp;
import org.moca.R;
import org.moca.model.LoginResult;
import org.moca.net.AddisCallback;
import org.moca.ui.views.ObfuscatedPasswordView;
import org.moca.util.UserSettings;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LoginFragmentListener} interface
 * to handle interaction events.
 * Use the {@link LoginFragment#getInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends DialogFragment {
    private static final String TAG = LoginFragment.class.getSimpleName();
    private static final String USERNAME_KEY = TAG + ".USERNAME_KEY";
    private static final String PASSWORD_KEY = TAG + ".PASSWORD_KEY";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private int mParam2;

    private LoginFragmentListener mListener;


    // UI references.
    private AutoCompleteTextView emailView;
    private ObfuscatedPasswordView passwordView;
    private ProgressBar mProgressView;
    private View mLoginFormView;
    private AppCompatButton loginButton;

    public LoginFragment() {
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoginFragment getInstance(int param2) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

            mParam2 = getArguments().getInt(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_login, container, false);
        mProgressView = (ProgressBar) rootView.findViewById(R.id.login_progress);
        emailView = (AutoCompleteTextView) rootView.findViewById(R.id.input_email);
        passwordView = (ObfuscatedPasswordView) rootView.findViewById(R.id.input_password);
        mLoginFormView = rootView.findViewById(R.id.login_form);
        loginButton = (AppCompatButton) rootView.findViewById(R.id.btn_login);
        loginButton.setEnabled(true);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginButton.setEnabled(false);
                attemptLogin();
            }
        });
        showCredentials(savedInstanceState);
        return rootView;
    }


    private void showCredentials(Bundle savedState) {
        if (savedState != null && savedState.containsKey(USERNAME_KEY)) {
            emailView.setText(savedState.getString(USERNAME_KEY));
            passwordView.setText(savedState.getString(PASSWORD_KEY));
        } else  {
            UserSettings settings = new UserSettings();

            emailView.setText(settings.getUsername());
            passwordView.setText(settings.getPassword());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof LoginFragmentListener) {
            mListener = (LoginFragmentListener) activity;
        } else {
            throw new RuntimeException(activity.getClass().getSimpleName()
                    + " must implement " + LoginFragmentListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface LoginFragmentListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }



    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int animTime = getResources().getInteger(android.R.integer.config_longAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(animTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(animTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        emailView.setAdapter(adapter);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {


        // Reset errors.
        emailView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        String email = emailView.getText().toString();
        String password = passwordView.getText();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            loginButton.setEnabled(true);
        } else {
            loginButton.setEnabled(false);
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            AddisApp.getInstance().getNetworkClient().login(callback, email, password);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return !TextUtils.isEmpty(email);
    }

    private boolean isPasswordValid(String password) {

        return password.length() >= 8 && password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
    }

    private void showLoginFail(final String errMsg) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setMessage("Login Failed: " + errMsg)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                loginButton.setEnabled(true);
                            }
                        }).create();
                dialog.show();
            }
        });
    }

    private AddisCallback<LoginResult> callback = new AddisCallback<LoginResult>() {
        @Override
        public void onResponse(Call<LoginResult> call, Response<LoginResult> response) {
            showProgress(false);
            if (response.body().status.equals("SUCCESS")) {
                dismiss();
                loginButton.setEnabled(true);
            } else if (response.code() == 200) {
                showLoginFail(String.valueOf(response.body().data));
            } else {
                showLoginFail(String.valueOf(String.valueOf(response.code())));
            }
        }

        @Override
        public void onFailure(Call<LoginResult> call, Throwable t) {
            showProgress(false);
            showLoginFail(t.getMessage());
        }
    };
}
