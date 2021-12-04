package androidnews.kiloproject.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.BusUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.chad.library.adapter.base.BaseQuickAdapter;

import androidnews.kiloproject.system.AppConfig;

import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.activity.GalleyNewsActivity;
import androidnews.kiloproject.activity.NewsDetailActivity;
import androidnews.kiloproject.adapter.MainRvAdapter;
import androidnews.kiloproject.entity.data.BlockItem;
import androidnews.kiloproject.entity.data.CacheNews;
import androidnews.kiloproject.entity.net.GalleyData;
import androidnews.kiloproject.entity.net.NewMainListData;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static androidnews.kiloproject.entity.data.BlockItem.TYPE_KEYWORDS;
import static androidnews.kiloproject.entity.data.BlockItem.TYPE_SOURCE;
import static androidnews.kiloproject.entity.data.CacheNews.CACHE_HISTORY;
import static androidnews.kiloproject.system.AppConfig.CONFIG_AUTO_LOADMORE;
import static androidnews.kiloproject.system.AppConfig.GET_MAIN_DATA;
import static androidnews.kiloproject.system.AppConfig.LIST_TYPE_MULTI;
import static com.blankj.utilcode.util.CollectionUtils.isEmpty;

public class MainRvFragment extends BaseRvFragment {

    MainRvAdapter mAdapter;
    //    MainListData contents;
    List<NewMainListData> contents;

    private String CACHE_LIST_DATA;

    private int currentPage = 0;
    private int questPage = 20;

    private int adSize = 0;
    private int realPicCount = 0;

    private String lastSkipId;

    String typeStr;

    public static MainRvFragment newInstance(int type) {
        MainRvFragment f = new MainRvFragment();
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
                if (!TextUtils.isEmpty(json)) {
                    contents = gson.fromJson(json, new TypeToken<List<NewMainListData>>() {
                    }.getType());
                    if (contents != null && contents.size() > 0) {
                        try {
                            List<CacheNews> cacheNews = null;
                            try {
                                cacheNews = LitePal.where("type = ?", String.valueOf(CACHE_HISTORY)).find(CacheNews.class);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            if (cacheNews != null && cacheNews.size() > 0) {
                                for (Iterator<NewMainListData> it = contents.iterator(); it.hasNext(); ) {
                                    NewMainListData dataItem = it.next();
//                                for (NewMainListData dataItem : contents) {
                                    for (CacheNews cacheNew : cacheNews) {
                                        if (dataItem.getDocid().contains(cacheNew.getDocid())) {
                                            dataItem.setReaded(true);
                                            break;
                                        }
                                    }
                                }
                            }
                            e.onNext(true);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            e.onNext(false);
                        }
                    } else e.onNext(false);
                } else e.onNext(false);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean s) throws Exception {
                        if (s)
                            createAdapter();
                        MainRvFragment.super.onViewCreated(view, savedInstanceState);
                    }
                });

        if (AppConfig.listType == LIST_TYPE_MULTI)
            mRecyclerView.setLayoutManager(new GridLayoutManager(mActivity, 2));
        else
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
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
                    (AppConfig.isAutoRefresh) &&
                            (System.currentTimeMillis() - lastAutoRefreshTime > dividerAutoRefresh)) {
                refreshLayout.autoRefresh();
            }
        }
    }

    @Override
    public void requestData(int type) {
        if (TextUtils.isEmpty(typeStr)) return;
        String dataUrl = "";
        switch (type) {
            case TYPE_REFRESH:
                currentPage = 0;
            case TYPE_LOADMORE:
                dataUrl = GET_MAIN_DATA.replace("{typeStr}", typeStr).replace("{currentPage}", String.valueOf(currentPage));
                break;
        }
        EasyHttp.get(dataUrl)
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
                                    HashMap<String, List<NewMainListData>> retMap = null;
                                    List<NewMainListData> newList = new ArrayList<>();
                                    List<CacheNews> cacheNews = null;
                                    try {
                                        retMap = gson.fromJson(response,
                                                new TypeToken<HashMap<String, List<NewMainListData>>>() {
                                                }.getType());
                                        //设置头部轮播
                                        if (type == TYPE_REFRESH) {
                                            NewMainListData first = retMap.get(typeStr).get(0);
                                            first.setItemType(HEADER);
                                            checkAdPic(first);
                                        }
                                        cacheNews = LitePal.where("type = ?", String.valueOf(CACHE_HISTORY)).find(CacheNews.class);
                                    } catch (Exception e1) {
                                        e1.printStackTrace();
                                    }

                                    switch (type) {
                                        case TYPE_REFRESH:
                                            currentPage = 0;
                                            contents = new ArrayList<>();
                                            try {
                                                newList = retMap.get(typeStr);
                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }
                                            if (newList != null && newList.size() > 2) {
                                                newList = kickRepeat(newList);
                                                for (Iterator<NewMainListData> it = newList.iterator(); it.hasNext(); ) {
                                                    NewMainListData dataItem = it.next();
                                                    if (dataItem == null ||
                                                            checkExtra(dataItem) == 2 ||
                                                            TextUtils.equals(dataItem.getArticleType(), "webview") ||
                                                            ((!TextUtils.isEmpty(dataItem.getDigest())) && dataItem.getDigest().contains("下载地址")) ||
                                                            TextUtils.isEmpty(dataItem.getLtitle())
                                                    ) {
                                                        it.remove();
                                                        continue;
                                                    }
                                                    if (cacheNews != null && cacheNews.size() > 0) {
                                                        for (CacheNews cacheNew : cacheNews) {
                                                            if (dataItem.getDocid().contains(cacheNew.getDocid())) {
                                                                dataItem.setReaded(true);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    boolean isBlockBingo = false;
                                                    boolean isHasAd = false;        //防止有ad的item因为ad而被屏蔽视为普通条目

                                                    if (dataItem.getAds() == null) {
                                                        if (!isEmpty(AppConfig.blockList)) {
                                                            for (BlockItem blockItem : AppConfig.blockList) {
                                                                if (isBlockBingo)
                                                                    break;
                                                                switch (blockItem.getType()) {
                                                                    case TYPE_SOURCE:
                                                                        if (TextUtils.equals(dataItem.getSource(), blockItem.getText())) {
                                                                            it.remove();
                                                                            isBlockBingo = true;
                                                                        }
                                                                        break;
                                                                    case TYPE_KEYWORDS:
                                                                        if (dataItem.getTitle().contains(blockItem.getText())) {
                                                                            it.remove();
                                                                            isBlockBingo = true;
                                                                        }
                                                                        break;
                                                                }
                                                            }
                                                        }
                                                    } else if (dataItem.getAds().size() > 0) {
                                                        isHasAd = true;
                                                        for (Iterator<NewMainListData.AdsBean> adIt = dataItem.getAds().iterator(); adIt.hasNext(); ) {
                                                            NewMainListData.AdsBean adItem = adIt.next();
                                                            if (adItem.getSkipID().equals("00AJ0003|650617")) {   //王凯微笑
                                                                adIt.remove();
                                                                continue;
                                                            }
                                                            if (!isEmpty(AppConfig.blockList)) {
                                                                for (BlockItem blockItem : AppConfig.blockList) {
                                                                    if (blockItem.getType() == TYPE_KEYWORDS && adItem.getTitle().contains(blockItem.getText()))
                                                                        adIt.remove();
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (AppConfig.isBlockWeMedia && dataItem.getSource().endsWith("#"))
                                                        isBlockBingo = true;
                                                    if (isHasAd || (isGoodItem(dataItem) && !isBlockBingo)) {
                                                        contents.add(dataItem);
                                                    }
                                                }
                                            }
                                            e.onNext(true);
                                            try {
                                                SPUtils.getInstance().put(CACHE_LIST_DATA, gson.toJson(contents));
                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }
                                            break;
                                        case TYPE_LOADMORE:
                                            currentPage += questPage;
                                            try {
                                                newList.addAll(contents);
                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }
                                            boolean isAllSame = true;
                                            try {
                                                for (Iterator<NewMainListData> it = retMap.get(typeStr).iterator(); it.hasNext(); ) {
                                                    NewMainListData dataItem = it.next();
//                                                for (NewMainListData dataItem : retMap.get(typeStr)) {
                                                    boolean isSame = false;
//                                                if (TextUtils.isEmpty(newBean.getSource()) && !TextUtils.isEmpty(newBean.getTAG())){
                                                    if (!isGoodItem(dataItem) ||
                                                            checkExtra(dataItem) == 2 ||
                                                            TextUtils.equals(dataItem.getArticleType(), "webview") ||
                                                            dataItem.getDigest().contains("下载地址") ||
                                                            TextUtils.isEmpty(dataItem.getLtitle())
                                                    ) {
                                                        continue;
                                                    }
                                                    for (NewMainListData myBean : contents) {
                                                        if (TextUtils.equals(myBean.getDocid(), dataItem.getDocid())) {
                                                            isSame = true;
                                                            break;
                                                        }
                                                    }

                                                    if (!isSame) {
                                                        if (cacheNews != null && cacheNews.size() > 0) {
                                                            for (CacheNews cacheNew : cacheNews) {
                                                                if (dataItem.getDocid().contains(cacheNew.getDocid())) {
                                                                    dataItem.setReaded(true);
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        boolean isBlockBingo = false;
                                                        if (!isEmpty(AppConfig.blockList)) {
                                                            for (BlockItem blockItem : AppConfig.blockList) {
                                                                if (isBlockBingo)
                                                                    break;
                                                                switch (blockItem.getType()) {
                                                                    case TYPE_SOURCE:
                                                                        if (TextUtils.equals(dataItem.getSource(), blockItem.getText())) {
                                                                            it.remove();
                                                                            isBlockBingo = true;
                                                                        }
                                                                        break;
                                                                    case TYPE_KEYWORDS:
                                                                        if (dataItem.getTitle().contains(blockItem.getText())) {
                                                                            it.remove();
                                                                            isBlockBingo = true;
                                                                        }
                                                                        break;
                                                                }
                                                            }
                                                        }
                                                        if (AppConfig.isBlockWeMedia && dataItem.getSource().endsWith("#"))
                                                            isBlockBingo = true;
                                                        if (!isBlockBingo)
                                                            newList.add(dataItem);
                                                        isAllSame = false;
                                                    }
                                                }
                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }
                                            if (!isAllSame) {
                                                contents.clear();
                                                contents.addAll(newList);
                                            }
                                            e.onNext(true);
                                            break;
                                    }
                                    e.onComplete();
                                }
                            }).subscribeOn(Schedulers.computation())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Consumer<Boolean>() {
                                        @Override
                                        public void accept(Boolean o) throws Exception {
                                            if (mAdapter == null || type == TYPE_REFRESH) {
                                                createAdapter();
                                                lastAutoRefreshTime = System.currentTimeMillis();
                                                try {
                                                    if (AppConfig.isHaptic)
                                                        refreshLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                                    refreshLayout.finishRefresh(true);
                                                    if (!AppConfig.isDisNotice)
                                                        BusUtils.post(BUS_TAG_MAIN_SHOW, getString(R.string.load_success));
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
        if (contents == null || contents.size() < 1)
            return;
        realPicCount = 0;
        if (contents.get(0).getAds() != null) {
            adSize = contents.get(0).getAds().size();
        } else {
            adSize = 0;
        }
        mAdapter = new MainRvAdapter(mActivity, contents);
//        mAdapter.openLoadAnimation(BaseQuickAdapter.ALPHAIN);
        mAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (contents == null || contents.size() < 1)
                    return;
                NewMainListData item = contents.get(position);
                Intent intent = null;
                if (item.getItemType() == CELL) {
                    if (!TextUtils.isEmpty(item.getSkipID()) && TextUtils.equals(item.getSkipType(), "photoset")) {
                        String skipID = "";
                        String rawId;
                        rawId = item.getSkipID();
                        if (!TextUtils.isEmpty(rawId)) {
                            int index = rawId.lastIndexOf("|");
                            if (index != -1) {
                                skipID = rawId.substring(index - 4, rawId.length());
                                intent = new Intent(mActivity, GalleyNewsActivity.class);
                                intent.putExtra("skipID", skipID.replace("|", "/") + ".json");
                                if (!item.isReaded()) {
                                    item.setReaded(true);
                                    mAdapter.notifyItemChanged(position);
                                }
                                startActivity(intent);
                            } else {
                                BusUtils.post(BUS_TAG_MAIN_SHOW_ERROR, getString(R.string.server_fail));
                                return;
                            }
                        }
                    } else {
                        intent = new Intent(mActivity, NewsDetailActivity.class);
                        intent.putExtra("docid", item.getDocid().replace("_special", "").trim());
                        if (!item.isReaded()) {
                            item.setReaded(true);
                            mAdapter.notifyItemChanged(position);
                        }
                        startActivity(intent);
                    }
                }
            }
        });
        mAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                NewMainListData item = contents.get(position);
                showLongClickDialog(item.getTitle(),item.getUrl(),item.getSource());
                return true;
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

    private void checkAdPic(NewMainListData first) {
        if (first.getAds() != null) {       //剔除重复
            Iterator<NewMainListData.AdsBean> adIt = first.getAds().iterator();
            while (adIt.hasNext()) {
                NewMainListData.AdsBean bean =
                        adIt.next();
                if (!bean.getSkipID().contains("|") ||
                        TextUtils.equals(bean.getTitle(), (first.getTitle())) ||
                        !TextUtils.equals(bean.getImgsrc(), "bigimg")
                )
                    adIt.remove();
            }
            for (int i = 0; i < first.getAds().size(); i++) {       //搜索图片真实地址
                NewMainListData.AdsBean bean =
                        first.getAds().get(i);
                String skipID = bean.getSkipID().split("000")[1];
                requestAdPic(skipID, i);
            }
        }
    }

    private void requestAdPic(String skipID, int position) {
        EasyHttp.get("/photo/api/set/" + "000" + skipID.replace("|", "/") + ".json")
                .readTimeOut(30 * 1000)//局部定义读超时
                .writeTimeOut(30 * 1000)
                .connectTimeout(30 * 1000)
                .timeStamp(true)
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        if (refreshLayout != null)
                            try {
                                BusUtils.post(BUS_TAG_MAIN_SHOW_ERROR, getString(R.string.load_fail) + e.getMessage());
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                    }

                    @Override
                    public void onSuccess(String response) {
                        if (!TextUtils.isEmpty(response) || TextUtils.equals(response, "{}")) {
                            GalleyData galleyContent;
                            try {
                                galleyContent = gson.fromJson(response, GalleyData.class);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return;
                            }
                            if (contents != null && contents.size() > 0) {
                                NewMainListData bean = contents.get(0);
                                try {
                                    bean.getAds().get(position).setImgsrc(galleyContent.getPhotos().get(0).getImgurl());
                                    if (mAdapter != null)
                                        mAdapter.notifyItemChanged(0);
                                    realPicCount++;
                                    if (adSize > 0 && realPicCount == adSize) {
                                        String json = gson.toJson(contents);
                                        SPUtils.getInstance().put(CACHE_LIST_DATA, json);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
    }

    public static boolean isGoodItem(NewMainListData data) {
        if (data.getAds() != null && data.getAds().size() > 0)
            return true;
        String mTag = data.getTAGS();
        if (data.getPriority() > 500)       //某些霸屏的新闻
            return false;
        if (TextUtils.isEmpty(mTag))
            return true;
        else {
            if (AppConfig.goodTags != null)
                for (String gTag : AppConfig.goodTags) {
                    if (TextUtils.equals(gTag, mTag)) {
                        return true;
                    }
                }
            return false;
        }
    }

    private int checkExtra(NewMainListData data) {
        if (data.getSpecialextra() != null && data.getSpecialextra().size() > 1) {
            if (TextUtils.equals(data.getSkipID(), lastSkipId))
                return 2;
            else {
                data.setItemType(CELL_EXTRA);
                data.getSpecialextra().remove(0);
                lastSkipId = data.getSkipID();
                return 1;
            }
        }
        return 0;
    }

    private List<NewMainListData> kickRepeat(List<NewMainListData> newList) {
        if (TextUtils.equals(newList.get(1).getDocid(), newList.get(3).getDocid()))
            newList.remove(1);
        return newList;
    }
}
