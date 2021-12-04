package androidnews.kiloproject.web;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DetailWebViewClient extends WebViewClient {
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        addImageClickListener(view);//待网页加载完全后设置图片点击的监听方法
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    public static void addImageClickListener(WebView webView) {
        webView.loadUrl("javascript:(function(){" +
                "var imgArray = new Array();" +
                "var objs = document.getElementsByTagName(\"img\"); " +
                "for(var i = 0 ; i < objs.length ; i++)  " +
                "{" +
                "   imgArray[i] = objs[i].src;" +
                "   objs[i].onclick=function()  {" +
                "        window.image_listener.openImage(this.src);  " +
                "   }  " +
                "}" +
                "window.image_listener.getImg(imgArray);" +
                "})()");
    }
}