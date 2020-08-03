package com.ucsunup.keepass.fragment;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ucsunup.keepass.R;
import com.ucsunup.keepass.compat.StorageAF;
import com.ucsunup.keepass.fileselect.BrowserDialog;
import com.ucsunup.keepass.utils.Intents;
import com.ucsunup.keepass.utils.Interaction;
import com.ucsunup.keepass.utils.Util;
import com.ucsunup.keepass.utils.ViewUtils;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AdvancedDbEditFragment.OnDbEditListener} interface
 * to handle interaction events.
 * Use the {@link AdvancedDbEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * @author ucsunup
 */
public class AdvancedDbEditFragment extends DialogFragment implements View.OnClickListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_DB_PATH = "db_path";

    private String mDbPath;

    public static final int FILE_BROWSE = 1;
    public static final int GET_CONTENT = 2;
    public static final int OPEN_DOC = 3;

    private OnDbEditListener mListener;

    private TextInputLayout mInputLayout;
    private View.OnClickListener mOnToggleViewClickListener;

    public AdvancedDbEditFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dbPath Parameter 1: DB Path
     * @return A new instance of fragment AdvancedDbEditFragment.
     */
    public static AdvancedDbEditFragment newInstance(String dbPath) {
        AdvancedDbEditFragment fragment = new AdvancedDbEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DB_PATH, dbPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDbPath = getArguments().getString(ARG_DB_PATH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_advanced_db_edit, container, false);
        mOnToggleViewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StorageAF.useStorageFramework(getActivity())) {
                    Intent i = new Intent(StorageAF.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    startActivityForResult(i, OPEN_DOC);
                } else {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    try {
                        startActivityForResult(i, GET_CONTENT);
                    } catch (ActivityNotFoundException e) {
                        lookForOpenIntentsFilePicker();
                    } catch (SecurityException e) {
                        lookForOpenIntentsFilePicker();
                    }
                }
            }
        };
        mInputLayout = (TextInputLayout) rootView.findViewById(R.id.file_layout);
        mInputLayout.getEditText().setText(mDbPath);
        ViewUtils.updateToogleView(getContext(),
                mInputLayout, R.drawable.file, mOnToggleViewClickListener);

        // Button
        rootView.findViewById(R.id.open).setOnClickListener(this);
        rootView.findViewById(R.id.create).setOnClickListener(this);
        return rootView;
    }

    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        if (requestCode == OPEN_DOC) {
            String path = data.getDataString();
            mInputLayout.getEditText().setText(path);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDbEditListener) {
            mListener = (OnDbEditListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        String filename = mInputLayout.getEditText().getText().toString();
        switch (v.getId()) {
            case R.id.open:
                dismiss();
                mListener.openDatabase(filename, null);
                break;
            case R.id.create:
                dismiss();
                mListener.createDatabase(filename, null);
                break;
            default:
                break;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDbEditListener {
        /**
         * Create new database callback
         *
         * @param fileName
         * @param keyFile
         */
        void createDatabase(String fileName, String keyFile);

        /**
         * Open database callback
         *
         * @param fileName
         * @param keyFile
         */
        void openDatabase(String fileName, String keyFile);
    }

    /**
     * Make a chooser for user to pick
     */
    private void lookForOpenIntentsFilePicker() {
        if (Interaction.isIntentAvailable(getActivity(), Intents.OPEN_INTENTS_FILE_BROWSE)) {
            Intent i = new Intent(Intents.OPEN_INTENTS_FILE_BROWSE);
            i.setData(Uri.parse("file://" + Util.getEditText(getActivity(), R.id.file_filename)));
            try {
                startActivityForResult(i, FILE_BROWSE);
            } catch (ActivityNotFoundException e) {
                showBrowserDialog();
            }
        } else {
            showBrowserDialog();
        }
    }

    /**
     * Show browser dialog
     */
    private void showBrowserDialog() {
        new BrowserDialog(getActivity()).show();
    }
}
