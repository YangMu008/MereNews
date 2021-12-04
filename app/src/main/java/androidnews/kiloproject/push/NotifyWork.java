package androidnews.kiloproject.push;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import androidnews.kiloproject.util.PushUtils;
import static com.blankj.utilcode.util.NetworkUtils.getMobileDataEnabled;
import static com.blankj.utilcode.util.NetworkUtils.getWifiEnabled;

public class NotifyWork extends Worker {
    private Context mContext;
    private final static String typeStr = "T1348647853363";

    public NotifyWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        /**
         //         * WorkerResult.SUCCESS //成功
         //         * WorkerResult.FAILURE //失败
         //         * WorkerResult.RETRY //重试
         //         */
        //接收传递进来的参数
//        String parame_str = this.getInputData().getString("workparame");

        //执行网络请求
        try {
            if (getWifiEnabled() || getMobileDataEnabled())
                PushUtils.getPushList(mContext);
        } catch (Exception e) {
            return Result.failure();
        }
        return Result.success();
    }
}