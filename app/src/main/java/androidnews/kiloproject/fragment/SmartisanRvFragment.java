package androidnews.kiloproject.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import org.litepal.LitePal;

import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.activity.SmartisanDetailActivity;
import androidnews.kiloproject.adapter.SmartisanAdapter;
import androidnews.kiloproject.entity.data.CacheNews;
import androidnews.kiloproject.entity.net.SmartisanListData;
import androidnews.kiloproject.system.AppConfig;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static androidnews.kiloproject.entity.data.CacheNews.CACHE_HISTORY;
import static androidnews.kiloproject.system.AppConfig.CONFIG_AUTO_LOADMORE;
import static androidnews.kiloproject.system.AppConfig.HOST_SMARTISAN;
import static androidnews.kiloproject.system.AppConfig.GET_SMARTISAN_LOAD_MORE;
import static androidnews.kiloproject.system.AppConfig.GET_SMARTISAN_REFRESH;
import static androidnews.kiloproject.system.AppConfig.LIST_TYPE_MULTI;
import static androidx.recyclerview.widget.OrientationHelper.HORIZONTAL;

public class SmartisanRvFragment extends BaseRvFragment {

    //    MainListData contents;
    SmartisanListData.DataBean contents;

    CardView header;

    private String CACHE_LIST_DATA;

    String typeStr;

    private String lastItemId = "";

    public static SmartisanRvFragment newInstance(int type) {
        SmartisanRvFragment f = new SmartisanRvFragment();
        Bundle b = new Bundle();
        b.putInt("type", type);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        int position = 999;
        if (args != null) {
            position = args.getInt("type");
        }
        typeStr = getResources().getStringArray(R.array.address)[position];
        this.CACHE_LIST_DATA = typeStr + "_data";

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                String json = SPUtils.getInstance().getString(CACHE_LIST_DATA, "");
                if (TextUtils.isEmpty(json) || TextUtils.equals(json, "[]")) {
                    e.onNext(false);
                } else {
                    contents = gson.fromJson(json, SmartisanListData.DataBean.class);
                    if (contents != null) {
                        if (contents.getList() != null) {
                            List<CacheNews> cacheNews = null;
                            try {
                                cacheNews = LitePal.where("type = ?", String.valueOf(CACHE_HISTORY)).find(CacheNews.class);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            if (cacheNews != null && cacheNews.size() > 0)
                                for (SmartisanListData.DataBean.ListBean data : contents.getList()) {
                                    for (CacheNews cacheNew : cacheNews) {
                                        if (TextUtils.equals(data.getId() + "", cacheNew.getDocid())) {
                                            data.setReaded(true);
                                            break;
                                        }
                                    }
                                }
                            e.onNext(true);
                        }
                    } else {
                        e.onNext(false);
                    }
                }
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean o) throws Exception {
                        if (o)
                            createAdapter();
                        else
                            contents = new SmartisanListData.DataBean();
                        SmartisanRvFragment.super.onViewCreated(view, savedInstanceState);
                    }
                });
        if (AppConfig.listType == LIST_TYPE_MULTI)
            mRecyclerView.setLayoutManager(new GridLayoutManager(mActivity, 2));
        else
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        mRecyclerView.setHasFixedSize(true);
//        mRecyclerView.addItemDecoration(new DividerItemDecoration(mActivity,HORIZONTAL));

        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                requestData(TYPE_REFRESH);
            }
        });
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                requestData(TYPE_LOADMORE);
            }
        });
    }

    protected void onFragmentVisibleChange(boolean isVisible) {
        if (isVisible) {
            if (contents == null ||
                    contents.getList() == null ||
                    (AppConfig.isAutoRefresh) &&
                            (System.currentTimeMillis() - lastAutoRefreshTime > dividerAutoRefresh)) {
                refreshLayout.autoRefresh();
            }
        }
    }

    @Override
    protected void onFragmentFirstVisible() {
        super.onFragmentFirstVisible();
    }

    @Override
    public void requestData(int type) {
        String dataUrl = "";
        switch (type) {
            case TYPE_REFRESH:
                dataUrl = GET_SMARTISAN_REFRESH
                        .replace("{cate_id}", typeStr);
                break;
            case TYPE_LOADMORE:
                dataUrl = GET_SMARTISAN_LOAD_MORE
                        .replace("{cate_id}", typeStr)
                        .replace("{last_id}", lastItemId);
                break;
        }
        EasyHttp.get(HOST_SMARTISAN + dataUrl)
                .readTimeOut(30 * 1000)//局部定义读超时
                .writeTimeOut(30 * 1000)
                .connectTimeout(30 * 1000)
                .timeStamp(true)
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        if (refreshLayout != null) {
                            switch (type) {
                                case TYPE_REFRESH:
                                    if (AppConfig.isHaptic)
                                        refreshLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                    refreshLayout.finishRefresh(false);
                                    break;
                                case TYPE_LOADMORE:
                                    if (SPUtils.getInstance().getBoolean(CONFIG_AUTO_LOADMORE) && mAdapter != null)
                                        mAdapter.loadMoreFail();
                                    else
                                        refreshLayout.finishLoadMore(false);
                                    break;
                            }
                            if (isAdded())
                                ToastUtils.showShort(getResources().getString(R.string.load_fail) + e.getMessage());
                        }
                    }

                    @Override
                    public void onSuccess(String response) {
                        if (!TextUtils.isEmpty(response) || TextUtils.equals(response, "{}")) {
                            Observable.create(new ObservableOnSubscribe<Boolean>() {
                                @Override
                                public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                                    SmartisanListData.DataBean newData = null;
                                    List<CacheNews> cacheNews = null;
                                    try {
                                        newData = gson.fromJson(response, SmartisanListData.class).getData();
                                        cacheNews = LitePal.where("type = ?", String.valueOf(CACHE_HISTORY)).find(CacheNews.class);
                                    } catch (Exception e1) {
                                        e1.printStackTrace();
                                    }
                                    switch (type) {
                                        case TYPE_REFRESH:
                                            if (newData == null)
                                                return;
                                            if (cacheNews != null && cacheNews.size() > 0)
                                                for (SmartisanListData.DataBean.ListBean data : newData.getList()) {
                                                    for (CacheNews cacheNew : cacheNews) {
                                                        if (TextUtils.equals(data.getId() + "", cacheNew.getDocid())) {
                                                            data.setReaded(true);
                                                            break;
                                                        }
                                                    }
                                                }
                                            contents.setList(newData.getList());
                                            e.onNext(true);
                                            try {
                                                SPUtils.getInstance().put(CACHE_LIST_DATA, gson.toJson(newData));
                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }
                                            break;
                                        case TYPE_LOADMORE:
                                            try {
                                                if (cacheNews != null && cacheNews.size() > 0 && newData.getList() != null) {
                                                    for (SmartisanListData.DataBean.ListBean data : newData.getList()) {
                                                        for (CacheNews cacheNew : cacheNews) {
                                                            if (TextUtils.equals(data.getId() + "", cacheNew.getDocid())) {
                                                                data.setReaded(true);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                                for (SmartisanListData.DataBean.ListBean data : newData.getList()) {
                                                    contents.getList().addAll(newData.getList());
                                                }
                                                e.onNext(true);
                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }
                                            break;
                                    }
                                    e.onComplete();
                                }
                            }).subscribeOn(Schedulers.computation())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Consumer<Boolean>() {
                                        @Override
                                        public void accept(Boolean o) throws Exception {
                                            if (o)
                                                lastItemId = contents.getList().get(contents.getList().size() - 1).getId();
                                            if (mAdapter == null || type == TYPE_REFRESH) {
                                                createAdapter();
                                                lastAutoRefreshTime = System.currentTimeMillis();
                                                try {
                                                    if (AppConfig.isHaptic)
                                                        refreshLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                                    refreshLayout.finishRefresh(true);
                                                    if (!AppConfig.isDisNotice)
                                                        SnackbarUtils.with(refreshLayout)
                                                                .setMessage(getString(R.string.load_success))
                                                                .show();
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            } else if (type == TYPE_LOADMORE) {
                                                mAdapter.notifyDataSetChanged();
                                                if (SPUtils.getInstance().getBoolean(CONFIG_AUTO_LOADMORE))
                                                    mAdapter.loadMoreComplete();
                                                else
                                                    try {
                                                        refreshLayout.finishLoadMore(true);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                            }
                                        }
                                    });
                        } else {
                            loadFailed(type);
                        }
                    }
                });
    }

    private void createAdapter() {
        if (contents == null || contents.getList() == null)
            return;
        mAdapter = new SmartisanAdapter(mActivity, contents.getList());
//        mAdapter.openLoadAnimation(BaseQuickAdapter.ALPHAIN);
        mAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                SmartisanListData.DataBean.ListBean bean = contents.getList().get(position);
                Intent intent = new Intent(getActivity(), SmartisanDetailActivity.class);
                intent.putExtra("title", bean.getTitle());
                intent.putExtra("id", bean.getId());
                intent.putExtra("url", bean.getOrigin_url());
                intent.putExtra("img", bean.getPrepic1());
                intent.putExtra("source", bean.getSite_info().getName());
                if (!bean.isReaded()) {
                    bean.setReaded(true);
                    mAdapter.notifyItemChanged(position);
                }
                startActivity(intent);
                bean.setReaded(true);
            }
        });

        if (mRecyclerView == null)
            return;
        mRecyclerView.setAdapter(mAdapter);
        if (SPUtils.getInstance().getBoolean(CONFIG_AUTO_LOADMORE)) {
            mAdapter.setPreLoadNumber(PRE_LOAD_ITEM);
            mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
                @Override
                public void onLoadMoreRequested() {
                    requestData(TYPE_LOADMORE);
                }
            }, mRecyclerView);
            mAdapter.disableLoadMoreIfNotFullPage();
            refreshLayout.setEnableLoadMore(false);
        }
    }
}
