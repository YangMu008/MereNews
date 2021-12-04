package androidnews.kiloproject.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import com.blankj.utilcode.util.SnackbarUtils;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import java.util.ArrayList;
import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.adapter.CommentAdapter;
import androidnews.kiloproject.entity.data.CommentLevel;
import androidnews.kiloproject.entity.net.NewsCommonData;
import androidnews.kiloproject.system.base.BaseActivity;


import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static androidnews.kiloproject.adapter.CommentAdapter.LEVEL_ONE;
import static androidnews.kiloproject.adapter.CommentAdapter.LEVEL_TWO;
import static androidnews.kiloproject.system.AppConfig.GET_NEWS_COMMENT;
import static androidnews.kiloproject.system.AppConfig.HOST_163_COMMENT;
import static com.blankj.utilcode.util.CollectionUtils.isEmpty;

public class CommentActivity extends BaseActivity {

    Toolbar toolbar;
    ProgressBar progress;
    CommentAdapter commentAdapter;
    RecyclerView rvContent;
    ConstraintLayout emptyView;

    private List<CommentLevel> comments = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview);
        toolbar = findViewById(R.id.toolbar);
        progress = findViewById(R.id.progress);
        rvContent = findViewById(R.id.rv_content);
        emptyView = findViewById(R.id.empty_view);

        initToolbar(toolbar, true);
        getSupportActionBar().setTitle(R.string.action_comment);
        initBar(R.color.main_background, true);
    }

    @Override
    protected void initSlowly() {
        String docid = getIntent().getStringExtra("docid");
        String board = getIntent().getStringExtra("board");
        EasyHttp.get(GET_NEWS_COMMENT.replace("{board}",board).replace("{docid}",docid))
                .baseUrl(HOST_163_COMMENT)
                .readTimeOut(30 * 1000)//局部定义读超时
                .writeTimeOut(30 * 1000)
                .connectTimeout(30 * 1000)
                .timeStamp(true)
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        progress.setVisibility(View.GONE);
                        SnackbarUtils.with(toolbar).setMessage(getString(R.string.load_fail) + e.getMessage()).showError();
                        emptyView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onSuccess(String response) {
                        progress.setVisibility(View.GONE);
                        if (!TextUtils.isEmpty(response) || TextUtils.equals(response, "{}")) {
                            NewsCommonData data = null;
                            try {
                                data = gson.fromJson(response, NewsCommonData.class);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            if (data != null && !isEmpty(data.getNewPosts()))
                                analysisData(data);
                            else
                                emptyView.setVisibility(View.VISIBLE);
                        } else {
                            progress.setVisibility(View.GONE);
                            SnackbarUtils.with(toolbar).setMessage(getString(R.string.load_fail)).showError();
                        }
                    }
                });
    }

    private void analysisData(NewsCommonData data) {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                for (NewsCommonData.NewPostsBean floor : data.getNewPosts()) {
                    NewsCommonData.NewPostsBean._$1Bean layer = floor.get_$1();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_ONE));
                    }

                    layer = floor.get_$2();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    } else {
                        continue;
                    }

                    layer = floor.get_$3();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    } else {
                        continue;
                    }

                    layer = floor.get_$4();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    } else {
                        continue;
                    }

                    layer = floor.get_$5();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    } else {
                        continue;
                    }

                    layer = floor.get_$6();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    } else {
                        continue;
                    }

                    layer = floor.get_$7();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    } else {
                        continue;
                    }

                    layer = floor.get_$8();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    } else {
                        continue;
                    }

                    layer = floor.get_$9();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    } else {
                        continue;
                    }

                    layer = floor.get_$10();
                    if (layer != null) {
                        comments.add(new CommentLevel(layer.getTimg(), layer.getF(), layer.getB(), layer.getT(), LEVEL_TWO));
                    }
                }
                e.onNext(true);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        commentAdapter = new CommentAdapter(mActivity, comments);
                        rvContent.setLayoutManager(new LinearLayoutManager(mActivity));
                        rvContent.setAdapter(commentAdapter);
                    }
                });
    }
}
