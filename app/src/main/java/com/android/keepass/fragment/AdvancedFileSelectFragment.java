package com.android.keepass.fragment;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.keepass.R;
import com.android.keepass.PasswordActivity;
import com.android.keepass.app.App;
import com.android.keepass.compat.StorageAF;
import com.android.keepass.fileselect.BrowserDialog;
import com.android.keepass.fileselect.RecentFileHistory;
import com.android.keepass.utils.Intents;
import com.android.keepass.utils.Interaction;
import com.android.keepass.utils.Util;
import com.android.keepass.view.FileNameView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AdvancedFileSelectFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AdvancedFileSelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AdvancedFileSelectFragment extends Fragment implements View.OnClickListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private RecyclerView mList;
    private BaseListAdapter mAdapter;
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

        mList = (RecyclerView) view.findViewById(R.id.file_list);
        mList.setLayoutManager(new LinearLayoutManager(getContext()));
        mList.setItemAnimator(new DefaultItemAnimator());

        mFilename = (EditText) view.findViewById(R.id.file_filename);
        // Open button
        Button openButton = (Button) view.findViewById(R.id.open);
        openButton.setOnClickListener(this);
        // Create button
        Button createButton = (Button) view.findViewById(R.id.create);
        createButton.setOnClickListener(this);
        ((FileNameView) view.findViewById(R.id.file_select)).setOnToggleViewClickListener(new View.OnClickListener() {
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
        });

        fillData();

        registerForContextMenu(mList);
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
        mAdapter.notifyDataSetChanged();
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
        mAdapter = new BaseListAdapter(mFileHistory.getDbList());
        mAdapter.setOnClickListener(new CardListAdapter.OnClickListener() {
            @Override
            public void onClick(View v, int position) {
                onListItemClick(v, position);
            }
        });
        mList.setAdapter(mAdapter);
    }

    protected void onListItemClick(View v, int position) {

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
