package androidnews.kiloproject.system;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.gyf.cactus.Cactus;
import com.gyf.cactus.callback.CactusCallback;
import com.lzf.easyfloat.EasyFloat;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshFooter;
import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshFooterCreator;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshHeaderCreator;
import com.tencent.bugly.crashreport.CrashReport;
import com.zhouyou.http.EasyHttp;

import org.litepal.LitePal;

import java.io.File;

import androidnews.kiloproject.R;
import androidnews.kiloproject.util.PushUtils;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

import static androidnews.kiloproject.system.AppConfig.CONFIG_AUTO_LOADMORE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_AUTO_NIGHT;
import static androidnews.kiloproject.system.AppConfig.CONFIG_AUTO_REFRESH;
import static androidnews.kiloproject.system.AppConfig.CONFIG_BACK_EXIT;
import static androidnews.kiloproject.system.AppConfig.CONFIG_BLOCK_WE_MEDIA;
import static androidnews.kiloproject.system.AppConfig.CONFIG_DISABLE_NOTICE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_EASTER_EGGS;
import static androidnews.kiloproject.system.AppConfig.CONFIG_HAPTIC;
import static androidnews.kiloproject.system.AppConfig.CONFIG_HIGH_RAM;
import static androidnews.kiloproject.system.AppConfig.CONFIG_LIST_TYPE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_NIGHT_MODE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_NO_IMAGE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_PUSH;
import static androidnews.kiloproject.system.AppConfig.CONFIG_PUSH_MODE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_PUSH_SOUND;
import static androidnews.kiloproject.system.AppConfig.CONFIG_PUSH_TIME;
import static androidnews.kiloproject.system.AppConfig.CONFIG_SHOW_DETAIL_TIME;
import static androidnews.kiloproject.system.AppConfig.CONFIG_SHOW_SKELETON;
import static androidnews.kiloproject.system.AppConfig.CONFIG_STATUS_BAR;
import static androidnews.kiloproject.system.AppConfig.CONFIG_SWIPE_BACK;
import static androidnews.kiloproject.system.AppConfig.CONFIG_TEXT_SIZE;
import static androidnews.kiloproject.system.AppConfig.HOST_163;
import static androidnews.kiloproject.system.AppConfig.isAutoNight;
import static androidnews.kiloproject.system.AppConfig.isNightMode;
import static androidnews.kiloproject.system.AppConfig.isPush;
import static androidnews.kiloproject.system.AppConfig.isPushMode;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

/**
 * Created by Administrator on 2017/12/9.
 */

public class MyApplication extends Application {
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        CrashReport.initCrashReport(getApplicationContext(), "e86bab41f6", false);

        //网络框架
        EasyHttp.init(this);//默认初始化
        EasyHttp.getInstance()
                .setBaseUrl(HOST_163)
//                .debug("网络DEBUG", true);
        ;

        //数据库
        LitePal.initialize(this);

        //Util工具包
        Utils.init(this);

        SPUtils spUtils = SPUtils.getInstance();
        AppConfig.isShowSkeleton = spUtils.getBoolean(CONFIG_SHOW_SKELETON, true);
        AppConfig.isAutoNight = spUtils.getBoolean(CONFIG_AUTO_NIGHT);
        AppConfig.listType = spUtils.getInt(CONFIG_LIST_TYPE, -1);
        AppConfig.mTextSize = spUtils.getInt(CONFIG_TEXT_SIZE, 1);
        AppConfig.isNightMode = spUtils.getBoolean(CONFIG_NIGHT_MODE);
        AppConfig.isSwipeBack = spUtils.getBoolean(CONFIG_SWIPE_BACK);
        AppConfig.isAutoRefresh = spUtils.getBoolean(CONFIG_AUTO_REFRESH);
        AppConfig.isAutoLoadMore = spUtils.getBoolean(CONFIG_AUTO_LOADMORE);
        AppConfig.isBackExit = spUtils.getBoolean(CONFIG_BACK_EXIT);
        AppConfig.isStatusBar = spUtils.getBoolean(CONFIG_STATUS_BAR);
        AppConfig.isDisNotice = spUtils.getBoolean(CONFIG_DISABLE_NOTICE);
        AppConfig.isPush = spUtils.getBoolean(CONFIG_PUSH, true);
        AppConfig.isPushSound = spUtils.getBoolean(CONFIG_PUSH_SOUND);
        AppConfig.isPushMode = spUtils.getBoolean(CONFIG_PUSH_MODE);
        AppConfig.pushTime = spUtils.getInt(CONFIG_PUSH_TIME, 1);
        AppConfig.isEasterEggs = spUtils.getBoolean(CONFIG_EASTER_EGGS);
        AppConfig.isHaptic = spUtils.getBoolean(CONFIG_HAPTIC);
        AppConfig.isNoImage = spUtils.getBoolean(CONFIG_NO_IMAGE);
        AppConfig.isHighRam = spUtils.getBoolean(CONFIG_HIGH_RAM);
        AppConfig.isBlockWeMedia = spUtils.getBoolean(CONFIG_BLOCK_WE_MEDIA);
        AppConfig.isShowDetailTime = spUtils.getBoolean(CONFIG_SHOW_DETAIL_TIME);

        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (AppConfig.isAutoNight && mode == Configuration.UI_MODE_NIGHT_YES)
            AppConfig.isNightMode = true;

        if (isAutoNight) {
            int currentNightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    // Night mode is not active, we're in day time
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    // We don't know what mode we're in, assume notnight
                    isNightMode = false;
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    // Night mode is active, we're at night!
                    isNightMode = true;
                    break;
            }
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(isNightMode ? AppCompatDelegate.MODE_NIGHT_YES :
                    AppCompatDelegate.MODE_NIGHT_NO);
        }

        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                LogUtils.d("onRxJavaErrorHandler ---->: $it");
            }
        });

        if (isPush && isPushMode) {
            Cactus.getInstance()
                    .isDebug(AppUtils.isAppDebug())
                    .setChannelId("Mere Push Cactus")
                    .setChannelName("Mere Push Channel Compat")
                    .setTitle("MereNews")
                    .setContent("MereNews推送服务运行中...")
                    .setSmallIcon(R.drawable.ic_launcher_logo)
                    .hideNotificationAfterO(true)
                    .setMusicInterval(25)
                    .addCallback(new CactusCallback() {
                        @Override
                        public void doWork(int i) {
                            LogUtils.d("Mere新闻兼容推送模式开始工作!");
                            PushUtils.getPushList(getInstance());
                        }

                        @Override
                        public void onStop() {
                            LogUtils.d("Mere新闻兼容推送模式停止工作!");
                        }
                    })
                    .register(this);
        }
        EasyFloat.init(this, AppUtils.isAppDebug());
        Glide.init(getInstance(),new GlideBuilder().setDiskCache(new DiskCache.Factory() {
            @Nullable
            @Override
            public DiskCache build() {
                File cacheLocation = new File(PathUtils.getInternalAppCachePath());
                cacheLocation.mkdirs();
                return DiskLruCacheWrapper.create(cacheLocation, 1024 * 1024 * 196);
            }
        }));
    }

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    static {
        //设置全局的Header构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreator(new DefaultRefreshHeaderCreator() {
            @Override
            public RefreshHeader createRefreshHeader(Context context, RefreshLayout layout) {
//                layout.setPrimaryColorsId(R.color.colorPrimary, android.R.color.white);//全局设置主题颜色
                return new MaterialHeader(context);//.setTimeFormat(new DynamicTimeFormat("更新于 %s"));//指定为经典Header，默认是 贝塞尔雷达Header
            }
        });
        //设置全局的Footer构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator(new DefaultRefreshFooterCreator() {
            @Override
            public RefreshFooter createRefreshFooter(Context context, RefreshLayout layout) {
//                指定为经典Footer，默认是 BallPulseFooter
                return new ClassicsFooter(context).setDrawableSize(24);
            }
        });
    }
}
