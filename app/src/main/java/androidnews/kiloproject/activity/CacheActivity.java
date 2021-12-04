package androidnews.kiloproject.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.adapter.CacheNewsAdapter;
import androidnews.kiloproject.entity.data.CacheNews;
import androidnews.kiloproject.system.base.BaseActivity;


import androidnews.kiloproject.util.diff.CacheDiffCallBack;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static androidnews.kiloproject.entity.data.CacheNews.CACHE_COLLECTION;
import static androidnews.kiloproject.entity.data.CacheNews.CACHE_HISTORY;
import static androidnews.kiloproject.system.AppConfig.TYPE_CNBETA;
import static androidnews.kiloproject.system.AppConfig.TYPE_GUOKR;
import static androidnews.kiloproject.system.AppConfig.TYPE_ITHOME_START;
import static androidnews.kiloproject.system.AppConfig.TYPE_NETEASE_START;
import static androidnews.kiloproject.system.AppConfig.TYPE_SMARTISAN_START;
import static androidnews.kiloproject.system.AppConfig.TYPE_ZHIHU;
import static androidnews.kiloproject.system.AppConfig.isNightMode;

public class CacheActivity extends BaseActivity {

    Toolbar toolbar;
    RecyclerView rvContent;
    ProgressBar progress;
    ConstraintLayout rootView;
    ConstraintLayout emptyView;
    SearchView searchView;

    CacheNewsAdapter cacheNewsAdapter;
    int type;
    String typeStr = "";
    boolean isChange;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        rvContent = (RecyclerView) findViewById(R.id.rv_content);
        progress = (ProgressBar) findViewById(R.id.progress);
        rootView = (ConstraintLayout) findViewById(R.id.root_view);
        emptyView = (ConstraintLayout) findViewById(R.id.empty_view);


        initToolbar(toolbar, true);
        type = getIntent().getIntExtra("type", 0);
//        type = (type == 0) ? CACHE_COLLECTION : type;
        switch (type) {
            case CACHE_HISTORY:
                getSupportActionBar().setTitle(getString(R.string.history));
                break;
            case CACHE_COLLECTION:
                getSupportActionBar().setTitle(getString(R.string.action_star));
                break;
            default:
                break;
        }
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_list_number:
                        new MaterialStyledDialog.Builder(mActivity)
                                .setHeaderDrawable(R.drawable.ic_phone)
                                .setHeaderScaleType(ImageView.ScaleType.CENTER)
                                .setTitle(R.string.action_list_number)
                                .setDescription(getString(R.string.message_reading_record).replace("{type}", typeStr)
                                        .replace("{num}", String.valueOf(cacheNewsAdapter.getData().size())))
                                .setHeaderColor(R.color.colorAccent)
                                .setPositiveText(R.string.action_share)
                                .setNegativeText(android.R.string.ok)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        Intent intent = new Intent();
                                        intent.setAction(Intent.ACTION_SEND);//设置分享行为
                                        intent.setType("text/plain");//设置分享内容的类型
                                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.reading_record));//添加分享内容标题
                                        intent.putExtra(Intent.EXTRA_TEXT, "【" + getString(R.string.reading_record) + "】"
                                                + getString(R.string.shared_reading_record)
                                                .replace("{type}", typeStr)
                                                .replace("{num}", String.valueOf(cacheNewsAdapter.getData().size())));//添加分享内容
                                        //创建分享的Dialog
                                        intent = Intent.createChooser(intent, getString(R.string.action_share));
                                        startActivity(intent);
                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                        break;
                }
                return false;
            }
        });
        initBar(R.color.main_background, true);
    }

    @Override
    protected void initSlowly() {
        if (type > 0) {
            Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                    try {
                        List<CacheNews> currentData = LitePal.where("type = ?", String.valueOf(type))
                                .order("id desc")
                                .find(CacheNews.class);
                        if (currentData == null || currentData.size() < 1)
                            e.onNext(0);
                        else {
                            if (NetworkUtils.isConnected())
                                e.onNext(1);
                            else
                                e.onNext(2);
                        }
                        cacheNewsAdapter = new CacheNewsAdapter(mActivity, currentData);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        e.onNext(0);
                    } finally {
                        e.onComplete();
                    }

                    switch (type) {
                        case CACHE_HISTORY:
                            typeStr = getString(R.string.read);
                            break;
                        case CACHE_COLLECTION:
                            typeStr = getString(R.string.collected);
                            break;
                    }
                    e.onComplete();
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer i) throws Exception {
                            progress.setVisibility(View.GONE);
                            if (i == 0)
                                setEmptyView();
                            cacheNewsAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                                    CacheNews cacheNews = (CacheNews) adapter.getData().get(position);
                                    Intent intent = null;
                                    switch (cacheNews.getChannel()) {
                                        case TYPE_ZHIHU:
                                            if (i == 0)
                                                return;
                                            intent = new Intent(mActivity, ZhiHuDetailActivity.class);
                                            try {
                                                intent.putExtra("id", Integer.parseInt(cacheNews.getDocid()));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case TYPE_GUOKR:
                                            if (i == 0)
                                                return;
                                            intent = new Intent(mActivity, GuoKrDetailActivity.class);
                                            try {
                                                intent.putExtra("id", Integer.parseInt(cacheNews.getDocid()));
                                                intent.putExtra("title", cacheNews.getTitle());
                                                intent.putExtra("img", cacheNews.getImgUrl());
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        case TYPE_CNBETA:
                                            intent = new Intent(mActivity, CnBetaDetailActivity.class);
                                            switch (i) {
                                                case 1:
                                                    intent.putExtra("sid", cacheNews.getDocid());
                                                    break;
                                                case 2:
                                                    intent.putExtra("htmlText", cacheNews.getHtmlText());
                                                    break;
                                            }
                                            break;
                                        case TYPE_NETEASE_START://包含网易报刊
                                            intent = new Intent(mActivity, NewsDetailActivity.class);
                                            switch (i) {
                                                case 1:
                                                    intent.putExtra("docid", cacheNews.getDocid());
                                                    break;
                                                case 2:
                                                    intent.putExtra("htmlText", cacheNews.getHtmlText());
                                                    break;
                                            }
                                            break;
                                        case TYPE_ITHOME_START:
                                            intent = new Intent(mActivity, ITHomeDetailActivity.class);
                                            switch (i) {
                                                case 1:
                                                    intent.putExtra("title", cacheNews.getTitle());
                                                    intent.putExtra("url", cacheNews.getUrl());
                                                    intent.putExtra("id", cacheNews.getDocid());
                                                    intent.putExtra("time", cacheNews.getTimeStr());
                                                    intent.putExtra("img", cacheNews.getImgUrl());
                                                    break;
                                                case 2:
                                                    intent.putExtra("htmlText", cacheNews.getHtmlText());
                                                    break;
                                            }
                                            break;
                                        case TYPE_SMARTISAN_START:
                                            intent = new Intent(mActivity, SmartisanDetailActivity.class);
                                            intent.putExtra("title", cacheNews.getTitle());
                                            intent.putExtra("url", cacheNews.getUrl());
                                            intent.putExtra("id", cacheNews.getDocid());
                                            intent.putExtra("time", cacheNews.getTimeStr());
                                            intent.putExtra("img", cacheNews.getImgUrl());
                                            intent.putExtra("source", cacheNews.getSource());
                                            break;
                                    }
                                    if (intent != null)
                                        startActivityForResult(intent, CACHE_RESULT);
                                }
                            });
                            cacheNewsAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
                                @Override
                                public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                                    builder.setTitle(R.string.delete)
                                            .setMessage(R.string.delete_message)
                                            .setCancelable(true)
                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    isChange = true;
                                                    LitePal.delete(CacheNews.class, ((CacheNews) adapter.getData().get(position)).getId());
                                                    List<CacheNews> newList = new ArrayList<>();
                                                    newList.addAll(cacheNewsAdapter.getData());
                                                    newList.remove(position);
                                                    CacheDiffCallBack callback = new CacheDiffCallBack(newList);
                                                    cacheNewsAdapter.setNewDiffData(callback);
                                                }
                                            }).setNegativeButton(android.R.string.cancel,null).show()
                                            .getButton(Dialog.BUTTON_NEGATIVE)
                                            .setBackgroundColor(getResources()
                                                    .getColor(isNightMode ? R.color.awesome_background : android.R.color.darker_gray));
                                    return true;
                                }
                            });
                            rvContent.setLayoutManager(new LinearLayoutManager(mActivity));
                            rvContent.setAdapter(cacheNewsAdapter);
                        }
                    });
        } else {
            setEmptyView();
        }
    }

    private void setEmptyView() {
        progress.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            initSlowly();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cache_items, menu);//加载menu布局
        MenuItem myActionMenuItem = menu.findItem(R.id.action_search);

        searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Toast like print
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                List<CacheNews> totalList = new ArrayList<>();
                try {
                    totalList = LitePal.where("type = ?", String.valueOf(type))
                            .order("id desc")
                            .find(CacheNews.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (TextUtils.isEmpty(s)) {
                    CacheDiffCallBack callback = new CacheDiffCallBack(totalList);
                    cacheNewsAdapter.setNewDiffData(callback);
                } else {
                    List<CacheNews> newList = new ArrayList<>();
                    for (CacheNews datum : totalList) {
                        if (datum.getTitle().contains(s) ||
                                datum.getSource().contains(s))
                            newList.add(datum);
                    }
                    CacheDiffCallBack callback = new CacheDiffCallBack(newList);
                    cacheNewsAdapter.setNewDiffData(callback);
                }
                return false;
            }
        });
        return true;
    }
}
