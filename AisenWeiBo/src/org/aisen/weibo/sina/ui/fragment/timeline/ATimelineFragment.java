package org.aisen.weibo.sina.ui.fragment.timeline;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.m.component.container.FragmentContainerActivity;
import com.m.network.task.TaskException;
import com.m.support.adapter.ABaseAdapter.AbstractItemView;
import com.m.support.paging.IPaging;
import com.m.ui.fragment.ARefreshFragment;
import com.m.ui.fragment.AStripTabsFragment;

import org.aisen.weibo.sina.R;
import org.aisen.weibo.sina.base.AppContext;
import org.aisen.weibo.sina.base.AppSettings;
import org.aisen.weibo.sina.support.bean.TimelineGroupBean;
import org.aisen.weibo.sina.support.paging.TimelinePagingProcessor;
import org.aisen.weibo.sina.support.utils.AisenUtils;
import org.aisen.weibo.sina.ui.activity.basic.MainActivity;
import org.aisen.weibo.sina.ui.fragment.basic.AWeiboRefreshListFragment;
import org.aisen.weibo.sina.ui.fragment.basic.BizFragment;
import org.aisen.weibo.sina.ui.widget.TimelinePicsView;
import org.sina.android.bean.StatusContent;
import org.sina.android.bean.StatusContents;
import org.sina.android.bean.WeiBoUser;

import java.util.ArrayList;
import java.util.List;

/**
 * 微博列表基类
 * 
 * @author wangdan
 * 
 */
public abstract class ATimelineFragment extends AWeiboRefreshListFragment<StatusContent, StatusContents>
											implements OnItemLongClickListener, BizFragment.OnLikeCallback {

	private TimelineGroupBean mGroupBean;

	private WeiBoUser loggedIn;
	
	@Override
	public void onCreate(Bundle savedInstanceSate) {
        loggedIn = AppContext.getUser();

		mGroupBean = savedInstanceSate == null ? (TimelineGroupBean) getArguments().getSerializable("bean")
				   : (TimelineGroupBean) savedInstanceSate.getSerializable("bean");
		
		super.onCreate(savedInstanceSate);
	}

	@Override
	protected void layoutInit(LayoutInflater inflater, Bundle savedInstanceSate) {
		super.layoutInit(inflater, savedInstanceSate);
		
        getRefreshView().setOnItemLongClickListener(this);

		setHasOptionsMenu(true);
	}
	
	@Override
	protected void config(ARefreshFragment.RefreshConfig config) {
		super.config(config);

        if (getGroup() != null)
            config.saveLastPositionKey = AisenUtils.getUserKey(getGroup().getType(), loggedIn);
		config.emptyLabel = getString(R.string.empty_status);
        config.animEnable = false;
	}

    @Override
    protected String loadingLabel() {
        return String.format(getString(R.string.loading_status), AppSettings.getTimelineCount());
    }

    @Override
    protected String loadDisabledLabel() {
        return getString(R.string.disable_status);
    }

    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable("bean", mGroupBean);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) getRefreshView();
		int index = listView.getHeaderViewsCount();

		if (getAdapterItems().size() > 0 && position >= index) {
			StatusContent status = getAdapterItems().get(position - index);

//			TimelineCommentsFragment.launch(this, status);
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (view.findViewById(R.id.btnMenus) != null)
			view.findViewById(R.id.btnMenus).performClick();
		return true;
	}

	@Override
	protected IPaging<StatusContent, StatusContents> configPaging() {
		return new TimelinePagingProcessor();
	}

	@Override
	protected AbstractItemView<StatusContent> newItemView() {
		return new TimelineItemView(this, true);
	}

	public TimelineGroupBean getGroup() {
		return mGroupBean;
	}

	static final int[] imageResArr = new int[] { R.id.img01, R.id.img02, R.id.img03, R.id.img04, R.id.img05, R.id.img06, R.id.img07, R.id.img08,
			R.id.img09, R.id.imgPhoto };

	@Override
	protected int[] recyleImageViewRes() {
		return imageResArr;
	}

	/**
	 * 如果当前的视图是Pager显示的视图，释放后会造成视图闪烁的情况出现
	 */
	@Override
	public void onMovedToScrapHeap(View view) {
//	    Logger.v("当前展示的是" + current + "，不释放视图");
        Fragment fragment = getPagerCurrentFragment();
        if (fragment == this) {
            return;
        }

        super.onMovedToScrapHeap(view);
	}

	/**
	 * 如果Pager显示的不是当前视图，则不刷新视图
	 */
	@Override
	public void refreshUI() {
        // 如果当前的Pager显示的不是当前的Fragment，就不刷新
        Fragment fragment = getPagerCurrentFragment();
        if (fragment != null && fragment != this)
            return;

		super.refreshUI();
	}

	protected Fragment getPagerCurrentFragment() {
		if (getActivity() == null)
			return null;

        // 首页
        Fragment aFragment = null;
        if (getActivity() instanceof MainActivity) {
            aFragment = MainActivity.getContentFragment((MainActivity) getActivity());
        }
        // 其他页面
        else if (getActivity() instanceof FragmentContainerActivity) {
            aFragment = getActivity().getFragmentManager().findFragmentByTag(FragmentContainerActivity.FRAGMENT_TAG);
        }

		if (aFragment instanceof AStripTabsFragment) {
            AStripTabsFragment fragment = (AStripTabsFragment) aFragment;
            return fragment.getCurrentFragment();
		}

		return null;
	}

	@Override
	protected boolean releaseView(View view) {
		TimelinePicsView picsView = (TimelinePicsView) view.findViewById(R.id.layPicturs);
		if (picsView != null)
			picsView.release();

		return super.releaseView(view);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// 微博被删除了
		if (resultCode == Activity.RESULT_OK && requestCode == 1000) {
			String statusId = data.getStringExtra("status");
			if (!TextUtils.isEmpty(statusId)) {
				for (int i = 0; i < getAdapterItems().size(); i++) {
					if (statusId.equals(getAdapterItems().get(i).getId())) {
						getAdapterItems().remove(i);

						getAdapter().notifyDataSetChanged();
						break;
					}
				}
			}
		}
	}


    @Override
	public boolean onToolbarDoubleClick() {
        //		if (getActivity() instanceof AViewpagerActivity) {
//			AViewpagerActivity activity = (AViewpagerActivity) getActivity();
//			if (activity.getCurrentFragment() == this)
//				return super.onToolbarDoubleClick();
//			else
//				return false;
//		}
        Fragment aFragment = getPagerCurrentFragment();
        if (aFragment == this) {
            ListView listView = (ListView) getRefreshView();
            listView.setSelectionFromTop(0, 0);
            requestDataDelay(200);

            return true;
        }

		return super.onToolbarDoubleClick();
	}

    @Override
    public void onLikeRefreshUI() {
        refreshUI();
    }

    public abstract class TimelineTask extends PagingTask<Void, Void, StatusContents> {

        public TimelineTask(ARefreshFragment.RefreshMode mode) {
            super("TimelineTask", mode);
        }

        @Override
        protected List<StatusContent> parseResult(StatusContents result) {
            return result.getStatuses();
        }

        @Override
        protected void onFailure(TaskException exception) {
            super.onFailure(exception);

            if (!isContentEmpty())
                showMessage(exception.getMessage());
        }

        @Override
        protected boolean handleResult(RefreshMode mode, List<StatusContent> datas) {
            // 如果是重置或者刷新数据，加载数据大于分页大小，则清空之前的数据
            if (mode == ARefreshFragment.RefreshMode.refresh) {
                // 目前微博加载分页大小是默认大小
                if (datas.size() >= AppSettings.getTimelineCount()) {
                    setAdapterItems(new ArrayList<StatusContent>());
                    return true;
                }
            }

            return super.handleResult(mode, datas);
        }

        @Override
        protected void onSuccess(StatusContents result) {
            if (result == null)
                return;
            super.onSuccess(result);

            ListView listView = (ListView) getRefreshView();
            // 2014-08-27 当刷新列表时，返回最上面
            if (mode == ARefreshFragment.RefreshMode.reset && getTaskCount(getTaskId()) > 1)
                listView.setSelectionFromTop(0, 0);
        }

    }

}
