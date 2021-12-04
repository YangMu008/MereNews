package androidnews.kiloproject.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.BusUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.gyf.immersionbar.ImmersionBar;
import com.youth.banner.Banner;
import com.youth.banner.listener.OnBannerListener;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.adapter.MainBgBannerAdapter;
import androidnews.kiloproject.entity.data.BlockItem;
import androidnews.kiloproject.entity.data.ListBannerData;
import androidnews.kiloproject.entity.data.MainBgBannerData;
import androidnews.kiloproject.entity.net.PhotoCenterData;
import androidnews.kiloproject.fragment.BaseRvFragment;
import androidnews.kiloproject.fragment.CnBetaRvFragment;
import androidnews.kiloproject.fragment.GuoKrRvFragment;
import androidnews.kiloproject.fragment.ITHomeRvFragment;
import androidnews.kiloproject.fragment.MainRvFragment;
import androidnews.kiloproject.fragment.PearVideoRvFragment;
import androidnews.kiloproject.fragment.PressRvFragment;
import androidnews.kiloproject.fragment.SmartisanRvFragment;
import androidnews.kiloproject.fragment.VideoRvFragment;
import androidnews.kiloproject.fragment.ZhihuRvFragment;
import androidnews.kiloproject.system.AppConfig;
import androidnews.kiloproject.system.base.BaseActivity;
import cn.jzvd.Jzvd;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static androidnews.kiloproject.entity.data.CacheNews.CACHE_COLLECTION;
import static androidnews.kiloproject.entity.data.CacheNews.CACHE_HISTORY;
import static androidnews.kiloproject.fragment.BaseRvFragment.BUS_TAG_MAIN_SHOW;
import static androidnews.kiloproject.fragment.BaseRvFragment.BUS_TAG_MAIN_SHOW_ERROR;
import static androidnews.kiloproject.fragment.BaseRvFragment.TYPE_REFRESH;
import static androidnews.kiloproject.system.AppConfig.CONFIG_NIGHT_MODE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_RANDOM_HEADER;
import static androidnews.kiloproject.system.AppConfig.CONFIG_SHOW_EXPLORER;
import static androidnews.kiloproject.system.AppConfig.CONFIG_TYPE_ARRAY;
import static androidnews.kiloproject.system.AppConfig.DOWNLOAD_EXPLORER_ADDRESS;
import static androidnews.kiloproject.system.AppConfig.NEWS_PHOTO_URL;
import static androidnews.kiloproject.system.AppConfig.TYPE_CNBETA;
import static androidnews.kiloproject.system.AppConfig.TYPE_GUOKR;
import static androidnews.kiloproject.system.AppConfig.TYPE_ITHOME_END;
import static androidnews.kiloproject.system.AppConfig.TYPE_ITHOME_START;
import static androidnews.kiloproject.system.AppConfig.TYPE_PEAR_VIDEO;
import static androidnews.kiloproject.system.AppConfig.TYPE_PRESS_END;
import static androidnews.kiloproject.system.AppConfig.TYPE_PRESS_START;
import static androidnews.kiloproject.system.AppConfig.TYPE_SMARTISAN_END;
import static androidnews.kiloproject.system.AppConfig.TYPE_SMARTISAN_START;
import static androidnews.kiloproject.system.AppConfig.TYPE_VIDEO_END;
import static androidnews.kiloproject.system.AppConfig.TYPE_VIDEO_START;
import static androidnews.kiloproject.system.AppConfig.TYPE_ZHIHU;
import static androidnews.kiloproject.system.AppConfig.isAutoNight;
import static androidnews.kiloproject.system.AppConfig.isNightMode;

public class NewsMainActivity extends BaseActivity implements View.OnClickListener {

    FragmentPagerAdapter mPagerAdapter;
    public static final int DEFAULT_PAGE = 4;
    PhotoCenterData photoData;
    int[] channelArray = new int[DEFAULT_PAGE];
    List<BaseRvFragment> fragmentList = new ArrayList<>();
    String[] tagNames;
    private Banner mBannerTop;
    private Toolbar toolbar;
    private TabLayout mCollectTab;
    private ViewPager mContentVp;
    private BottomAppBar mAppBarBottom;
    private FloatingActionButton mFab;
    private CoordinatorLayout mLayoutCoordinator;
    private AppBarLayout mLayoutAppbar;
    private CollapsingToolbarLayout mToolbarLayoutCollapsing;
    private View mMaskTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_news);
        initView();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ImmersionBar.with(mActivity)
                .titleBar(toolbar)
                .navigationBarColor(ImmersionBar.isSupportNavigationIconDark() ? R.color.main_background : R.color.divider)
                .navigationBarDarkIcon(!isNightMode && ImmersionBar.isSupportNavigationIconDark())
                .init();
    }

    private void initView() {
        mAppBarBottom = (BottomAppBar) findViewById(R.id.bottom_app_bar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mCollectTab = (TabLayout) findViewById(R.id.tab_collect);
        mContentVp = (ViewPager) findViewById(R.id.vp_content);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(this);
        mLayoutCoordinator = (CoordinatorLayout) findViewById(R.id.root_view);
        mBannerTop = (Banner) findViewById(R.id.banner_top);
        mLayoutAppbar = (AppBarLayout) findViewById(R.id.appbar_layout);
        mToolbarLayoutCollapsing = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);
        mMaskTop = (View) findViewById(R.id.top_mask);

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mMaskTop.getLayoutParams();
        layoutParams.height = BarUtils.getStatusBarHeight();
        mMaskTop.setLayoutParams(layoutParams);

        if (!isNightMode)
            mToolbarLayoutCollapsing.post(() -> {
                int offHeight = mToolbarLayoutCollapsing.getHeight()
                        - ImmersionBar.getStatusBarHeight(this) * 2
                        - mCollectTab.getHeight();
                //考虑到有白色遮罩,把变色的时间提前一点
//            int offHeight = mToolbarLayoutCollapsing.getHeight()
//            - ImmersionBar.getStatusBarHeight(this)
//            - mCollectTab.getHeight();
                mLayoutAppbar.addOnOffsetChangedListener((appBarLayout1, i) -> {
                    mBannerTop.setVisibility(Math.abs(i) >= mToolbarLayoutCollapsing.getHeight()
                            - ImmersionBar.getStatusBarHeight(this)
                            - mCollectTab.getHeight() ? View.INVISIBLE : View.VISIBLE);
                    ImmersionBar.with(this).statusBarDarkFont(Math.abs(i) >= offHeight, 0.2f).init();
                });
            });

        mCollectTab.setupWithViewPager(mContentVp);

        mFab.setShowMotionSpecResource(R.animator.fab_show);
        mFab.setHideMotionSpecResource(R.animator.fab_hide);

        //设置底部栏Menu的点击事件
        mAppBarBottom.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(mActivity, ChannelActivity.class), SELECT_RESULT);
            }
        });
        mAppBarBottom.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                Intent intent;
                switch (id) {
                    case R.id.nav_his:
                        intent = new Intent(mActivity, CacheActivity.class);
                        intent.putExtra("type", CACHE_HISTORY);
                        startActivity(intent);
                        break;
                    case R.id.nav_coll:
                        intent = new Intent(mActivity, CacheActivity.class);
                        intent.putExtra("type", CACHE_COLLECTION);
                        startActivity(intent);
                        break;
                    case R.id.nav_block:
                        intent = new Intent(mActivity, BlockActivity.class);
                        startActivityForResult(intent, BLOCK_RESULT);
                        break;
                    case R.id.nav_setting:
                        intent = new Intent(mActivity, SettingActivity.class);
                        startActivityForResult(intent, SETTING_RESULT);
                        break;
                    case R.id.nav_about:
                        intent = new Intent(mActivity, AboutActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_theme:
                        if (isAutoNight) {
                            SnackbarUtils.with(mContentVp).setMessage(getString(R.string.tip_to_close_auto_night)).show();
                        } else {
                            isNightMode = !isNightMode;
                            SPUtils.getInstance().put(CONFIG_NIGHT_MODE, isNightMode);
                            restartWithAnime(R.id.root_view, R.id.vp_content);
                        }
                }
                return false;
            }
        });
    }

    @Override
    protected void initSlowly() {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                tagNames = getResources().getStringArray(R.array.address_tag);

                String arrayStr = SPUtils.getInstance().getString(CONFIG_TYPE_ARRAY);

                AppConfig.blockList = LitePal.findAll(BlockItem.class);

                if (TextUtils.isEmpty(arrayStr)) {
                    channelArray = new int[DEFAULT_PAGE];
                    for (int i = 0; i < DEFAULT_PAGE; i++) {
                        channelArray[i] = i;
                    }
                    e.onNext(true);
                } else {
                    String[] channelStrArray = arrayStr.split("#");
                    List<Integer> channelList = new ArrayList<>();
                    for (int i = 0; i < channelStrArray.length; i++) {
                        int index = Integer.parseInt(channelStrArray[i]);
                        if (index > tagNames.length - 1)
                            continue;
                        if (!TextUtils.equals(tagNames[index], "fake")) {
                            channelList.add(index);
                        }
                    }
                    channelArray = new int[channelList.size()];
                    for (int i = 0; i < channelList.size(); i++) {
                        channelArray[i] = channelList.get(i);
                    }
                    e.onNext(true);
                }
                saveChannel(channelArray);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            for (int i : channelArray) {
                                TabLayout.Tab tab = mCollectTab.newTab();
                                tab.setText(tagNames[i]);
                                mCollectTab.addTab(tab);
                            }
                            if (mPagerAdapter == null) {
                                mPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
                                    @NonNull
                                    @Override
                                    public Fragment getItem(int position) {
                                        BaseRvFragment fragment = createFragment(position);
                                        fragmentList.add(fragment);
                                        return fragment;
                                    }

                                    @Override
                                    public int getCount() {
                                        return channelArray.length;
                                    }

                                    @Nullable
                                    @Override
                                    public CharSequence getPageTitle(int position) {
                                        return tagNames[channelArray[position]];
                                    }
                                };
                                mContentVp.setAdapter(mPagerAdapter);
                                startBgAnimate();
                            } else {
                                mPagerAdapter.notifyDataSetChanged();
                            }
                        }
//                        if (AppConfig.isEasterEggs && !SPUtils.getInstance().getBoolean(CONFIG_SHOW_EXPLORER)) {
//                            new MaterialStyledDialog.Builder(mActivity)
//                                    .setHeaderDrawable(R.drawable.ic_smile)
//                                    .setHeaderScaleType(ImageView.ScaleType.CENTER)
//                                    .setTitle(getResources().getString(R.string.explorer_title))
//                                    .setDescription(getResources().getString(R.string.explorer_message))
//                                    .setHeaderColor(R.color.colorPrimary)
//                                    .setPositiveText(android.R.string.ok)
//                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
//                                        @Override
//                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                                            Uri uri = Uri.parse(DOWNLOAD_EXPLORER_ADDRESS);
//                                            startActivity(new Intent(Intent.ACTION_VIEW, uri));
//                                        }
//                                    })
//                                    .setNegativeText(getResources().getString(android.R.string.cancel))
//                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
//                                        @Override
//                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                                            dialog.dismiss();
//                                        }
//                                    })
//                                    .show();
//                            SPUtils.getInstance().put(CONFIG_SHOW_EXPLORER, true);
//                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Glide.with(mActivity).resumeRequests();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                BaseRvFragment fragment = null;
                try {
                    fragment = fragmentList.get(mContentVp.getCurrentItem());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (fragment != null) {
                    fragment.showRefresh();
                    fragment.requestData(TYPE_REFRESH);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_RESULT:
                if (resultCode == RESULT_OK) {
                    data = getIntent();
                    finish();
                    startActivity(data);
                }
                break;
            case SETTING_RESULT:
                if (resultCode == RESULT_OK)
                    initSlowly();
                break;
            case BLOCK_RESULT:
                if (resultCode == RESULT_OK)
                    SnackbarUtils.with(mContentVp)
                            .setMessage(getResources()
                                    .getString(R.string.start_after_restart_list))
                            .show();
                break;
        }
    }

    private BaseRvFragment createFragment(int position) {
        int type = channelArray[position];
        if (type >= TYPE_ZHIHU) {
            switch (type) {
                case TYPE_ZHIHU:
                    return new ZhihuRvFragment();
                case TYPE_GUOKR:
                    return new GuoKrRvFragment();
                case TYPE_CNBETA:
                    return new CnBetaRvFragment();
                case TYPE_PEAR_VIDEO:
                    return new PearVideoRvFragment();
            }
            if (type >= TYPE_ITHOME_START && type <= TYPE_ITHOME_END)
                return ITHomeRvFragment.newInstance(channelArray[position]);
            else if (type >= TYPE_PRESS_START && type <= TYPE_PRESS_END)
                return PressRvFragment.newInstance(channelArray[position]);
            else if (type >= TYPE_SMARTISAN_START && type <= TYPE_SMARTISAN_END)
                return SmartisanRvFragment.newInstance(channelArray[position]);
//            else if (type >= TYPE_VIDEO_START && type <= TYPE_VIDEO_END)
//                return VideoRvFragment.newInstance(channelArray[position]);
        }
        return MainRvFragment.newInstance(channelArray[position]);
    }

    private void startBgAnimate() {
        int headerType = SPUtils.getInstance().getInt(CONFIG_RANDOM_HEADER, 0);
        if (AppConfig.isNoImage) headerType = 1; //强制渐变色
        switch (headerType) {
            case 0:
                requestBgData();
                break;
            default:
                List<MainBgBannerData> imgs = new ArrayList<>();
                imgs.add(new MainBgBannerData(R.drawable.drawer_header_bg, ""));

                mBannerTop.addBannerLifecycleObserver(mActivity)//添加生命周期观察者
                        .setAdapter(new MainBgBannerAdapter(imgs))
                        .setDelayTime(8 * 1000)
                        .start();
                break;
        }
    }

    private void requestBgData() {
        EasyHttp.get(NEWS_PHOTO_URL)
                .readTimeOut(30 * 1000)//局部定义读超时
                .writeTimeOut(30 * 1000)
                .connectTimeout(30 * 1000)
                .timeStamp(true)
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onSuccess(final String result) {
                        Observable.create(new ObservableOnSubscribe<Boolean>() {
                            @Override
                            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                                String temp = result.replace(")", "}");
                                String response = temp.replace("cacheMoreData(", "{\"cacheMoreData\":");
                                if (!TextUtils.isEmpty(response) || TextUtils.equals(response, "{}")) {
                                    try {
                                        photoData = gson.fromJson(response, PhotoCenterData.class);
                                        e.onNext(true);
                                    } catch (Exception e1) {
                                        e1.printStackTrace();
                                        e.onNext(false);
                                    }
                                } else e.onNext(false);
                                e.onComplete();
                            }
                        }).subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean aBoolean) throws Exception {
                                        if (aBoolean) {
                                            List<MainBgBannerData> imgs = new ArrayList<>();
                                            for (PhotoCenterData.CacheMoreDataBean datum : photoData.getCacheMoreData()) {
                                                imgs.add(new MainBgBannerData(datum.getCover(), datum.getSetname()));
                                            }
                                            mBannerTop.addBannerLifecycleObserver(mActivity)//添加生命周期观察者
                                                    .setAdapter(new MainBgBannerAdapter(imgs))
                                                    .setDelayTime(8 * 1000)
                                                    .setOnBannerListener(new OnBannerListener() {
                                                        @Override
                                                        public void OnBannerClick(Object data, int position) {
                                                            Intent intent = new Intent(mActivity, GalleySimpleActivity.class);
                                                            intent.putExtra("img", (String) imgs.get(position).getPath());
                                                            intent.putExtra("title", imgs.get(position).getTitle());
                                                            intent.putExtra("desc", photoData.getCacheMoreData().get(position).getDesc());
                                                            startTransition(intent, mBannerTop);
                                                        }
                                                    })
                                                    .start();
                                        } else
                                            SnackbarUtils.with(mContentVp).setMessage(getString(R.string.server_fail)).showError();
                                    }
                                });
                    }
                });
    }

    public static boolean saveChannel(int[] channelArray) {
        StringBuilder sb = new StringBuilder();

        try {
            for (Integer integer : channelArray) {
                sb.append(integer + "#");
            }
            SPUtils.getInstance().put(CONFIG_TYPE_ARRAY, sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @BusUtils.Bus(tag = BUS_TAG_MAIN_SHOW,
            threadMode = BusUtils.ThreadMode.MAIN)
    public void showSnack(String text) {
        Snackbar.make(mLayoutCoordinator, text, Snackbar.LENGTH_SHORT)
                .setAnchorView(mAppBarBottom)
                .show();
    }

    @BusUtils.Bus(tag = BUS_TAG_MAIN_SHOW_ERROR,
            threadMode = BusUtils.ThreadMode.MAIN)
    public void showErrorSnack(String text) {
        Snackbar.make(mLayoutCoordinator, text, Snackbar.LENGTH_SHORT)
                .setAnchorView(mAppBarBottom)
                .setBackgroundTint(getResources().getColor(R.color.orangered))
                .show();
    }

    @Override
    public void onStart() {
        super.onStart();
        BusUtils.register(mActivity);
    }

    @Override
    public void onStop() {
        super.onStop();
        BusUtils.unregister(mActivity);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Jzvd.releaseAllVideos();
        Glide.with(mActivity).pauseRequests();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Glide.get(mActivity).clearMemory();
            for (BaseRvFragment fragment : fragmentList) {
                fragment.startLowMemory();
            }
        }
        Glide.get(mActivity).trimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        //内存低时,清理缓存
        Glide.get(mActivity).clearMemory();
    }
}
