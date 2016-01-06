package org.moca.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import org.moca.R;

import java.util.Random;

/**
 * Created by Albert on 1/4/2016.
 */
public class BaseDialog extends DialogFragment {
    private static final String TAG = BaseDialog.class.getSimpleName();
    private static final String TITLE_TEXT_KEY = TAG + ".TITLE_TEXT_KEY";
    protected static final String MSG_BODY_KEY = TAG + ".MSG_BODY_KEY";
    private static final String ICON_KEY = TAG + ".ICON_KEY";
    private static final String LEFT_BUTTON_KEY = TAG + ".LEFT_BUTTON_KEY";
    private static final String MIDDLE_BUTTON_KEY = TAG + ".MIDDLE_BUTTON_KEY";
    private static final String RIGHT_BUTTON_KEY = TAG + ".RIGHT_BUTTON_KEY";
    private static final String MSG_BODY_ID_KEY = TAG + ".MSG_BODY_ID_KEY";
    private static final String DIALOG_ID_KEY = TAG + ".DIALOG_ID_KEY";
    private static final int SMALL_PHONE_SCREEN_HEIGHT = 960; // threshold level in pixel height
    private static final String HAS_HTML_KEY = TAG + ".HAS_HTML_KEY";

    public interface DialogListener {
        public void onOk(View v, String dialogId);
        public void onCancel(View v, String dialogId);
        public void onAuxiliary(View v, String dialogId);
    }


    public static BaseDialog getInstance(int title, int body) {
        BaseDialog frag = new BaseDialog();
        Bundle args = new Bundle();

        args.putInt(TITLE_TEXT_KEY, title);
        args.putInt(MSG_BODY_ID_KEY, body);
        frag.setArguments(args);
        return frag;
    }


    public BaseDialog() {
        super();
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
    }


    private DialogListener dialogListener;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            dialogListener = (DialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("parent activity must implement " + DialogListener.class.getSimpleName());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState) {
        View rootDialogView = inflater.inflate(R.layout.fragment_base_dialog, container, false);

        inflate(rootDialogView);
        inflateButtons(rootDialogView);
        return rootDialogView;
    }


    private void setMsgBodyText(TextView msg, String msgText) {
        // should the message body be interpreted as HTML ?
        if (getArguments().containsKey(HAS_HTML_KEY) && getArguments().getBoolean(HAS_HTML_KEY)) {
            msg.setText(Html.fromHtml(msgText));
            msg.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            msg.setText(msgText);
        }
        msg.setVisibility(View.VISIBLE);
    }

    private void setTitle(View rootDialogView) {
        TextView title = (TextView) rootDialogView.findViewById(R.id.title);

        if (title == null) return;

        if (getArguments().containsKey(TITLE_TEXT_KEY)) {
            title.setText(getString(getArguments().getInt(TITLE_TEXT_KEY, 0)));
        } else {
            title.setVisibility(View.GONE);
        }
    }

    protected void inflate(View rootDialogView) {
        setTitle(rootDialogView);

        TextView msg = (TextView) rootDialogView.findViewById(R.id.msg);
        if (getArguments().containsKey(MSG_BODY_KEY)) {
            setMsgBodyText(msg, getArguments().getString(MSG_BODY_KEY));

        } else if (getArguments().containsKey(MSG_BODY_ID_KEY)) {
            setMsgBodyText(msg, getString(getArguments().getInt(MSG_BODY_ID_KEY)));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Enforce policy that user has to acknowledge this problem alert - except for SplashActivity
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(false);
        WindowManager.LayoutParams wlp = getDialog().getWindow().getAttributes();
        wlp.gravity = Gravity.CENTER;
        getDialog().getWindow().setAttributes(wlp);
        setSize();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        dialogListener = null;
    }

    public void show() {

        if (getArguments().containsKey(DIALOG_ID_KEY)) {
            String dialogId = getArguments().getString(DIALOG_ID_KEY);
            BaseDialog dialogFragment = (BaseDialog)getFragmentManager().findFragmentByTag(dialogId);
            if (dialogFragment != null) {
                dialogFragment.dismiss();
            }
            super.show(getFragmentManager(), dialogId);
        }
    }

    protected void inflateButtons(View rootDialogView) {

        final String dialogId = getArguments().getString(DIALOG_ID_KEY);
        if (getArguments().containsKey(LEFT_BUTTON_KEY)) {
            TextView leftButton = (TextView) rootDialogView.findViewById(R.id.view_button);
            leftButton.setText(getArguments().getInt(LEFT_BUTTON_KEY));
            if (getArguments().containsKey(MIDDLE_BUTTON_KEY)) {
                leftButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            }
            leftButton.setVisibility(View.VISIBLE);
            leftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    dialogListener.onAuxiliary(v, dialogId);
                }
            });
        }



        if (getArguments().containsKey(RIGHT_BUTTON_KEY)) {
            TextView rightButton = (TextView) rootDialogView.findViewById(R.id.accept_button);
            rightButton.setText(getArguments().getInt(RIGHT_BUTTON_KEY));
            rightButton.setVisibility(View.VISIBLE);
            if (getArguments().containsKey(LEFT_BUTTON_KEY)) {
                rightButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            }
            rightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    dialogListener.onOk(v, dialogId);
                }
            });
        }

    }

    // dialog does not respect the height and width in the layout xml in JellyBean and possibly other
    // OS's, so set it here when necessary
    protected void setSize() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        //Log.w(TAG, "screen height: " + height);
        if (height > SMALL_PHONE_SCREEN_HEIGHT) {
            return;
        }
        // specific cases where the dialog sizes need to be manually overridden in code:
        // tablet landscape, and smaller size phones (Samsung S3 mini) in portrait or landscape
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        int orientation = getResources().getConfiguration().orientation;

        height = getDialogHeight(isTablet, orientation, height);
        width  = getDialogWidth(isTablet, orientation, width);

        getDialog().getWindow().setLayout(width, height);
    }

    protected int getDialogHeight(boolean isTablet, int orientation, int height) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isTablet) {
                return getDimension(DialogDimension.TABLET_HEIGHT_LANDSCAPE, height);
            }
            return getDimension(DialogDimension.SMALL_PHONE_HEIGHT_LANDSCAPE, height);
        }

        return getDimension(DialogDimension.SMALL_PHONE_HEIGHT_PORTRAIT, height);
    }

    protected int getDialogWidth(boolean isTablet, int orientation, int width) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isTablet) {
                return getDimension(DialogDimension.TABLET_WIDTH_LANDSCAPE, width);
            }
            return getDimension(DialogDimension.SMALL_PHONE_WIDTH_LANDSCAPE, width);
        }
        // use default width for now
        return width;
    }

    private int getDimension(DialogDimension type, int inputDimension) {

        switch (type) {
            case TABLET_HEIGHT_LANDSCAPE:
                return ((inputDimension * 5) >> 4);

            case TABLET_WIDTH_LANDSCAPE:
                return ((inputDimension  * 12) >> 4);

            case SMALL_PHONE_HEIGHT_LANDSCAPE:
                return  ((inputDimension * 8) >> 4);

            case SMALL_PHONE_WIDTH_LANDSCAPE:
                return ((inputDimension  * 11) >> 4);

            case SMALL_PHONE_HEIGHT_PORTRAIT:
                return ((inputDimension * 10) >> 5);

            case SMALL_PHONE_WIDTH_PORTRAIT:
                return ((inputDimension  * 15) >> 4);
        }
        return 0;
    }

    private enum DialogDimension {
        TABLET_HEIGHT_LANDSCAPE,
        TABLET_WIDTH_LANDSCAPE,
        SMALL_PHONE_HEIGHT_LANDSCAPE,
        SMALL_PHONE_WIDTH_LANDSCAPE,
        SMALL_PHONE_HEIGHT_PORTRAIT,
        SMALL_PHONE_WIDTH_PORTRAIT
    }

}
