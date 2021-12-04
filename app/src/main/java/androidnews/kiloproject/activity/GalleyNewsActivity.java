package androidnews.kiloproject.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.blankj.utilcode.util.SnackbarUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.gyf.immersionbar.ImmersionBar;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.DownloadProgressCallBack;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.entity.net.GalleyData;
import androidnews.kiloproject.system.base.BaseActivity;
import androidnews.kiloproject.util.FileCompatUtils;
import androidnews.kiloproject.widget.PinchImageView;

import static androidnews.kiloproject.system.AppConfig.isSwipeBack;
import static com.blankj.utilcode.util.CollectionUtils.isEmpty;

public class GalleyNewsActivity extends BaseActivity {

    ViewPager galleyViewpager;
    TextView tvGalleyTitle;
    TextView tvGalleyText;
    TextView tvGalleyPage;
    ProgressBar progressBar;
    View btnGalleyDownload;
    View mArrowDownBtn;
    private int currentIndex = 0;
    private ArrayList<String> imgList;
    private String currentTitle;
    private String currentDesc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galley_news);

        if (isLollipop()) postponeEnterTransition();

        galleyViewpager = (ViewPager) findViewById(R.id.galley_viewpager);
        tvGalleyTitle = (TextView) findViewById(R.id.tv_galley_title);
        tvGalleyText = (TextView) findViewById(R.id.tv_galley_text);
        tvGalleyPage = (TextView) findViewById(R.id.tv_galley_page);
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
        String skipID = getIntent().getStringExtra("skipID");
        if (!TextUtils.isEmpty(skipID)) {
            EasyHttp.get("/photo/api/set/" + skipID)
                    .readTimeOut(30 * 1000)//局部定义读超时
                    .writeTimeOut(30 * 1000)
                    .connectTimeout(30 * 1000)
                    .timeStamp(true)
                    .execute(new SimpleCallBack<String>() {
                        @Override
                        public void onError(ApiException e) {
                            SnackbarUtils.with(galleyViewpager).setMessage(getString(R.string.load_fail) + e.getMessage()).showError();
                        }

                        @Override
                        public void onSuccess(String response) {
                            try {
                                GalleyData galleyContent = gson.fromJson(response, GalleyData.class);
                                initGalley(galleyContent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                SnackbarUtils.with(galleyViewpager).setMessage(getString(R.string.load_fail)).showError();
                            }
                        }
                    });
        } else {
            currentIndex = getIntent().getIntExtra("index", 0);
            imgList = getIntent().getStringArrayListExtra("imgs");
            if (currentIndex != -1 && !isEmpty(imgList))
                initGalley();
        }
    }


    private void initGalley(GalleyData galleyContent) {
        progressBar.setVisibility(View.GONE);

        if (isEmpty(galleyContent.getPhotos()))
            return;
        imgList = new ArrayList<>();
        for (GalleyData.PhotosBean photo : galleyContent.getPhotos()) {
            imgList.add(photo.getImgurl());
        }
        currentTitle = galleyContent.getSetname();
        currentDesc = galleyContent.getPhotos().get(0).getNote();

        initViewpager(true);
        galleyViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // 滚动的时候改变自定义控件的动画
                if (swipePanel != null) {
                    swipePanel.setLeftEnabled(position == 0 && isSwipeBack);
                    swipePanel.setRightEnabled(position == galleyContent.getPhotos().size() - 1 && isSwipeBack);
                }
            }

            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                List<GalleyData.PhotosBean> beans = galleyContent.getPhotos();

                currentTitle = galleyContent.getSetname();
                currentDesc = beans.get(position).getNote();

                tvGalleyPage.setText((position + 1) + "/" + beans.size());
                tvGalleyTitle.setText(currentTitle);
                tvGalleyText.setText(currentDesc);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void initGalley() {
        progressBar.setVisibility(View.GONE);
        RequestOptions options = new RequestOptions();
        options.error(R.drawable.ic_error);

        initViewpager(false);
        galleyViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // 滚动的时候改变自定义控件的动画
                if (swipePanel != null) {
                    swipePanel.setLeftEnabled(position == 0 && isSwipeBack);
                    swipePanel.setRightEnabled(position == imgList.size() - 1 && isSwipeBack);
                }
            }

            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                tvGalleyPage.setText((position + 1) + "/" + imgList.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void initViewpager(boolean isGalleyNews) {
        tvGalleyPage.setText((currentIndex + 1) + "/" + imgList.size());

        if (isGalleyNews) {
            tvGalleyTitle.setText(currentTitle);
            tvGalleyText.setText(currentDesc);
        } else {
            tvGalleyTitle.setVisibility(View.GONE);
            tvGalleyText.setVisibility(View.GONE);
            mArrowDownBtn.setVisibility(View.GONE);
        }

        RequestOptions options = new RequestOptions();
        options.error(R.drawable.ic_error);

        galleyViewpager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return imgList.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object o) {
                return view == o;
            }

            @Override
            public Object instantiateItem(ViewGroup container, final int position) {
                String imageUrl = imgList.get(position);
                ImageView imgView;
                if (imageUrl.endsWith(".gif")) {
                    imgView = new ImageView(mActivity);
                    Glide.with(mActivity)
                            .asGif()
                            .load(imageUrl)
                            .apply(options)
                            .into(new SimpleTarget<GifDrawable>() {
                                @Override
                                public void onResourceReady(@NonNull GifDrawable resource, @Nullable Transition<? super GifDrawable> transition) {
                                    imgView.setImageDrawable(resource);
                                    if (position == 0) supportStartPostponedEnterTransition();
                                    resource.start();
                                }
                            });
                }else {
                    imgView = new PinchImageView(mActivity);
                    Glide.with(mActivity)
                            .load(imageUrl)
                            .apply(options)
                            .into(new SimpleTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    imgView.setImageDrawable(resource);
                                    if (position == 0) supportStartPostponedEnterTransition();
                                }
                            });
                }
                container.addView(imgView);
                return imgView;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }

            @Override
            public void setPrimaryItem(ViewGroup container, int position, Object object) {
            }
        });
        if (currentIndex != 0) galleyViewpager.setCurrentItem(currentIndex);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_galley_download:
                if (!isEmpty(imgList)) {
                    String currentImg = imgList.get(currentIndex);
                    FileCompatUtils.downloadFile(mActivity, currentImg, tvGalleyTitle);
                }
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

