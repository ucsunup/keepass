package com.keepassdroid.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.GroupActivity;
import com.keepassdroid.PasswordActivity;
import com.keepassdroid.ProgressTask;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.database.edit.CreateDB;
import com.keepassdroid.database.edit.FileOnFinish;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.SetPassword;
import com.keepassdroid.fileselect.BrowserDialog;
import com.keepassdroid.fileselect.FileSelectActivity;
import com.keepassdroid.fileselect.RecentFileHistory;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Interaction;
import com.keepassdroid.utils.UriUtil;

import java.io.File;
import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FillUsrPwdFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FillUsrPwdFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FillUsrPwdFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG_FILENAME = "filename";
    public static final String ARG_KEYFILE = "keyfile";
    public static final String ARG_MODEL = "model";

    private static final int REQUEST_CODE_FILE_BROWSE = 256;
    public static final int REQUEST_CODE_GET_CONTENT = 257;
    private static final int REQUEST_CODE_OPEN_DOC = 258;
    // TODO: Rename and change types of parameters
    private String mCurrentFileName;
    private String mCurrentKeyFile;
    private Model mCurrentModel;

    private RecentFileHistory mFileHistory;
    private boolean mRecentMode = false;

    private OnFragmentInteractionListener mListener;
    private FileOnFinish mFinish;
    private TextView mPassView;
    private TextView mPassConfView;
    private TextView mKeyfileView;
    private ImageButton mBrowseBtn;

    public enum Model {
        LOGIN, REGISTER
    }

    public FillUsrPwdFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param fileName Parameter 1.
     * @return A new instance of fragment FillUsrPwdFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FillUsrPwdFragment newInstance(String fileName, String keyFile, boolean login) {
        FillUsrPwdFragment fragment = new FillUsrPwdFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILENAME, fileName);
        args.putString(ARG_KEYFILE, keyFile);
        args.putBoolean(ARG_MODEL, login);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("heihei", "oncreate + " + getArguments().size());
        if (getArguments() != null) {
            mCurrentFileName = getArguments().getString(ARG_FILENAME);
            mCurrentKeyFile = getArguments().getString(ARG_KEYFILE);
            if (getArguments().getBoolean(ARG_MODEL)) {
                mCurrentModel = Model.LOGIN;
            } else {
                mCurrentModel = Model.REGISTER;
            }
        }

        mFileHistory = App.getFileHistory();
        if (mFileHistory.hasRecentFiles()) {
            mRecentMode = true;
        }

        if (!TextUtils.isEmpty(mCurrentFileName)) {
            Uri dbUri = UriUtil.parseDefaultFile(mCurrentFileName);
            String scheme = dbUri.getScheme();
            if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
                mCurrentFileName = dbUri.getPath();
                File db = new File(mCurrentFileName);
                if (!db.exists()) {
                    mCurrentModel = Model.REGISTER;
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d("heihei", "oncreateview " + mCurrentFileName);
        return inflater.inflate(R.layout.fragment_fill_usr_pwd, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("heihei", "onStart " + mCurrentFileName);
        View view = getView();
        mPassView = ((TextInputLayout) view.findViewById(R.id.pass_password)).getEditText();
        mPassConfView = ((TextInputLayout) view.findViewById(R.id.pass_conf_password)).getEditText();
        if (mCurrentModel == Model.LOGIN) {
            view.findViewById(R.id.pass_conf_password).setVisibility(View.GONE);
        }
        mKeyfileView = ((TextInputLayout) view.findViewById(R.id.pass_keyfile)).getEditText();
        mBrowseBtn = (ImageButton) view.findViewById(R.id.browse_button);
        mBrowseBtn.setOnClickListener(this);
        // Ok button
        Button okButton = (Button) view.findViewById(R.id.ok);
        okButton.setOnClickListener(this);
        // Cancel button
        Button cancel = (Button) view.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see if we need to change modes
        if (mFileHistory.hasRecentFiles() != mRecentMode) {
            // Restart the activity
            Intent intent = getActivity().getIntent();
            startActivity(intent);
            getActivity().finish();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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
        switch (v.getId()) {
            case R.id.browse_button:
                if (StorageAF.useStorageFramework(getContext())) {
                    Intent i = new Intent(StorageAF.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, REQUEST_CODE_OPEN_DOC);
                } else {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");

                    try {
                        startActivityForResult(i, REQUEST_CODE_GET_CONTENT);
                    } catch (ActivityNotFoundException e) {
                        lookForOpenIntentsFilePicker(UriUtil.parseDefaultFile(mKeyfileView.getText().toString()));
                    }
                }
                break;
            case R.id.ok:
                String pass = mPassView.getText().toString();
                String confpass = mPassConfView.getText().toString();

                Log.d("heihei", "pass = " + pass + ", congPass = " + confpass + ", currentFile = " + mCurrentFileName);
                // Verify that passwords match
                if (mCurrentModel == Model.REGISTER && !pass.equals(confpass)) {
                    // Passwords do not match
                    Toast.makeText(getContext(), R.string.error_pass_match, Toast.LENGTH_LONG).show();
                    return;
                }

                mCurrentKeyFile = mKeyfileView.getText().toString();

                // Verify that a password or keyfile is set
                if (pass.length() == 0 && EmptyUtils.isNullOrEmpty(mCurrentKeyFile)) {
                    Toast.makeText(getContext(), R.string.error_nopass, Toast.LENGTH_LONG).show();
                    return;
                }

                Log.d("heihei", "createDatabse will do");
                boolean createSuccess = createDatabase(mCurrentFileName, pass, UriUtil.parseDefaultFile(mCurrentKeyFile),
                        new ProcessInitDB(pass, UriUtil.parseDefaultFile(mCurrentKeyFile),
                                new LoadDatabase(UriUtil.parseDefaultFile(mCurrentKeyFile), pass, mCurrentKeyFile, null)));
                break;
            case R.id.cancel:
                if (mFinish != null) {
                    mFinish.run();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_FILE_BROWSE:
                if (resultCode == Activity.RESULT_OK) {
                    String filename = data.getDataString();
                    if (filename != null) {
                        mKeyfileView.setText(filename);
                    }
                }
                break;
            case REQUEST_CODE_GET_CONTENT:
            case REQUEST_CODE_OPEN_DOC:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            if (requestCode == REQUEST_CODE_GET_CONTENT) {
                                uri = UriUtil.translate(getContext(), uri);
                            }
                            String path = uri.toString();
                            if (path != null) {
                                mKeyfileView.setText(path);

                            }
                        }
                    }
                }
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
    public interface OnFragmentInteractionListener {
        // TODO: get current database filepath
        String getCurrentFileName();
    }

    public boolean createDatabase(String filename, String pass, Uri keyfile, FileOnFinish onFinish) {
        // Make sure file name exists
        if (TextUtils.isEmpty(filename)) {
            Toast.makeText(getContext(), R.string.error_filename_required, Toast.LENGTH_LONG).show();
            return false;
        }

        // Try to create the file
        File file = new File(filename);
        try {
            if (file.exists()) {
                // If file exit, then enter
                Log.d("heihei", "file.exists");
                PasswordActivity.loadDatabase(getContext(), Uri.fromFile(file), pass, keyfile);
                getActivity().finish();
                return true;
            }
            File parent = file.getParentFile();

            if (parent == null || (parent.exists() && !parent.isDirectory())) {
                Toast.makeText(getContext(), R.string.error_invalid_path, Toast.LENGTH_LONG).show();
                return false;
            }

            if (!parent.exists()) {
                // Create parent dircetory
                if (!parent.mkdirs()) {
                    Toast.makeText(getContext(), R.string.error_could_not_create_parent, Toast.LENGTH_LONG).show();
                    return false;
                }
            }

            file.createNewFile();
        } catch (IOException e) {
            Toast.makeText(getContext(), getText(R.string.error_file_not_create) + " " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return false;
        }

        // Create the new database
        CreateDB create = new CreateDB(getActivity(), filename,
                new LaunchGroupActivity(filename, onFinish), true);
        ProgressTask createTask = new ProgressTask(getActivity(), create, R.string.progress_create);
        createTask.run();
        return true;
    }

    private class LaunchGroupActivity extends FileOnFinish {
        private Uri mUri;

        public LaunchGroupActivity(String filename, FileOnFinish onFinish) {
            super(onFinish);
            mUri = UriUtil.parseDefaultFile(filename);
        }

        @Override
        public void run() {
            Log.d("heihei", "LaunchGroupActivity: mSuccess = " + mSuccess);
            if (mSuccess) {
                super.run();
                // Add to recent files
                mFileHistory.createFile(mUri, getFilename());
                GroupActivity.Launch(getActivity());
                getActivity().finish();
            }
        }
    }

    private class AfterSave extends OnFinish {
        private FileOnFinish mFinish;

        public AfterSave(FileOnFinish finish, Handler handler) {
            super(finish, handler);
            mFinish = finish;
        }

        @Override
        public void run() {
            Log.d("heihei", "FillUsrPWdFragment: AfterSave: mSuccess = " + mSuccess);
            if (mSuccess) {
                if (mFinish != null) {
                    mFinish.setFilename(UriUtil.parseDefaultFile(mCurrentKeyFile));
                }
            } else {
                displayMessage(getContext());
            }
            super.run();
        }
    }

    private class ProcessInitDB extends FileOnFinish {
        private String mPass;

        public ProcessInitDB(String pass, Uri keyfile, FileOnFinish onFinish) {
            super(onFinish);
            mPass = pass;
            setFilename(keyfile);
        }

        @Override
        public void run() {
            Log.d("heihei", "FillUsrPWdFragment: ProcessInitDb: mSuccess = " + mSuccess);
            if (mSuccess) {
                SetPassword sp = new SetPassword(getContext(), App.getDB(), mPass, getFilename(),
                        new AfterSave(mFinish, new Handler()));
                final ProgressTask pt = new ProgressTask(getContext(), sp, R.string.saving_database);
                boolean valid = sp.validatePassword(getContext(), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pt.run();
                    }
                });

                if (valid) {
                    pt.run();
                }
            }
        }
    }

    private class LoadDatabase extends FileOnFinish {
        private String mPass;
        private String mKeyFile;

        public LoadDatabase(Uri fileName, String pass, String keyFile, FileOnFinish finish) {
            super(finish);
            setFilename(fileName);
            this.mPass = pass;
            this.mKeyFile = keyFile;
        }

        @Override
        public void run() {
            Log.d("heihei", "LoadDatabse: filename = " + getFilename() + ", pass = " + mPass + ", mKEeyFile  = " + mKeyFile
                    + ", mSuccess = " + mSuccess);
            if (mSuccess) {
                PasswordActivity.loadDatabase(getContext(), getFilename(), mPass, mKeyFile);
            }
            super.run();
        }
    }

    private void lookForOpenIntentsFilePicker(Uri dbUri) {
        Log.d("heihei", "LoadDatabase: dbUri = " + dbUri.toString());
        if (Interaction.isIntentAvailable(getContext(), Intents.OPEN_INTENTS_FILE_BROWSE)) {
            Intent i = new Intent(Intents.OPEN_INTENTS_FILE_BROWSE);

            // Get file path parent if possible
            try {
                if (dbUri != null && dbUri.toString().length() > 0) {
                    if (dbUri.getScheme().equals("file")) {
                        File keyfile = new File(dbUri.getPath());
                        File parent = keyfile.getParentFile();
                        if (parent != null) {
                            i.setData(Uri.parse("file://" + parent.getAbsolutePath()));
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore
            }

            try {
                startActivityForResult(i, REQUEST_CODE_FILE_BROWSE);
            } catch (ActivityNotFoundException e) {
                showBrowserDialog();
            }
        } else {
            showBrowserDialog();
        }
    }

    private void showBrowserDialog() {
        BrowserDialog diag = new BrowserDialog(getContext());
        diag.show();
    }
}
