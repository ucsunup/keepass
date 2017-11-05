package com.android.keepass.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.Database;
import com.android.keepass.GroupActivity;
import com.android.keepass.ProgressTask;
import com.android.keepass.R;
import com.android.keepass.app.App;
import com.android.keepass.compat.BackupManagerCompat;
import com.android.keepass.compat.EditorCompat;
import com.android.keepass.compat.StorageAF;
import com.android.keepass.database.edit.CreateDB;
import com.android.keepass.database.edit.FileOnFinish;
import com.android.keepass.database.edit.LoadDB;
import com.android.keepass.database.edit.OnFinish;
import com.android.keepass.database.edit.SetPassword;
import com.android.keepass.dialog.PasswordEncodingDialogHelper;
import com.android.keepass.fileselect.BrowserDialog;
import com.android.keepass.fileselect.RecentFileHistory;
import com.android.keepass.timeout.TimeoutHelper;
import com.android.keepass.utils.AnimatorUtils;
import com.android.keepass.utils.EmptyUtils;
import com.android.keepass.utils.Intents;
import com.android.keepass.utils.Interaction;
import com.android.keepass.utils.PermissionUtils;
import com.android.keepass.utils.UriUtil;
import com.android.keepass.utils.ViewUtils;

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
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String DEFAULT_FILENAME = Environment.getExternalStorageDirectory().getAbsolutePath() + "/keepass/keepass.kdbx";
    public static final String KEY_DEFAULT_FILENAME = "defaultFileName";
    public static final String ARG_FILENAME = "filename";
    public static final String ARG_KEYFILE = "keyfile";
    public static final String ARG_PASSWORD = "password";
    public static final String ARG_LAUNCH_IMMEDIATELY = "launchImmediately";
    public static final String ARG_MODEL = "model";

    private static final int REQUEST_CODE_FILE_BROWSE = 256;
    public static final int REQUEST_CODE_GET_CONTENT = 257;
    private static final int REQUEST_CODE_OPEN_DOC = 258;
    // TODO: Rename and change types of parameters
    private Uri mCurrentDbUri;
    private Uri mCurrentKeyUri;
    private Model mCurrentModel;

    private RecentFileHistory mFileHistory;
    private boolean mRecentMode = false;
    private boolean mRememberKeyfile;

    private OnFragmentInteractionListener mListener;
    private FileOnFinish mFinish;
    private TextView mPassView;
    private TextView mPassConfView;
    private TextView mKeyfileView;
    private ImageButton mFoldSwitch;
    private TextView mDbName;
    private TextView mDbPath;
    private CheckBox mSetDefaultDb;

    public enum Model {
        LOGIN, REGISTER
    }

    public FillUsrPwdFragment() {
        // Required empty public constructor
    }

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
        PermissionUtils.requestWriteExternalStoragePermission(getContext());
        mRememberKeyfile = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(getString(R.string.keyfile_key), getResources().getBoolean(R.bool.keyfile_default));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fill_usr_pwd, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Init Field From Arguments, setArguments will run here always.
        new InitTask().execute(getArguments());
    }

    @Override
    public void onResume() {
        super.onResume();
        // start app, then clear last timeout recorder.
        TimeoutHelper.clear(getContext());

        // Check to see if we need to change modes
//        if (mFileHistory.hasRecentFiles() != mRecentMode) {
//            // Restart the activity
//            Intent intent = getActivity().getIntent();
//            startActivity(intent);
//            getActivity().finish();
//        }
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
            case R.id.ok:
                // Check if set default database
                String newDefaultFileName;
                if (mSetDefaultDb.isChecked()) {
                    newDefaultFileName = mCurrentDbUri.toString();
                } else {
                    newDefaultFileName = "";
                }
                if (!TextUtils.isEmpty(newDefaultFileName)) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putString(KEY_DEFAULT_FILENAME, newDefaultFileName);
                    EditorCompat.apply(editor);
                }

                BackupManagerCompat backupManager = new BackupManagerCompat(getActivity());
                backupManager.dataChanged();

                // loadDatabase
                String pass = mPassView.getText().toString();
                String confpass = mPassConfView.getText().toString();

                // Verify that passwords match
                if (mCurrentModel == Model.REGISTER && !pass.equals(confpass)) {
                    // Passwords do not match
                    Toast.makeText(getContext(), R.string.error_pass_match, Toast.LENGTH_LONG).show();
                    return;
                }

                mCurrentKeyUri = UriUtil.parseDefaultFile(mKeyfileView.getText().toString());

                // Verify that a password or keyfile is set
                if (pass.length() == 0 && EmptyUtils.isNullOrEmpty(mCurrentKeyUri)) {
                    Toast.makeText(getContext(), R.string.error_nopass, Toast.LENGTH_LONG).show();
                    return;
                }

                boolean createSuccess = createDatabase(mCurrentDbUri, pass, mCurrentKeyUri, new LaunchGroupActivity(mCurrentDbUri));
                break;
            case R.id.db_name:
            case R.id.arrow_fold:
                View view = getView().findViewById(R.id.more_info);
                if (!mFoldSwitch.isSelected()) {
                    AnimatorUtils.foldView(view, 0, mFoldSwitch, true);
                    mFoldSwitch.setSelected(true);
                } else {
                    AnimatorUtils.foldView(view, 0, mFoldSwitch, false);
                    mFoldSwitch.setSelected(false);
                }
                break;
            default:
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
                if (resultCode == Activity.RESULT_OK && data != null) {
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
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionUtils.REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
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

    public boolean createDatabase(Uri dbUri, String pass, Uri keyfile, FileOnFinish onFinish) {
        // Make sure file name exists
        if (dbUri == null || TextUtils.isEmpty(dbUri.toString())) {
            Toast.makeText(getContext(), R.string.error_filename_required, Toast.LENGTH_LONG).show();
            return false;
        }

        // Try to create the file
        File file = new File(dbUri.getPath());
        try {
            if (file.exists()) {
                // If file exit, then enter
                loadDatabase(getContext(), dbUri, pass, keyfile);
                return true;
            }
            File parent = file.getParentFile();

            if (parent == null || (parent.exists() && !parent.isDirectory())) {
                Toast.makeText(getContext(), R.string.error_invalid_path, Toast.LENGTH_LONG).show();
                return false;
            }
            PermissionUtils.requestWriteExternalStoragePermission(getContext());
            if (!parent.exists()) {
                // Create parent dircetory
                if (!parent.mkdirs()) {
                    Toast.makeText(getContext(), R.string.error_could_not_create_parent, Toast.LENGTH_LONG).show();
                    return false;
                }
            }

            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), getText(R.string.error_file_not_create) + " " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return false;
        }

        // Create the new database
        CreateDB create = new CreateDB(getActivity(), dbUri.getPath(),
                new CollectPassword(onFinish, pass, keyfile),
                true);
        ProgressTask createTask = new ProgressTask(getContext(), create,
                R.string.progress_create);
        createTask.run();
        return true;
    }

    public void loadDatabase(Context context, Uri dbUri, String pass, Uri keyfile) {
        if (pass.length() == 0 && (keyfile == null || keyfile.toString().length() == 0)) {
            Toast.makeText(context, R.string.error_nopass, Toast.LENGTH_LONG).show();
            return;
        }

        // Clear before we load
        Database db = App.getDB();
        db.clear();

        // Clear the shutdown flag
        App.clearShutdown();

        Handler handler = new Handler();
        LoadDB task = new LoadDB(db, context, dbUri, pass, keyfile, new AfterLoad((Activity) context, handler, db));
        ProgressTask pt = new ProgressTask(context, task, R.string.loading_database);
        pt.run();
    }

    private class CollectPassword extends FileOnFinish {
        private Uri mKeyFile;
        private String mPassword;

        public CollectPassword(FileOnFinish finish, String password, Uri keyFile) {
            super(finish);
            mPassword = password;
            mKeyFile = keyFile;
        }

        @Override
        public void run() {
            if (mSuccess) {
                SetPassword sp = new SetPassword(getContext(), App.getDB(), mPassword, mKeyFile,
                        new AfterSave((FileOnFinish) this.mOnFinish, new Handler()));
                final ProgressTask pt = new ProgressTask(getContext(), sp, R.string.progress_create);
                boolean valid = sp.validatePassword(getContext(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pt.run();
                    }
                });
                if (valid) {
                    pt.run();
                }
            } else {
                super.run();
            }
        }
    }

    private final class AfterLoad extends OnFinish {
        private Database db;
        private Activity mActivity;

        public AfterLoad(Activity activity, Handler handler, Database db) {
            super(handler);

            this.mActivity = activity;
            this.db = db;
        }

        @Override
        public void run() {
            if (db.passwordEncodingError) {
                PasswordEncodingDialogHelper dialog = new PasswordEncodingDialogHelper();
                dialog.show(mActivity, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GroupActivity.Launch(mActivity);
                        mActivity.finish();
                    }

                });
            } else if (mSuccess) {
                GroupActivity.Launch(mActivity);
                mActivity.finish();
            } else {
                displayMessage(mActivity);
            }
        }
    }

    private class LaunchGroupActivity extends FileOnFinish {

        public LaunchGroupActivity(Uri filename) {
            super(null);
            setFilename(filename);
        }

        @Override
        public void run() {
            if (mSuccess) {
                // Add to recent files
                mFileHistory.createFile(getFilename(), getFilename());
                GroupActivity.Launch(getActivity());
                getActivity().finish();
                super.run();
            }
        }
    }

    private class AfterSave extends FileOnFinish {

        public AfterSave(FileOnFinish finish, Handler handler) {
            super(finish, handler);
        }

        @Override
        public void run() {
            if (mSuccess && mOnFinish != null) {
                mOnFinish.setResult(true);
            } else {
                displayMessage(getContext());
            }
            super.run();
        }
    }

    private void lookForOpenIntentsFilePicker(Uri dbUri) {
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

    private class InitTask extends AsyncTask<Bundle, Void, Integer> {
        String password = "";
        boolean launch_immediately = false;

        @Override
        protected Integer doInBackground(Bundle... args) {
            Bundle arguments = args[0];
            mCurrentDbUri = UriUtil.parseDefaultFile(arguments.getString(ARG_FILENAME));
            mCurrentKeyUri = UriUtil.parseDefaultFile(arguments.getString(ARG_KEYFILE));
            if (arguments.getBoolean(ARG_MODEL)) {
                mCurrentModel = Model.LOGIN;
            } else {
                mCurrentModel = Model.REGISTER;
            }
            password = arguments.getString(ARG_PASSWORD);
            launch_immediately = arguments.getBoolean(ARG_LAUNCH_IMMEDIATELY, false);
            if (mCurrentKeyUri == null || mCurrentKeyUri.toString().length() == 0) {
                mCurrentKeyUri = getKeyFile(mCurrentDbUri);
            }

            mFileHistory = App.getFileHistory();
            if (mFileHistory.hasRecentFiles()) {
                mRecentMode = true;
            }

            if (!TextUtils.isEmpty(mCurrentDbUri.toString())) {
                Uri dbUri = UriUtil.parseDefaultFile(mCurrentDbUri);
                String scheme = dbUri.getScheme();
                if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
                    mCurrentDbUri = dbUri;
                    File db = new File(mCurrentDbUri.getPath());
                    if (!db.exists()) {
                        mCurrentModel = Model.REGISTER;
                    } else {
                        mCurrentModel = Model.LOGIN;
                    }
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(Integer result) {
            if (result != null) {
                Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
                getActivity().finish();
                return;
            }

//            populateView();

            final View view = FillUsrPwdFragment.this.getView();
            TextInputLayout passInputLayout = view.findViewById(R.id.pass_password);
            mPassView = passInputLayout.getEditText();
            ViewUtils.updateToogleView(getContext(), passInputLayout, R.drawable.password_toggle, null);
            if (!TextUtils.isEmpty(password)) {
                mPassView.setText(password);
            }
            TextInputLayout passConfigInputLayout = view.findViewById(R.id.pass_conf_password);
            mPassConfView = passConfigInputLayout.getEditText();
            ViewUtils.updateToogleView(getContext(), passConfigInputLayout, R.drawable.password_toggle, null);
            if (mCurrentModel == Model.LOGIN) {
                view.findViewById(R.id.pass_conf_password).setVisibility(View.GONE);
            }
            TextInputLayout passKeyFileLayput = view.findViewById(R.id.pass_keyfile);
            mKeyfileView = passKeyFileLayput.getEditText();
            ViewUtils.updateToogleView(getContext(), passKeyFileLayput, R.drawable.file,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
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
                        }
                    });
            // Ok button
            Button okButton = view.findViewById(R.id.ok);
            okButton.setOnClickListener(FillUsrPwdFragment.this);
            mFoldSwitch = view.findViewById(R.id.arrow_fold);
            mFoldSwitch.setOnClickListener(FillUsrPwdFragment.this);

            mDbName = view.findViewById(R.id.db_name);
            mDbName.setOnClickListener(FillUsrPwdFragment.this);
            mDbPath = view.findViewById(R.id.db_filepath);
            String fileName = mCurrentDbUri.getLastPathSegment();
            mDbName.setText(fileName.substring(0, fileName.indexOf(".")));
            mDbPath.setText(mCurrentDbUri.getPath());
            mDbName.post(new Runnable() {
                @Override
                public void run() {
                    AnimatorUtils.foldView(view.findViewById(R.id.more_info), 0, mFoldSwitch, false);
                }
            });

            // update checkbox state
            mSetDefaultDb = view.findViewById(R.id.set_default_db);
            String oldDefaultDb = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(KEY_DEFAULT_FILENAME, DEFAULT_FILENAME);
            if (UriUtil.equalsDefaultfile(mCurrentDbUri, oldDefaultDb)) {
                mSetDefaultDb.setChecked(true);
            } else {
                mSetDefaultDb.setChecked(false);
            }

            retrieveSettings(getContext());

            if (launch_immediately) {
                loadDatabase(getContext(), mCurrentDbUri, password, mCurrentKeyUri);
            }
        }
    }

    private void retrieveSettings(Context context) {
        String defaultFilename = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_DEFAULT_FILENAME, "");
        if (!EmptyUtils.isNullOrEmpty(mCurrentDbUri.getPath()) && UriUtil.equalsDefaultfile(mCurrentDbUri, defaultFilename)) {
            mSetDefaultDb.setChecked(true);
        }
    }

    private Uri getKeyFile(Uri dbUri) {
        if (mRememberKeyfile) {
            return App.getFileHistory().getFileByName(dbUri);
        } else {
            return null;
        }
    }
}
