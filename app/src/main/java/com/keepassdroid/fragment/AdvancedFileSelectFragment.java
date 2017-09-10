package com.keepassdroid.fragment;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.keepass.R;
import com.keepassdroid.PasswordActivity;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.fileselect.BrowserDialog;
import com.keepassdroid.fileselect.RecentFileHistory;
import com.keepassdroid.intents.Intents;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Interaction;
import com.keepassdroid.utils.UriUtil;
import com.keepassdroid.utils.Util;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AdvancedFileSelectFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AdvancedFileSelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AdvancedFileSelectFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private ListView mList;
    private ListAdapter mAdapter;
    private EditText mFilename;

    private RecentFileHistory mFileHistory;
    private boolean mRecentMode = false;
    private static final int CMENU_CLEAR = Menu.FIRST;

    public static final int FILE_BROWSE = 1;
    public static final int GET_CONTENT = 2;
    public static final int OPEN_DOC = 3;

    public AdvancedFileSelectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AdvancedFileSelectFragment.
     */
    public static AdvancedFileSelectFragment newInstance(String param1, String param2) {
        AdvancedFileSelectFragment fragment = new AdvancedFileSelectFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advanced_file_select, container, false);
        mFileHistory = App.getFileHistory();
        if (mFileHistory.hasRecentFiles()) {
            mRecentMode = true;
        }

        mList = (ListView) view.findViewById(R.id.file_list);

        mList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        onListItemClick((ListView) parent, v, position, id);
                    }
                }
        );

        mFilename = (EditText) view.findViewById(R.id.file_filename);
        // Open button
        Button openButton = (Button) view.findViewById(R.id.open);
        openButton.setOnClickListener(this);
        // Create button
        Button createButton = (Button) view.findViewById(R.id.create);
        createButton.setOnClickListener(this);
        ImageButton browseButton = (ImageButton) view.findViewById(R.id.browse_button);
        browseButton.setOnClickListener(this);

        fillData();

        registerForContextMenu(mList);

        // Load default database
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String fileName = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, "");

        if (fileName.length() > 0) {
            Uri dbUri = UriUtil.parseDefaultFile(fileName);
            String scheme = dbUri.getScheme();

            if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
                String path = dbUri.getPath();
                File db = new File(path);

                if (db.exists()) {
                    try {
                        PasswordActivity.Launch(getActivity(), path);
                    } catch (Exception e) {
                        // Ignore exception
                    }
                }
            } else {
                try {
                    PasswordActivity.Launch(getActivity(), dbUri.toString());
                } catch (Exception e) {
                    // Ignore exception
                }
            }
        }
        return view;
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
            case R.id.open:
                String filename = Util.getEditText(getActivity(),
                        R.id.file_filename);

                mListener.openDatabase(filename, null);
                break;
            case R.id.create:
                filename = Util.getEditText(getActivity(), R.id.file_filename);
                mListener.setPwdForNewDatabase(filename, null);
                break;
            case R.id.browse_button:
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
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, CMENU_CLEAR, 0, R.string.remove_from_filelist);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        if (item.getItemId() == CMENU_CLEAR) {
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

            TextView tv = (TextView) acmi.targetView;
            String filename = tv.getText().toString();
            new AsyncTask<String, Void, Void>() {
                protected java.lang.Void doInBackground(String... args) {
                    String filename = args[0];
                    mFileHistory.deleteFile(Uri.parse(args[0]));
                    return null;
                }

                protected void onPostExecute(Void v) {
                    refreshList();
                }
            }.execute(filename);
            return true;
        }

        return false;
    }

    private void refreshList() {
        ((BaseAdapter) mAdapter).notifyDataSetChanged();
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
        // TODO: Update argument type and name
        void setPwdForNewDatabase(String fileName, String keyFile);

        void openDatabase(String fileName, String keyFile);
    }

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

    private void showBrowserDialog() {
        BrowserDialog diag = new BrowserDialog(getActivity());
        diag.show();
    }

    private void fillData() {
        // Set the initial value of the filename
        mFilename.setText(PasswordActivity.DEFAULT_FILENAME);
        mAdapter = new ArrayAdapter<String>(getContext(), R.layout.file_row, R.id.file_filename, mFileHistory.getDbList());
        mList.setAdapter(mAdapter);
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {

        new AsyncTask<Integer, Void, Void>() {
            String fileName;
            String keyFile;

            protected Void doInBackground(Integer... args) {
                int position = args[0];
                fileName = mFileHistory.getDatabaseAt(position);
                keyFile = mFileHistory.getKeyfileAt(position);
                return null;
            }

            protected void onPostExecute(Void v) {
                mListener.openDatabase(fileName, keyFile);
            }
        }.execute(position);
    }
}
