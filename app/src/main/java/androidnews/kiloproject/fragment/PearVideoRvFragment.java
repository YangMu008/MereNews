package androidnews.kiloproject.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import androidnews.kiloproject.R;
import androidnews.kiloproject.system.AppConfig;

import static androidnews.kiloproject.system.AppConfig.TYPE_PEAR_VIDEO;
import static androidnews.kiloproject.system.AppConfig.TYPE_ZHIHU;

public class PearVideoRvFragment extends BaseRvFragment {

//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        typeStr = getResources().getStringArray(R.array.address)[TYPE_PEAR_VIDEO];
//        this.CACHE_LIST_DATA = typeStr + "_data";
//        return super.onCreateView(inflater, container, savedInstanceState);
//    }
//
//    protected void onFragmentVisibleChange(boolean isVisible) {
//        if (isVisible) {
//            if (contents == null ||
//                    (AppConfig.isAutoRefresh) &&
//                            (System.currentTimeMillis() - lastAutoRefreshTime > dividerAutoRefresh)) {
//                refreshLayout.autoRefresh();
//            }
//        }
//    }

    @Override
    public void requestData(int type) {

    }
}
