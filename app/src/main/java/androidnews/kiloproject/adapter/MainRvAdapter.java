package androidnews.kiloproject.adapter;

import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.youth.banner.Banner;
import com.youth.banner.indicator.CircleIndicator;
import com.youth.banner.listener.OnBannerListener;

import java.util.ArrayList;
import java.util.List;

import androidnews.kiloproject.entity.data.ListBannerData;
import androidnews.kiloproject.system.AppConfig;
import androidnews.kiloproject.R;
import androidnews.kiloproject.activity.GalleyNewsActivity;
import androidnews.kiloproject.activity.NewsDetailActivity;
import androidnews.kiloproject.entity.net.NewMainListData;
import androidnews.kiloproject.system.base.BaseActivity;
import androidnews.kiloproject.util.GlideUtils;

import static androidnews.kiloproject.fragment.BaseRvFragment.CELL;
import static androidnews.kiloproject.fragment.BaseRvFragment.CELL_EXTRA;
import static androidnews.kiloproject.fragment.BaseRvFragment.HEADER;
import static androidnews.kiloproject.util.TimeUtils.timeStrToTimelineTime;
import static com.blankj.utilcode.util.ActivityUtils.startActivity;
import static com.blankj.utilcode.util.ObjectUtils.isNotEmpty;

public class MainRvAdapter extends BaseMultiItemQuickAdapter<NewMainListData, BaseViewHolder> {
    RequestOptions options;
    private Context mContext;
    private RecyclerView.RecycledViewPool childRvPool;

    public MainRvAdapter(Context Context, List data) {
        super(data);
        this.mContext = Context;
        addItemType(HEADER, R.layout.list_item_card_banner);
        addItemType(CELL, R.layout.list_item_card_linear);
        addItemType(CELL_EXTRA, R.layout.list_item_card_linear_extra);
        options = new RequestOptions();
        options.centerCrop()
                .error(R.drawable.ic_error);
    }

    @Override
    protected void convert(BaseViewHolder helper, NewMainListData item) {
        switch (helper.getItemViewType()) {
            case HEADER:
                Banner banner = (Banner) helper.getView(R.id.banner);
                List<ListBannerData> imgs = new ArrayList<>();
                imgs.add(new ListBannerData(item.getImgsrc(),item.getTitle()));
                if (item.getAds() != null)
                    for (NewMainListData.AdsBean bean : item.getAds()) {
                        imgs.add(new ListBannerData(bean.getImgsrc(),bean.getTitle()));
                    }
                banner.addBannerLifecycleObserver((BaseActivity)mContext)//添加生命周期观察者
                        .setAdapter(new ListBannerAdapter(imgs))
                        .setDelayTime(8 * 1000)
                        .setIndicator(new CircleIndicator(mContext))
                        .setOnBannerListener(new OnBannerListener() {
                            @Override
                            public void OnBannerClick(Object data, int position) {
                                String skipID = "";
                                String rawId;
                                if (position == 0)
                                    rawId = item.getSkipID();
                                else
                                    rawId = item.getAds().get(position - 1).getSkipID();
                                Intent intent;
                                if (!TextUtils.isEmpty(rawId)) {
                                    int index = rawId.lastIndexOf("|");
                                    if (index != -1) {
                                        skipID = rawId.substring(index - 4, rawId.length());
                                        intent = new Intent(mContext, GalleyNewsActivity.class);
                                        intent.putExtra("skipID", skipID.replace("|", "/") + ".json");
                                        ((BaseActivity)mContext).startTransition(intent,banner);
                                    } else {
                                        ToastUtils.showShort(R.string.server_fail);
                                        return;
                                    }
                                } else {
                                    intent = new Intent(mContext, NewsDetailActivity.class);
                                    intent.putExtra("docid", item.getDocid().replace("_special", "").trim());
                                    startActivity(intent);
                                }
                            }
                        }).start();

                break;
            case CELL:
                try {
                    helper.setText(R.id.item_card_text, item.getTitle());
                    helper.setText(R.id.item_card_time, timeStrToTimelineTime(item.getPtime()));
                    helper.setText(R.id.item_card_info, item.getSource().replace("$", ""));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (item.isReaded())
                    helper.setTextColor(R.id.item_card_text,
                            mContext.getResources().getColor(R.color.main_text_color_read));
                else
                    helper.setTextColor(R.id.item_card_text,
                            mContext.getResources().getColor(R.color.main_text_color_dark));
                if (TextUtils.isEmpty(item.getImgsrc())) {
                    if (isNotEmpty(item.getDigest())) {
                        helper.setText(R.id.item_card_subtitle, item.getDigest().replace("&nbsp", ""));
                        helper.setImageResource(R.id.item_card_img, R.color.white);
                    }
                } else {
                    if (!AppConfig.isNoImage && GlideUtils.isValidContextForGlide(mContext))
                        Glide.with(mContext).load(item.getImgsrc())
                                .apply(options)
                                .into((ImageView) helper.getView(R.id.item_card_img));
                    else
                        helper.setImageResource(R.id.item_card_img, R.drawable.ic_news_pic);
                    helper.setText(R.id.item_card_subtitle, "");
                }
                break;
            case CELL_EXTRA:
                ImageView ivPic = helper.getView(R.id.item_card_img);
                TextView tvInfo = helper.getView(R.id.item_card_info);
                try {
                    helper.setText(R.id.item_card_text, item.getTitle());
                    helper.setText(R.id.item_card_time, timeStrToTimelineTime(item.getPtime()));
                    tvInfo.setText(item.getSource().replace("$", ""));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (GlideUtils.isValidContextForGlide(mContext))
                    Glide.with(mContext).load(item.getImgsrc())
                            .apply(options)
                            .into(ivPic);

                View.OnClickListener listener = new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, NewsDetailActivity.class);
                        intent.putExtra("docid", item.getDocid().replace("_special", "").trim());
                        if (!item.isReaded()) {
                            item.setReaded(true);
                            notifyItemChanged(helper.getAdapterPosition());
                        }
                        startActivity(intent);
                    }
                };
                ivPic.setOnClickListener(listener);
                tvInfo.setOnClickListener(listener);

                RecyclerView recyclerView = helper.getView(R.id.rv_extra);
                ExtraAdapter adapter = new ExtraAdapter(mContext,item.getSpecialextra());
                adapter.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                        Intent intent = new Intent(mContext, NewsDetailActivity.class);
                        NewMainListData.SpecialextraBean bean = item.getSpecialextra().get(position);
                        intent.putExtra("docid", bean.getDocid().replace("_special", "").trim());
                        if (!bean.isReaded()) {
                            bean.setReaded(true);
                            adapter.notifyItemChanged(position);
                        }
                        startActivity(intent);
                    }
                });
                recyclerView.setAdapter(adapter);
                recyclerView.setHasFixedSize(true);
                recyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
                if (childRvPool != null)
                    recyclerView.setRecycledViewPool(childRvPool);
                else
                    childRvPool = recyclerView.getRecycledViewPool();

                recyclerView.setLayoutManager(new LinearLayoutManager(mContext));

                TextView tvOpen = helper.getView(R.id.tv_open);
                tvOpen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        item.setOpen(true);
                        notifyItemChanged(helper.getAdapterPosition());
                    }
                });

                if (item.isOpen()){
                    tvOpen.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }else {
                    tvOpen.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }

                break;
        }
    }

    class ExtraAdapter extends BaseQuickAdapter<NewMainListData.SpecialextraBean, BaseViewHolder>{
        RequestOptions options;
        private Context mContext;

        public ExtraAdapter(Context Context, List data) {
            super(R.layout.layout_linear_extra_item, data);
            this.mContext = Context;
            options = new RequestOptions();
            options.centerCrop()
                    .error(R.drawable.ic_error);
        }

        @Override
        protected void convert(BaseViewHolder helper, NewMainListData.SpecialextraBean item) {
            try {
                helper.setText(R.id.item_card_text, item.getTitle());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (item.isReaded())
                helper.setTextColor(R.id.item_card_text,
                        mContext.getResources().getColor(R.color.main_text_color_read));
            else
                helper.setTextColor(R.id.item_card_text,
                        mContext.getResources().getColor(R.color.main_text_color_dark));

            if (GlideUtils.isValidContextForGlide(mContext))
                Glide.with(mContext).load(item.getImgsrc())
                        .apply(options)
                        .into((ImageView) helper.getView(R.id.item_card_img));
        }
    }
}
