package androidnews.kiloproject.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.blankj.utilcode.util.SnackbarUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.gyf.immersionbar.ImmersionBar;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.DownloadProgressCallBack;
import com.zhouyou.http.exception.ApiException;

import androidnews.kiloproject.R;
import androidnews.kiloproject.system.base.BaseActivity;
import androidnews.kiloproject.util.FileCompatUtils;
import androidnews.kiloproject.widget.PinchImageView;

public class GalleySimpleActivity extends BaseActivity {

    PinchImageView ivImg;
    TextView tvGalleyTitle;
    TextView tvGalleyText;
    ProgressBar progressBar;
    View btnGalleyDownload;
    View mArrowDownBtn;
    private String currentImg;
    private String currentTitle;
    private String currentDesc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galley_simple);

        if (isLollipop()) postponeEnterTransition();

        ivImg = (PinchImageView) findViewById(R.id.iv_img);
        tvGalleyTitle = (TextView) findViewById(R.id.tv_galley_title);
        tvGalleyText = (TextView) findViewById(R.id.tv_galley_text);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        btnGalleyDownload = (View) findViewById(R.id.btn_galley_download);
        mArrowDownBtn = (View) findViewById(R.id.btn_arrow_down);

//        ViewCompat.setTransitionName(galleyViewpager, "banner_pic");
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        if (ImmersionBar.hasNavigationBar(mActivity)) {
            ImmersionBar.with(this).transparentNavigationBar().init();
            if (ImmersionBar.isNavigationAtBottom(mActivity)) {
                int navHeight = ImmersionBar.getNavigationBarHeight(mActivity);
                Resources res = getResources();
                int dimNor = res.getDimensionPixelSize(R.dimen.margin_normal);
                int dimLarge = res.getDimensionPixelSize(R.dimen.margin_large);

                ConstraintLayout.LayoutParams lpDownLoad = (ConstraintLayout.LayoutParams) btnGalleyDownload.getLayoutParams();
                lpDownLoad.setMargins(0, 0, dimNor, dimLarge + navHeight);
            }
        }
    }

    @Override
    protected void initSlowly() {
        currentImg = getIntent().getStringExtra("img");
        currentTitle = getIntent().getStringExtra("title");
        currentDesc = getIntent().getStringExtra("desc");
        initGalley();
    }


    private void initGalley() {
        progressBar.setVisibility(View.GONE);
        if (TextUtils.isEmpty(currentImg))
            return;

        RequestOptions options = new RequestOptions();
        options.error(R.drawable.ic_error);

        tvGalleyTitle.setText(currentTitle);
        if (!TextUtils.isEmpty(currentDesc)) {
            tvGalleyText.setVisibility(View.VISIBLE);
            tvGalleyText.setText(currentDesc);
        }

        Glide.with(mActivity).load(currentImg).apply(options).into(new SimpleTarget<Drawable>() {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                ivImg.setImageDrawable(resource);
                supportStartPostponedEnterTransition();
            }
        });
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_galley_download:
                if (!TextUtils.isEmpty(currentImg))
                    FileCompatUtils.downloadFile(mActivity, currentImg, tvGalleyTitle);
                break;
            case R.id.btn_arrow_down:
                if (tvGalleyText.getVisibility() == View.VISIBLE) {
                    tvGalleyText.setVisibility(View.GONE);
                    mArrowDownBtn.setBackgroundResource(R.drawable.ic_arrow_up);
                } else {
                    tvGalleyText.setVisibility(View.VISIBLE);
                    mArrowDownBtn.setBackgroundResource(R.drawable.ic_arrow_down);
                }
                break;
        }
    }
}

