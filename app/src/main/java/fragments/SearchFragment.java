package fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.outsauce.R;
import adapters.UserAdapter;

import java.util.ArrayList;

import activities.ProfileActivity;
import models.User;

public class SearchFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, UserAdapter.clickListener {

    private UserAdapter userAdapter;
    private RecyclerView mRecyclerView;
    private ArrayList<User> userList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private refreshInterface refreshInterface = null;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        userAdapter = new UserAdapter(getActivity());
        userAdapter.setListener(this);
        mRecyclerView.setAdapter(userAdapter);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        mRecyclerView.scrollToPosition(0);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        return rootView;
    }

    public void refreshList(ArrayList<User> list, String query) {
        if (!list.isEmpty()) {
            userList = new ArrayList<>();
            userList.addAll(list);
            userAdapter.setUserList(userList, query);
        }
    }

    public void clearList() {
        userList.clear();
        userAdapter.clearUserList();
        userAdapter.notifyDataSetChanged();
    }

    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder1) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            int adapterPosition = viewHolder.getAdapterPosition();
            userList.remove(adapterPosition);
            userAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            refreshInterface = (refreshInterface) activity;
        } catch (ClassCastException e) {
            // unable to cast to correct type, i.e. does not implement the
            // required listener
            throw new ClassCastException(
                    activity.getClass().toString()
                            + " does not implement the DetailsFragment.DetailsFragmentListener interface.");
        }
    }

    @Override
    public void onRefresh() {
        if (refreshInterface != null)
            refreshInterface.refreshFragment();
        if (swipeRefreshLayout.isRefreshing())
            swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void itemClicked(int position) {
        Intent intent = new Intent(getActivity(), ProfileActivity.class);
        Bundle b = new Bundle();
        b.putBoolean("fromSearch", true);
        b.putString("email_address2", userList.get(position).getEmail_address()); //Your id
        intent.putExtras(b); //Put your id to your next Intent
        startActivity(intent);
    }

    public interface refreshInterface {
        void refreshFragment();
    }
}
