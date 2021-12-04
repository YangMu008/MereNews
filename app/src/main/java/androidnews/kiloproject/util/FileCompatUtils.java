package androidnews.kiloproject.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;

import com.blankj.utilcode.util.SnackbarUtils;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.DownloadProgressCallBack;
import com.zhouyou.http.exception.ApiException;

import java.io.File;

import androidnews.kiloproject.R;

public class FileCompatUtils {

    public static String getMediaDir(Context mContext) {
        String path = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            File[] files = mContext.getExternalMediaDirs();
            if (files != null && files.length > 0)
                path = files[0].getPath();
        } else
            path = "/sdcard/Download";
        return path;
    }

    public static void downloadFile(Context mContext, String fileUrl, View barRootView) {
        downloadFile(mContext,fileUrl,barRootView,false);
    }

    public static void downloadFile(Context mContext, String fileUrl, View barRootView, boolean isBigFile) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1, fileUrl.length());
            String path = FileCompatUtils.getMediaDir(mContext);
            EasyHttp.downLoad(fileUrl)
                    .savePath(path)
                    .saveName(fileName)//不设置默认名字是时间戳生成的
                    .execute(new DownloadProgressCallBack<String>() {
                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                        }

                        @Override
                        public void onStart() {
                            //开始下载
                            if (isBigFile)
                                SnackbarUtils.with(barRootView)
                                        .setMessage(mContext.getResources().getString(R.string.loading))
                                        .show();
                        }

                        @Override
                        public void onComplete(String path) {
                            //下载完成，path：下载文件保存的完整路径
                            SnackbarUtils.with(barRootView)
                                    .setMessage(mContext.getResources().getString(R.string.download_success))
                                    .show();
                            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
                        }

                        @Override
                        public void onError(ApiException e) {
                            //下载失败
                            SnackbarUtils.with(barRootView)
                                    .setMessage(mContext.getResources().getString(R.string.download_fail) + e.getMessage())
                                    .showError();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            SnackbarUtils.with(barRootView)
                    .setMessage(mContext.getResources().getString(R.string.download_fail))
                    .showError();
        }
    }
}
