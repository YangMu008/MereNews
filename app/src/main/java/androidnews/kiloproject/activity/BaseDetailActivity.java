package androidnews.kiloproject.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.blankj.utilcode.util.ArrayUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.RomUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.ethanhua.skeleton.Skeleton;
import com.ethanhua.skeleton.SkeletonScreen;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ObservableWebView;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidnews.kiloproject.R;
import androidnews.kiloproject.system.AppConfig;
import androidnews.kiloproject.system.base.BaseActivity;

import static androidnews.kiloproject.system.AppConfig.isAutoNight;
import static androidnews.kiloproject.system.AppConfig.isNightMode;
import static com.blankj.utilcode.util.CollectionUtils.isEmpty;


public class BaseDetailActivity extends BaseActivity implements ObservableScrollViewCallbacks {

    Toolbar toolbar;
    ObservableWebView webView;
    SkeletonScreen skeletonScreen;
//    SmartRefreshLayout refreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        webView = (ObservableWebView) findViewById(R.id.web_news);

        webView.setBackgroundColor(0);
        webView.getBackground().setAlpha(0);
        webView.setScrollViewCallbacks(this);

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
        }

        initListener();

        if (!RomUtils.isMeizu() && AppConfig.isShowSkeleton)
            skeletonScreen = Skeleton.bind(webView)
                    .load(R.layout.layout_skeleton_news)
                    .duration(1000)
                    .color(R.color.main_background)
                    .show();
        initView();
        initBar(R.color.main_background, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (NetworkUtils.isConnected())
            getMenuInflater().inflate(R.menu.detail_items, menu);//加载menu布局
        return true;
    }

    private void initListener() {
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult result = ((WebView) v).getHitTestResult();
                if (null == result)
                    return false;
                int type = result.getType();
                final String url = result.getExtra();
                switch (type) {
                    case WebView.HitTestResult.UNKNOWN_TYPE: //未知
                    case WebView.HitTestResult.EDIT_TEXT_TYPE: // 选中的文字类型
                    case WebView.HitTestResult.PHONE_TYPE: // 处理拨号
                    case WebView.HitTestResult.EMAIL_TYPE: // 处理Email
                    case WebView.HitTestResult.GEO_TYPE: // 　地图类型
                    case WebView.HitTestResult.SRC_ANCHOR_TYPE: // 超链接
                        break;
                    case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE: // 带有链接的图片类型
                    case WebView.HitTestResult.IMAGE_TYPE: // 处理长按图片的菜单项
//                        if (!TextUtils.isEmpty(url)) {
//                            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
//                            builder.setTitle(R.string.download)
//                                    .setMessage(R.string.download_img_q)
//                                    .setCancelable(true)
//                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            downloadImg(url);
//                                        }
//    }).setNegativeButton(android.R.string.cancel,null).show()
//                            .getButton(Dialog.BUTTON_NEGATIVE)
//                            .setBackgroundColor(getResources()
//                                    .getColor(isNightMode ? R.color.awesome_background : android.R.color.darker_gray));
//                        }
                        return true;
                }
                return false;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    protected void initWeb() {
        WebSettings webSetting = webView.getSettings();
        webSetting.setJavaScriptEnabled(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setAllowFileAccess(true);
        webSetting.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSetting.setAppCacheEnabled(true);
        webSetting.setDatabaseEnabled(true);
        webSetting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSetting.setPluginState(WebSettings.PluginState.ON);
        if (isLollipop())
            webSetting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.addJavascriptInterface(new MJavascriptInterface(mActivity), "image_listener");

        switch (AppConfig.mTextSize) {
            case 0:
                webSetting.setTextZoom(130);
                break;
            case 1:
                webSetting.setTextZoom(100);
                break;
            case 2:
                webSetting.setTextZoom(70);
                break;
            case 3:
                webSetting.setTextZoom(160);
                break;
            case 4:
                webSetting.setTextZoom(50);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            // 如果先调用destroy()方法，则会命中if (isDestroyed()) return;这一行代码，需要先onDetachedFromWindow()，再
            // destory()
            ViewParent parent = webView.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(webView);
            }

            webView.stopLoading();
            // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
            webView.getSettings().setJavaScriptEnabled(false);
            webView.clearHistory();
            webView.clearView();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    protected void initSlowly() {
    }

    protected void initView() {
    }

    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
    }

    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
        Log.e("DEBUG", "onUpOrCancelMotionEvent: " + scrollState);
        if (scrollState == ScrollState.UP) {
            if (toolbarIsShown()) {
                hideToolbar();
            }
        } else if (scrollState == ScrollState.DOWN) {
            if (toolbarIsHidden()) {
                showToolbar();
            }
        }
    }

    private boolean toolbarIsShown() {
        return ViewHelper.getTranslationY(toolbar) == 0;
    }

    private boolean toolbarIsHidden() {
        return ViewHelper.getTranslationY(toolbar) == -toolbar.getHeight();
    }

    private void showToolbar() {
        moveToolbar(0);
    }

    private void hideToolbar() {
        moveToolbar(-toolbar.getHeight());
    }

    private void moveToolbar(float toTranslationY) {
        if (ViewHelper.getTranslationY(toolbar) == toTranslationY) {
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(ViewHelper.getTranslationY(toolbar), toTranslationY).setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (webView == null || toolbar == null) return;
                float translationY = (float) animation.getAnimatedValue();
                ViewHelper.setTranslationY(toolbar, translationY);
                ViewHelper.setTranslationY(webView, translationY);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) (webView).getLayoutParams();
                lp.height = (int) -translationY + findViewById(android.R.id.content).getHeight() - lp.topMargin;
                (webView).requestLayout();
            }
        });
        animator.start();
    }

    protected void hideSkeleton() {
        if (!RomUtils.isMeizu() && AppConfig.isShowSkeleton)
            skeletonScreen.hide();
    }

    private static final String regEx_style = "<style[^>]*?>[\\s\\S]*?<\\/style>"; //定义style的正则表达式
    private static final String regEx_html = "<[^>]+>"; //定义HTML标签的正则表达式

    public String deleteHtml(String htmlStr) {
        Pattern p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
        Matcher m_style = p_style.matcher(htmlStr);
        htmlStr = m_style.replaceAll(""); //过滤style标签

        Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
        Matcher m_html = p_html.matcher(htmlStr);
        htmlStr = m_html.replaceAll(""); //过滤html标签

        htmlStr = htmlStr.replace(" ", "");
        htmlStr = htmlStr.replaceAll("\\s*|\t|\r|\n", "");
        htmlStr = htmlStr.replace("“", "");
        htmlStr = htmlStr.replace("”", "");
        htmlStr = htmlStr.replaceAll("　", "");
        htmlStr = htmlStr.replaceAll("&nbsp;", "");

        if (!TextUtils.isEmpty(htmlStr) && mActivity instanceof ZhiHuDetailActivity) {
            htmlStr = htmlStr.substring(htmlStr.indexOf("知乎日报每日提供高质量新闻资讯打开App") + 20, htmlStr.indexOf("进入「知乎」查看相关讨论"));
        }

        if (!TextUtils.isEmpty(htmlStr) && mActivity instanceof SmartisanDetailActivity) {
            int index = htmlStr.indexOf("本文由头条号授权锤子阅读转码");
            if (index > 0) {
                htmlStr = htmlStr.substring(0, htmlStr.indexOf("本文由头条号授权锤子阅读转码"));
            }
        }

        if (!TextUtils.isEmpty(htmlStr) && mActivity instanceof NewsDetailActivity) {
            if (htmlStr.endsWith("</p"))
                htmlStr = htmlStr.substring(0, htmlStr.length() - 3);
        }

        if (!TextUtils.isEmpty(htmlStr) && mActivity instanceof GuoKrDetailActivity) {
            htmlStr = htmlStr.substring(0, htmlStr.indexOf("来自果壳，查看原文"));
        }

        return htmlStr;
    }

    public class MJavascriptInterface {
        private Context mContext;
        private ArrayList<String> imageUrls;

        public MJavascriptInterface(Context context) {
            this.mContext = context;
        }

        @JavascriptInterface
        public void openImage(String selectImg) {
            if (!isEmpty(imageUrls)) {
                Intent intent = new Intent();
                intent.putExtra("imgs", imageUrls);
                intent.putExtra("index", imageUrls.indexOf(selectImg));
                intent.setClass(mContext, GalleyNewsActivity.class);
                mContext.startActivity(intent);
            }
        }

        @JavascriptInterface
        public void getImg(String[] imgArry) {
            this.imageUrls = (ArrayList<String>) ArrayUtils.asArrayList(imgArry);
        }
    }

    protected String getCssStr(int resId){
        InputStream is = getResources().openRawResource(resId);
        byte[] buffer = new byte[0];
        try {
            buffer = new byte[is.available()];
            is.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Base64.encodeToString(buffer, Base64.NO_WRAP);
    }

    protected String createHtmlText(String title,String source,String time,String body){
        String colorBody = isNightMode ? "<body bgcolor=\"#212121\" body text=\"#cccccc\">" : "<body text=\"#333\">";
        String html = "<!DOCTYPE html>" +
                "<html lang=\"zh\">" +
                "<head>" +
                "<meta charset=\"UTF-8\" />" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />" +
                "<meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\" />" +
                "<title>Document</title>" +
                "<style type=\"text/css\">" +
                "body{" +
                "margin-left:18px;" +
                "margin-right:18px;}" +
                "p {line-height:36px;}" +
                "body img{" +
                "width: 100%;" +
                "height: 100%;}" +
                "body video{" +
                "width: 100%;" +
                "height: 100%;}" +
                "p{margin: 25px auto}" +
                "div{width:100%;height:30px;} #from{width:auto;float:left;color:gray;} #time{width:auto;float:right;color:gray;}" +
                "</style>" +
                "</head>" +
                colorBody +
                "<p><h2>" + title + "</h2></p>" +
                "<p><div><div id=\"from\">" + source +
                "</div><div id=\"time\">" + time + "</div></div></p>" +
                "<font size=\"4\" face=\"system-ui\">" +
                body + "</font>" +
                "</body>" +
                "</html>";
        return html;
    }
}