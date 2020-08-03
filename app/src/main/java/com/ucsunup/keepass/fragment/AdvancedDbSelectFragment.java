package com.ucsunup.keepass.fragment;

import android.content.Context;
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
import android.widget.TextView;

import com.ucsunup.keepass.R;
import com.ucsunup.keepass.PasswordActivity;
import com.ucsunup.keepass.app.App;
import com.ucsunup.keepass.fileselect.RecentFileHistory;
import com.ucsunup.keepass.utils.Constants;
import com.ucsunup.keepass.utils.Util;

import com.github.clans.fab.FloatingActionButton;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AdvancedDbSelectFragment.OnDbSelectListener} interface
 * to handle interaction events.
 * Use the {@link AdvancedDbSelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * @author ucsunup
 */
public class AdvancedDbSelectFragment extends Fragment implements View.OnClickListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnDbSelectListener mListener;
    private RecyclerView mList;
    private BaseListAdapter mAdapter;

    private RecentFileHistory mFileHistory;
    private boolean mRecentMode = false;
    private static final int CMENU_CLEAR = Menu.FIRST;

    public AdvancedDbSelectFragment() {
        // Required empty public constructor
    }

    public static AdvancedDbSelectFragment newInstance(String param1, String param2) {
        AdvancedDbSelectFragment fragment = new AdvancedDbSelectFragment();
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

        FloatingActionButton editDbBtn = view.findViewById(R.id.db_edit);
        editDbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdvancedDbEditFragment dialog = AdvancedDbEditFragment.newInstance(Constants.DEFAULT_FILENAME);
                AdvancedDbSelectFragment.this.getChildFragmentManager().beginTransaction().add(dialog, "edit_db").commit();
            }
        });

        fillData();

        registerForContextMenu(mList);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDbSelectListener) {
            mListener = (OnDbSelectListener) context;
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
    public interface OnDbSelectListener {
        /**
         * Open database callback
         *
         * @param fileName
         * @param keyFile
         */
        void openDatabase(String fileName, String keyFile);
    }

    private void fillData() {
        // Set the initial value of the filename
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
