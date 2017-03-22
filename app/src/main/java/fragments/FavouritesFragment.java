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

import activities.MainActivity;
import activities.ProfileActivity;
import models.User;

public class FavouritesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, UserAdapter.clickListener {

    private UserAdapter userAdapter;
    private RecyclerView mRecyclerView;
    private ArrayList<User> userList, userListPersistent;
    private SwipeRefreshLayout swipeRefreshLayout;
    private refreshInterface refreshInterface = null;

    public FavouritesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favourites, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        userAdapter = new UserAdapter(getActivity());
        userAdapter.setListener(this);
        mRecyclerView.setAdapter(userAdapter);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        mRecyclerView.scrollToPosition(0);
        ((MainActivity) getActivity()).setFavouritesFragmentRefreshListener(new MainActivity.FavouritesFragmentListener() {
            @Override
            public void onSearch(String query) {
                setSearchResult(query);
            }
        });
        return rootView;
    }

    public void refreshList(ArrayList<User> list) {
        if (!list.isEmpty()) {
            userList = new ArrayList<>();
            userList.addAll(list);
            userListPersistent = new ArrayList<>();
            userListPersistent.addAll(userList);
            userAdapter.setUserList(userList, null);
        } else{
            userAdapter.clearUserList();
        }
    }

    public void setSearchResult(String query) {
        if (query.length() == 0) {
            resetSearchResults();
        } else {
            ArrayList<User> temp = new ArrayList<>();
            for (User user : userListPersistent) {
                if (user.getFirst_name().toLowerCase().contains(query.toLowerCase()) || user.getLast_name().toLowerCase().contains(query.toLowerCase()) || user.getEmail_address().toLowerCase().contains(query.toLowerCase()) || checkSkill(user, query)) {
                    temp.add(user);
                }
            }
            if (temp.size() > 0) {
                userList.clear();
                userList.addAll(temp);
                userAdapter.notifyDataSetChanged();
            } else {
                userList.clear();
                userAdapter.notifyDataSetChanged();
            }
        }
    }

    private boolean checkSkill(User user, String query) {
        ArrayList<String> skills = user.getSkills();
        for(int x = 0; x < skills.size(); x++)
        {
            String skill = skills.get(x);
            if(skill.toLowerCase().contains(query.toLowerCase()))
            {
                return true;
            }
        }
        return false;
    }

    public void resetSearchResults() {
        if(userList!=null)
        {
            userList.clear();
            userList.addAll(userListPersistent);
            userAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            refreshInterface = (refreshInterface) activity;
        } catch (ClassCastException e) {
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
        b.putInt("position", position);
        b.putString("email_address2", userList.get(position).getEmail_address()); //Your id
        intent.putExtras(b); //Put your id to your next Intent
        startActivity(intent);
    }

    public interface refreshInterface {
        void refreshFragment();
    }
}
