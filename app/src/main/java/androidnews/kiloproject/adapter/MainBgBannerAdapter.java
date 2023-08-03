package androidnews.kiloproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.youth.banner.adapter.BannerAdapter;

import java.util.List;

import androidnews.kiloproject.R;
import androidnews.kiloproject.entity.data.MainBgBannerData;
import androidnews.kiloproject.util.GlideUtils;

public class MainBgBannerAdapter extends BannerAdapter<MainBgBannerData, MainBgBannerAdapter.BannerViewHolder> {

    RequestOptions options;

    public MainBgBannerAdapter(List<MainBgBannerData> mDatas) {
        //设置数据，也可以调用banner提供的方法,或者自己在adapter中实现
        super(mDatas);
        options = new RequestOptions();
        options.centerCrop()
                .error(R.drawable.ic_error);
    }

    //创建ViewHolder，可以用viewType这个字段来区分不同的ViewHolder
    @Override
    public BannerViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_main_bg_banner_item, parent,false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindView(BannerViewHolder holder, MainBgBannerData data, int position, int size) {
        /**
         注意：
         1.图片加载器由自己选择，这里不限制，只是提供几种使用方法
         2.返回的图片路径为Object类型，由于不能确定你到底使用的那种图片加载器，
         传输的到的是什么格式，那么这种就使用Object接收和返回，你只需要强转成你传输的类型就行，
         切记不要胡乱强转！
         */
        if (GlideUtils.isValidContextForGlide(holder.rootView.getContext()))
            Glide.with(holder.rootView.getContext())
                    .load(data.getPath())
                    .apply(options)
                    .into(holder.ivImg);
    }

    class BannerViewHolder extends RecyclerView.ViewHolder {
        View rootView;
        ImageView ivImg;

        public BannerViewHolder(@NonNull View view) {
            super(view);
            this.rootView = view;
            this.ivImg = view.findViewById(R.id.iv_img);
        }
    }
}