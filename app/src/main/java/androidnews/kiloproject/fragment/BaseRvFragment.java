package androidnews.kiloproject.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

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
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.RomUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.Utils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.ethanhua.skeleton.Skeleton;
import com.ethanhua.skeleton.SkeletonScreen;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.google.gson.Gson;
import com.gyf.immersionbar.ImmersionBar;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.ArrayList;

import androidnews.kiloproject.R;
import androidnews.kiloproject.entity.data.BlockItem;
import androidnews.kiloproject.entity.net.NewMainListData;
import androidnews.kiloproject.system.AppConfig;
import androidnews.kiloproject.system.base.BaseLazyFragment;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static androidnews.kiloproject.entity.data.BlockItem.TYPE_KEYWORDS;
import static androidnews.kiloproject.entity.data.BlockItem.TYPE_SOURCE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_AUTO_LOADMORE;
import static androidnews.kiloproject.system.AppConfig.LIST_TYPE_SINGLE;
import static androidnews.kiloproject.system.AppConfig.isHighRam;


public abstract class BaseRvFragment extends BaseLazyFragment {

    RecyclerView mRecyclerView;
    SmartRefreshLayout refreshLayout;
    SkeletonScreen skeletonScreen;
    BaseQuickAdapter mAdapter;

    Gson gson = new Gson();

    protected long lastAutoRefreshTime = 0;
    public static final long dividerAutoRefresh = 3 * 60 * 1000;
    public static final int PRE_LOAD_ITEM = 5;

    public static final int TYPE_LOADMORE = 1000;
    public static final int TYPE_REFRESH = 1001;

    public static final String BUS_TAG_MAIN_SHOW = "BUS_TAG_MAIN_SHOW";
    public static final String BUS_TAG_MAIN_SHOW_ERROR = "BUS_TAG_MAIN_SHOW_ERROR";

    public static final int HEADER = 1;
    public static final int CELL = 0;
    public static final int CELL_EXTRA = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recyclerview, container, false);
        refreshLayout = (SmartRefreshLayout) view.findViewById(R.id.refreshLayout);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv_content);
        if (isHighRam) {
            mRecyclerView.setItemViewCacheSize(15);
            mRecyclerView.setDrawingCacheEnabled(true);
            mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        }
        if (ImmersionBar.hasNotchScreen(mActivity))
            refreshLayout.setHeaderInsetStart(ConvertUtils.px2dp(ImmersionBar.getStatusBarHeight(mActivity)));
        refreshLayout.setHeaderTriggerRate(0.75f);
        if (!RomUtils.isMeizu() && AppConfig.isShowSkeleton && !(this instanceof ZhihuRvFragment) && AppConfig.listType == LIST_TYPE_SINGLE)
            skeletonScreen = Skeleton.bind(mRecyclerView)
                    .adapter(mAdapter)
                    .shimmer(true)      // whether show shimmer animation.                      default is true
                    .count(10)          // the recycler view item count.                        default is 10
                    .color(R.color.awesome_background)       // the shimmer color.                                   default is #a2878787
                    .angle(20)          // the shimmer angle.                                   default is 20;
                    .duration(1200)     // the shimmer animation duration.                      default is 1000;
                    .frozen(true)      // whether frozen recyclerView during skeleton showing  default is true;
                    .load(R.layout.list_item_skeleton_news)
                    .show();
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        return view;
    }

    public abstract void requestData(int type);

    protected void loadFailed(int type) {
        switch (type) {
            case TYPE_REFRESH:
                if (AppConfig.isHaptic)
                    refreshLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                refreshLayout.finishRefresh(false);
                BusUtils.post(BUS_TAG_MAIN_SHOW_ERROR,getString(R.string.server_fail));
                break;
            case TYPE_LOADMORE:
                if (SPUtils.getInstance().getBoolean(CONFIG_AUTO_LOADMORE))
                    mAdapter.loadMoreFail();
                else
                    refreshLayout.finishLoadMore(false);
                BusUtils.post(BUS_TAG_MAIN_SHOW_ERROR,getString(R.string.server_fail));
                break;
        }
    }

    public void startLowMemory() {
        if (mRecyclerView != null) {
            mRecyclerView.setItemViewCacheSize(5);
            mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        }
    }

    public void showRefresh(){
        refreshLayout.autoRefresh();
    }

    protected void showLongClickDialog(String title,String link,String source) {
        final String[] items = {
                getResources().getString(R.string.action_link)
                , getResources().getString(R.string.action_block_source)
                , getResources().getString(R.string.action_block_keywords)
        };
        new AlertDialog.Builder(mActivity).setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                ClipboardManager cm = (ClipboardManager) Utils.getApp().getSystemService(Context.CLIPBOARD_SERVICE);
                                //noinspection ConstantConditions
                                cm.setPrimaryClip(ClipData.newPlainText("link", link));
                                BusUtils.post(BUS_TAG_MAIN_SHOW, getString(R.string.action_link)
                                        + " " + getString(R.string.successful));
                                break;
                            case 1:
                                Observable.create(new ObservableOnSubscribe<Integer>() {
                                    @Override
                                    public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                                        try {
                                            if (AppConfig.blockList == null)
                                                AppConfig.blockList = new ArrayList<>();
                                            boolean isAdd = true;
                                            if (AppConfig.blockList.size() > 0) {
                                                for (BlockItem blockItem : AppConfig.blockList) {
                                                    if (blockItem.getType() == TYPE_SOURCE && TextUtils.equals(blockItem.getText(), source))
                                                        isAdd = false;
                                                }
                                            }
                                            if (isAdd) {
                                                BlockItem newItem = new BlockItem(TYPE_SOURCE, source);
                                                AppConfig.blockList.add(newItem);
                                                e.onNext(1);
                                                newItem.save();
                                            } else {
                                                e.onNext(2);
                                            }
                                        } catch (Exception e1) {
                                            e1.printStackTrace();
                                            e.onNext(0);
                                        } finally {
                                            e.onComplete();
                                        }
                                    }
                                }).subscribeOn(Schedulers.computation())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Consumer<Integer>() {
                                            @Override
                                            public void accept(Integer i) throws Exception {
                                                switch (i) {
                                                    case 0:
                                                        BusUtils.post(BUS_TAG_MAIN_SHOW, getString(R.string.action_block_source) + " " + getString(R.string.fail));
                                                        break;
                                                    case 1:
                                                        BusUtils.post(BUS_TAG_MAIN_SHOW, getString(R.string.start_after_restart_list));
                                                        break;
                                                    case 2:
                                                        BusUtils.post(BUS_TAG_MAIN_SHOW, getString(R.string.repeated));
                                                        break;
                                                }
                                            }
                                        });
                                dialog.dismiss();
                                break;
                            case 2:
                                final EditText editText = new EditText(mActivity);
                                editText.setText(title);
                                editText.setHintTextColor(getResources().getColor(R.color.black));
                                editText.setTextColor(getResources().getColor(R.color.black));
                                new MaterialStyledDialog.Builder(mActivity)
                                        .setHeaderDrawable(R.drawable.ic_edit)
                                        .setHeaderScaleType(ImageView.ScaleType.CENTER)
                                        .setCustomView(editText)
                                        .setHeaderColor(R.color.colorAccent)
                                        .setPositiveText(R.string.save)
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                Observable.create(new ObservableOnSubscribe<Integer>() {
                                                    @Override
                                                    public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                                                        try {
                                                            if (AppConfig.blockList == null)
                                                                AppConfig.blockList = new ArrayList<>();
                                                            String keywords = editText.getText().toString();
                                                            boolean isAdd = true;
                                                            if (AppConfig.blockList.size() > 0) {
                                                                for (BlockItem blockItem : AppConfig.blockList) {
                                                                    if (blockItem.getType() == TYPE_KEYWORDS && TextUtils.equals(blockItem.getText(), keywords))
                                                                        isAdd = false;
                                                                }
                                                            }
                                                            if (isAdd) {
                                                                BlockItem newItem = new BlockItem(TYPE_KEYWORDS, keywords);
                                                                AppConfig.blockList.add(newItem);
                                                                e.onNext(1);
                                                                newItem.save();
                                                            } else {
                                                                e.onNext(2);
                                                            }
                                                        } catch (Exception e1) {
                                                            e1.printStackTrace();
                                                            e.onNext(0);
                                                        } finally {
                                                            e.onComplete();
                                                        }
                                                    }
                                                }).subscribeOn(Schedulers.computation())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(new Consumer<Integer>() {
                                                            @Override
                                                            public void accept(Integer i) throws Exception {
                                                                switch (i) {
                                                                    case 0:
                                                                        BusUtils.post(BUS_TAG_MAIN_SHOW_ERROR, getString(R.string.action_block_keywords)
                                                                                + " " + getString(R.string.fail));
                                                                        break;
                                                                    case 1:
                                                                        BusUtils.post(BUS_TAG_MAIN_SHOW, getString(R.string.start_after_restart_list));
                                                                        break;
                                                                    case 2:
                                                                        BusUtils.post(BUS_TAG_MAIN_SHOW, getString(R.string.repeated));
                                                                        break;
                                                                }
                                                            }
                                                        });
                                            }
                                        })
                                        .setNegativeText(getResources().getString(android.R.string.cancel))
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();
                                dialog.dismiss();
                                break;
                        }
                    }
                }).show();
    }
}
