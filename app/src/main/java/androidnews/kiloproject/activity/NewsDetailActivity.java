package androidnews.kiloproject.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.bumptech.glide.Glide;
import com.lzf.easyfloat.EasyFloat;
import com.lzf.easyfloat.enums.ShowPattern;
import com.lzf.easyfloat.enums.SidePattern;
import com.lzf.easyfloat.interfaces.OnFloatCallbacks;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.entity.data.CacheNews;
import androidnews.kiloproject.entity.net.NewsDetailData;
import androidnews.kiloproject.system.AppConfig;
import androidnews.kiloproject.util.FileCompatUtils;
import androidnews.kiloproject.web.DetailWebViewClient;
import androidnews.kiloproject.util.GlideUtils;
import androidnews.kiloproject.widget.MyJzvdStd;
import cn.jzvd.Jzvd;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static androidnews.kiloproject.entity.data.CacheNews.CACHE_COLLECTION;
import static androidnews.kiloproject.entity.data.CacheNews.CACHE_HISTORY;
import static androidnews.kiloproject.system.AppConfig.GET_NEWS_DETAIL;
import static androidnews.kiloproject.system.AppConfig.TYPE_NETEASE_START;
import static androidnews.kiloproject.system.AppConfig.isNightMode;
import static com.blankj.utilcode.util.CollectionUtils.isEmpty;

public class NewsDetailActivity extends BaseDetailActivity {
    private String html;
    private NewsDetailData currentData;
    private boolean isStar = false;

    private boolean isLand = false;
    private ViewStub videoStub;
    private FrameLayout videoLayout;

    private int type = 0;
    public static final int TYPE_AUDIO = 1024;

    private List<NewsDetailData.VideoBean> videoList = new ArrayList();
    private int videoIndex = 0;

    @Override
    protected void initView() {
        initToolbar(toolbar, true);
        getSupportActionBar().setTitle(getString(R.string.loading));
        //menu item点击事件监听
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {
                    case R.id.action_share:
                        if (currentData == null || currentData.getShareLink() == null)
                            break;
                        String title = "";
                        try {
                            title = currentData.getTitle();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        intent = new Intent();
                        intent.setAction(Intent.ACTION_SEND);//设置分享行为
                        intent.setType("text/plain");//设置分享内容的类型
                        if (!TextUtils.isEmpty(title))
                            intent.putExtra(Intent.EXTRA_SUBJECT, title);//添加分享内容标题
                        intent.putExtra(Intent.EXTRA_TEXT, "【" + title + "】"
                                + currentData.getShareLink());//添加分享内容
                        //创建分享的Dialog
                        intent = Intent.createChooser(intent, getString(R.string.action_share));
                        startActivity(intent);
                        break;
                    case R.id.action_star:
                        if (isStar) {
                            Observable.create(new ObservableOnSubscribe<Boolean>() {
                                @Override
                                public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                                    e.onNext(checkStar(true));
                                    e.onComplete();
                                }
                            }).subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Consumer<Boolean>() {
                                        @Override
                                        public void accept(Boolean aBoolean) throws Exception {
                                            if (aBoolean) {
                                                item.setIcon(R.drawable.ic_star_no);
                                                SnackbarUtils.with(toolbar).setMessage(getString(R.string.star_no)).show();
                                            } else
                                                SnackbarUtils.with(toolbar).setMessage(getString(R.string.fail)).showError();
                                        }
                                    });
                            isStar = false;
                        } else {
                            item.setIcon(R.drawable.ic_star_ok);
                            saveCacheAsyn(CACHE_COLLECTION);
                            SnackbarUtils.with(toolbar).setMessage(getString(R.string.star_yes)).show();
                            isStar = true;
                        }
                        break;
                    case R.id.action_comment:
//                        ObjectAnimator animator = ObjectAnimator.ofInt(webView,"scrollY",webView.getScrollY(),0);
//                        animator.setDuration(300).start();
                        if (currentData == null || TextUtils.isEmpty(currentData.getReplyBoard()) || TextUtils.isEmpty(currentData.getDocid())) {
                            SnackbarUtils.with(toolbar).setMessage(getString(R.string.no_comment)).showError();
                            break;
                        }
                        intent = new Intent(mActivity, CommentActivity.class);
                        intent.putExtra("board", currentData.getReplyBoard());
                        intent.putExtra("docid", currentData.getDocid());
                        startActivity(intent);
                        break;
                    case R.id.action_link:
                        if (currentData != null) {
                            ClipboardManager cm = (ClipboardManager) Utils.getApp().getSystemService(Context.CLIPBOARD_SERVICE);
                            //noinspection ConstantConditions
                            cm.setPrimaryClip(ClipData.newPlainText("link", currentData.getShareLink()));
                            SnackbarUtils.with(toolbar).setMessage(getString(R.string.action_link)
                                    + " " + getString(R.string.successful)).show();
                        }
                        break;
                    case R.id.action_browser:
                        try {
                            Uri uri = Uri.parse(currentData.getShareLink());
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case R.id.action_video:
                        if (videoList.size() > 0) {
                            if (EasyFloat.getFloatView(mActivity, "Video") == null) {
                                EasyFloat.with(mActivity)
                                        .setLayout(R.layout.layout_pip_video)
                                        .setShowPattern(ShowPattern.CURRENT_ACTIVITY)
                                        .setSidePattern(SidePattern.RESULT_HORIZONTAL)
                                        .setTag("Video")
                                        .setGravity(Gravity.TOP, 0, 200)
                                        .registerCallbacks(new OnFloatCallbacks() {
                                            @Override
                                            public void createdResult(boolean b, String s, View view) {
                                                if (view != null) {
                                                    MyJzvdStd videoView = view.findViewById(R.id.item_card_vid);
                                                    loadVideo(videoView);

                                                    view.findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            Jzvd.releaseAllVideos();
                                                            EasyFloat.hide(mActivity, "Video");
                                                        }
                                                    });
                                                    view.findViewById(R.id.btn_previous).setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            if (videoIndex == 0) {
                                                                SnackbarUtils.with(toolbar).setMessage(getString(R.string.no_video)).show();
                                                            } else {
                                                                videoIndex--;
                                                                loadVideo(videoView);
                                                            }
                                                        }
                                                    });
                                                    view.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            if (videoIndex == (videoList.size() - 1)) {
                                                                SnackbarUtils.with(toolbar).setMessage(getString(R.string.no_video)).show();
                                                            } else {
                                                                videoIndex++;
                                                                loadVideo(videoView);
                                                            }
                                                        }
                                                    });

                                                    view.findViewById(R.id.btn_download).setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            FileCompatUtils.downloadFile(mActivity, videoList.get(videoIndex).getUrl_mp4(), webView, true);
                                                        }
                                                    });
                                                }
                                            }

                                            @Override
                                            public void show(View view) { }

                                            @Override
                                            public void hide(View view) { }

                                            @Override
                                            public void dismiss() { }

                                            @Override
                                            public void touchEvent(View view, MotionEvent motionEvent) { }

                                            @Override
                                            public void drag(View view, MotionEvent motionEvent) { }

                                            @Override
                                            public void dragEnd(View view) { }
                                        })
                                        .show();
                            } else EasyFloat.show(mActivity, "Video");
                        } else
                            SnackbarUtils.with(toolbar).setMessage(getString(R.string.no_video)).show();
                        break;
                }
                return false;
            }
        });
//        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
//            @Override
//            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
//                initSlowly();
//            }
//        });
    }

    @Override
    protected void initSlowly() {
        String docid = getIntent().getStringExtra("docid");
        if (!TextUtils.isEmpty(docid)) {
            EasyHttp.get(GET_NEWS_DETAIL.replace("{docid}", docid))
                    .readTimeOut(30 * 1000)//局部定义读超时
                    .writeTimeOut(30 * 1000)
                    .connectTimeout(30 * 1000)
                    .timeStamp(true)
                    .execute(new SimpleCallBack<String>() {
                        @Override
                        public void onError(ApiException e) {
                            SnackbarUtils.with(toolbar).setMessage(getString(R.string.load_fail) + e.getMessage()).showError();
                            hideSkeleton();
//                            refreshLayout.finishRefresh();
                        }

                        @Override
                        public void onSuccess(String response) {
                            hideSkeleton();
                            if (!TextUtils.isEmpty(response) || TextUtils.equals(response, "{}")) {
                                String jsonNoHeader = response.substring(20, response.length());
                                String jsonFine = jsonNoHeader.substring(0, jsonNoHeader.length() - 1);
                                if (response.contains("点这里升级")) {
                                    loadError();
                                    return;
                                }
                                try {
                                    currentData = gson.fromJson(jsonFine, NewsDetailData.class);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    ToastUtils.showShort(getString(R.string.server_fail) + e.getMessage());
                                    finish();
                                }
                                if (currentData == null || TextUtils.isEmpty(currentData.getBody())) {
                                    loadError();
                                    return;
                                }
                                Observable.create(new ObservableOnSubscribe<Boolean>() {
                                    @Override
                                    public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                                        currentData.setBody(deleteAd(currentData.getBody()));
                                        e.onNext(checkStar(false));
                                        e.onComplete();
                                    }
                                }).subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Consumer<Boolean>() {
                                            @Override
                                            public void accept(Boolean aBoolean) throws Exception {
                                                if (aBoolean) {
                                                    isStar = true;
                                                    try {
                                                        toolbar.getMenu().findItem(R.id.action_star).setIcon(R.drawable.ic_star_ok);
                                                    } catch (Exception e) {
                                                    }
                                                }
//                                                refreshLayout.finishRefresh();
                                            }
                                        });
                                if (webView != null) {
                                    initWeb();
                                    loadUrl();
                                }
                            } else {
//                                refreshLayout.finishRefresh();
                                SnackbarUtils.with(toolbar).setMessage(getString(R.string.load_fail)).showError();
                            }
                        }
                    });
        } else {
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                    html = getIntent().getStringExtra("htmlText");
                    if (isNightMode)
                        html.replace("<body text=\"#333\">", "<body bgcolor=\"#212121\" body text=\"#ccc\">");
                    if (!StringUtils.isEmpty(html)) {
                        e.onNext(true);
                    } else {
                        e.onNext(false);
                    }
                    e.onComplete();
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean aBoolean) throws Exception {
                            hideSkeleton();
                            if (aBoolean) {
                                initWeb();
                                getSupportActionBar().setTitle(R.string.news);
                                webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", "about:blank");
//                                webView.loadData(html, "text/html; charset=UTF-8", null);
                            } else
                                SnackbarUtils.with(toolbar).setMessage(getString(R.string.load_fail)).showError();
                        }
                    });
        }
    }

    private void loadUrl() {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                String title = "";
                String source = "";
                String pTime = "";
                String body = "";
                if (currentData == null)
                    return;
                try {
                    title = currentData.getTitle();
                    pTime = currentData.getPtime();
                    body = currentData.getBody();
                    if (!TextUtils.isEmpty(currentData.getSource()) && !currentData.getSource().equals("null"))
                        source = currentData.getSource();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

                html = createHtmlText(title, source, pTime, body);
                if (currentData.getVideo() != null) {
                    for (NewsDetailData.VideoBean videoBean : currentData.getVideo()) {
                        String mediaUrl = videoBean.getM3u8Hd_url();
                        if (TextUtils.isEmpty(mediaUrl)) {
                            mediaUrl = videoBean.getM3u8_url();
                        }
                        if (TextUtils.isEmpty(mediaUrl)) {
                            mediaUrl = videoBean.getMp4Hd_url();
                        }
                        if (TextUtils.isEmpty(mediaUrl)) {
                            mediaUrl = videoBean.getMp4_url();
                        }
                        if (TextUtils.isEmpty(mediaUrl)) {
                            mediaUrl = videoBean.getUrl_mp4();
                        }

                        if (mediaUrl.endsWith(".mp3")) {       //音频
                            html = html.replace(videoBean.getRef(),
                                    "<audio  src=\"" + mediaUrl +
                                            "\" controls=\"controls\" src=\"" + videoBean.getCover() + "\"></audio >");
                            type = TYPE_AUDIO;
                        } else {
                            if (AppConfig.isNoImage)
                                html = html.replace(videoBean.getRef(),
                                        "<img src=\"" + videoBean.getCover() + "\"");
                            else
                                html = html.replace(videoBean.getRef(),
                                        "<video src=\"" + mediaUrl +
                                                "\" controls=\"controls\" poster=\"" + videoBean.getCover() + "\"type=\"video/mp4\"></video>");
                            videoBean.setUrl_mp4(mediaUrl); //自用链接,提供给悬浮窗和下载
                            videoList.add(videoBean);
                        }
                    }
                }
                if (currentData.getImg() != null) {
                    for (NewsDetailData.ImgBean imgBean : currentData.getImg()) {
                        html = html.replace(imgBean.getRef(), "<img src=\"" + imgBean.getSrc() + "\"/>");
                    }
                }
                e.onNext(true);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean b) throws Exception {
                        if (videoList.size() > 0) {
                            videoStub = (ViewStub) findViewById(R.id.stub_video);
                            videoStub.setVisibility(View.VISIBLE);
                            videoLayout = findViewById(R.id.fl_video_full);
                            videoLayout.setKeepScreenOn(true);
                        }
                        getSupportActionBar().setDisplayShowTitleEnabled(false);
                        if (b && webView != null) {
                            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", "about:blank");
//                            webView.loadData(html, "text/html; charset=UTF-8", null);
                            saveCacheAsyn(CACHE_HISTORY);
                        }
                    }
                });
    }

    private String deleteAd(String data) {
        if (data.contains("<!--SPINFO")) {
            int index = data.indexOf("<!--SPINFO");
            int imgHead = data.indexOf("<!--IMG#", index);
            if (imgHead != -1) {
                int imgEnd = data.indexOf("-->", imgHead);
                String resultStart = data.substring(0, imgHead - 1);
                String resultEnd = data.substring(imgEnd + 3, data.length() - 1);
                data = resultStart + resultEnd;
            }
        }
        if (data.contains("<p>原标题：")) {
            int imgHead = data.indexOf("<p>原标题：");
            int imgEnd = data.indexOf("</p>", imgHead);
            String resultStart = data.substring(0, imgHead - 1);
            String resultEnd = data.substring(imgEnd + 4, data.length() - 1);
            data = resultStart + resultEnd;
        }
        return data;
    }

    private void saveCacheAsyn(int type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (currentData == null)
                    return;
                List<CacheNews> list = new ArrayList<>();
                try {
                    list = LitePal.where("docid = ?", currentData.getDocid()).find(CacheNews.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!isEmpty(list))
                    for (CacheNews cacheNews : list) {
                        if (cacheNews.getType() == type)
                            return;
                    }
                CacheNews cacheNews = new CacheNews(currentData.getTitle(),
                        currentData.getRecImgsrc(),
                        currentData.getSource(),
                        currentData.getDocid(),
                        html,
                        type,
                        TYPE_NETEASE_START);
                cacheNews.save();
            }
        }).start();
    }

    private boolean checkStar(boolean isClear) {
        List<CacheNews> list = null;
        try {
            list = LitePal.where("docid = ?", currentData.getDocid()).find(CacheNews.class);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        if (!isEmpty(list)) {
            for (CacheNews cacheNews : list) {
                if (cacheNews.getType() == CACHE_COLLECTION) {
                    if (isClear) {
                        LitePal.delete(CacheNews.class, cacheNews.getId());
                        setResult(RESULT_OK);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void initWeb() {
        super.initWeb();
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (webView == null) return;
                webView.loadUrl("javascript:document.body.style.paddingBottom=\"" + ConvertUtils.dp2px(16) + "px\"; void 0");
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                ScreenUtils.getScreenRotation(mActivity);
                fullScreen(true);
                ScreenUtils.setFullScreen(mActivity);
                webView.setVisibility(View.GONE);
                videoLayout.setVisibility(View.VISIBLE);
                videoLayout.addView(view);
                super.onShowCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                fullScreen(false);
                ScreenUtils.setNonFullScreen(mActivity);
                webView.setVisibility(View.VISIBLE);
                videoLayout.setVisibility(View.GONE);
                videoLayout.removeAllViews();
                super.onHideCustomView();

            }
        });

        webView.setWebViewClient(new DetailWebViewClient());
    }

    private void fullScreen(boolean isStart) {
        if (isStart) {
            toolbar.setVisibility(View.GONE);
            isLand = ScreenUtils.isLandscape();
            try {
                if (currentData != null && currentData.getVideo().get(0).getVideoRatio() > 1) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            toolbar.setVisibility(View.VISIBLE);
            if (isLand)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        }
    }

    private void loadVideo(MyJzvdStd videoView) {
        NewsDetailData.VideoBean videoItem = videoList.get(videoIndex);
        videoView.setUp(videoItem.getUrl_mp4(), videoItem.getAlt(), Jzvd.SCREEN_WINDOW_NORMAL);
        if (!TextUtils.isEmpty(videoItem.getCover()) && GlideUtils.isValidContextForGlide(mActivity))
            Glide.with(mActivity)
                    .load(videoItem.getCover())
                    .into(videoView.thumbImageView);
    }

    private void loadError() {
        ToastUtils.showShort(getString(R.string.server_fail));
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (type != TYPE_AUDIO)
            webView.onPause();
        if (videoList.size() > 0)
            Jzvd.releaseAllVideos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (type != TYPE_AUDIO)
            webView.onResume();
    }

    @Override
    public void onBackPressed() {
        if (videoList.size() > 0 && Jzvd.backPress()) {
            return;
        }
        super.onBackPressed();
    }
}
