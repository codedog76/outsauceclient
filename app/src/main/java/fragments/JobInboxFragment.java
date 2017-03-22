package fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.outsauce.R;

import java.util.ArrayList;

import activities.JobActivity;
import adapters.JobAdapter;
import activities.MainActivity;
import models.Job;


/**
 * A simple {@link Fragment} subclass.
 */
public class JobInboxFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, JobAdapter.clickListener {

    private JobAdapter jobAdapter;
    private RecyclerView mRecyclerView;
    private ArrayList<Job> jobList, jobListPersistent;
    private SwipeRefreshLayout swipeRefreshLayout;
    private refreshInterface refreshInterface = null;

    public JobInboxFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_job_inbox, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        jobAdapter = new JobAdapter(getActivity());
        jobAdapter.setListener(this);
        jobList = new ArrayList<>();
        jobListPersistent = new ArrayList<>();
        mRecyclerView.setAdapter(jobAdapter);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        mRecyclerView.scrollToPosition(0);
        ((MainActivity) getActivity()).setJobInboxFragmentListener(new MainActivity.JobInboxFragmentListener() {
            @Override
            public void onSearch(String query) {
                setSearchResult(query);
            }

            @Override
            public void setPosition(int pos) {
                mRecyclerView.scrollToPosition(pos);
            }
        });
        return rootView;
    }

    public void refreshList(ArrayList<Job> list) {
        jobList = new ArrayList<>();
        jobListPersistent = new ArrayList<>();
        if (!list.isEmpty()) {
            jobList.addAll(list);
            jobListPersistent.addAll(jobList);
        }
        jobAdapter.setJobList(jobList);
    }

    public void setSearchResult(String query) {
        if (query.length() == 0) {
            resetSearchResults();
        } else {
            ArrayList<Job> temp = new ArrayList<>();
            for (Job job : jobListPersistent) {
                if (job.getFirst_name().toLowerCase().contains(query.toLowerCase()) || job.getLast_name().toLowerCase().contains(query.toLowerCase()) || job.getJob_title().toLowerCase().contains(query.toLowerCase()) || job.getJob_description().toLowerCase().contains(query.toLowerCase())) {
                    temp.add(job);
                }
            }
            if (temp.size() > 0) {
                jobList.clear();
                jobList.addAll(temp);
                jobAdapter.notifyDataSetChanged();
            } else {
                jobList.clear();
                jobAdapter.notifyDataSetChanged();
            }
        }
    }

    public void resetSearchResults() {
        jobList.clear();
        jobList.addAll(jobListPersistent);
        jobAdapter.notifyDataSetChanged();
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
        Job job = jobList.get(position);
        if (job != null) {
            Intent intent = new Intent(getActivity(), JobActivity.class);
            Bundle b = new Bundle();
            b.putInt("position", position);
            b.putBoolean("notification", false);
            b.putString("from", "inbox");
            b.putSerializable("job", job);
            intent.putExtras(b);
            startActivity(intent);
        }
    }

    public interface refreshInterface {
        void refreshFragment();
    }
}
