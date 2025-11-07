package kr.re.kitech.tractorinspectionrobot.views.tapPager;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.views.fragment.PageFragment0;

import kr.re.kitech.tractorinspectionrobot.views.fragment.PageFragment1;
import lombok.Getter;
import lombok.Setter;

public class TabFragmentPagerAdapter extends FragmentPagerAdapter {
    private int tabCount;
    private Context context; // 추가
    private boolean isAdmin;
    @Setter
    @Getter
    private int robotOptDivision = 0;
    public TabFragmentPagerAdapter(FragmentManager fm, int tabCount, Context context, SharedPreferences setting) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.isAdmin = "admin".equals(setting.getString("ID", ""));
        this.tabCount = tabCount;
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return PageFragment0.newInstance();
            case 1:
                return PageFragment1.newInstance();
        }
        return null;
    }

    @Override
    public int getCount() {
        return tabCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getString(R.string.bottom_menu_00);
            case 1:
                return context.getString(R.string.bottom_menu_01);
        }
        return null;
    }
}
