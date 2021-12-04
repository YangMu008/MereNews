package androidnews.kiloproject.adapter;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.entity.net.CnBetaListData;
import androidnews.kiloproject.system.AppConfig;
import androidnews.kiloproject.util.GlideUtils;

import static androidnews.kiloproject.util.TimeUtils.timeStrToTimelineTime;

public class CnBetaAdapter extends BaseQuickAdapter<CnBetaListData.ResultBean, BaseViewHolder> {
    RequestOptions options;
    private Context mContext;

    public CnBetaAdapter(Context Context, List<CnBetaListData.ResultBean> data) {
        super(R.layout.list_item_card_linear_smaill_pic, data);
        this.mContext = Context;
        options = new RequestOptions();
        options.centerCrop()
                .error(R.drawable.ic_error);
    }

    @Override
    protected void convert(BaseViewHolder helper, CnBetaListData.ResultBean item) {
        try {
            helper.setText(R.id.item_card_text, item.getTitle());
            helper.setText(R.id.item_card_time, timeStrToTimelineTime(item.getPubtime()));
            helper.setText(R.id.item_card_subtitle, item.getSummary());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (item.isReaded()) {
            helper.setTextColor(R.id.item_card_text,
                    mContext.getResources().getColor(R.color.main_text_color_read));
            helper.setTextColor(R.id.item_card_subtitle,
                    mContext.getResources().getColor(R.color.main_text_color_read));
        } else {
            helper.setTextColor(R.id.item_card_text,
                    mContext.getResources().getColor(R.color.main_text_color_dark));
            helper.setTextColor(R.id.item_card_subtitle,
                    mContext.getResources().getColor(R.color.main_text_color_dark));
        }
        if (!AppConfig.isNoImage && GlideUtils.isValidContextForGlide(mContext)) {
            Glide.with(mContext).load(item.getThumb())
                    .apply(options)
                    .into((ImageView) helper.getView(R.id.item_card_img));
            Glide.with(mContext).load(item.getTopic_logo())
                    .apply(options)
                    .into((ImageView) helper.getView(R.id.item_card_img_logo));
        } else {
            helper.setImageResource(R.id.item_card_img, R.drawable.ic_news_pic);
            helper.setImageResource(R.id.item_card_img_logo, R.mipmap.ic_launcher);
        }
    }
}
