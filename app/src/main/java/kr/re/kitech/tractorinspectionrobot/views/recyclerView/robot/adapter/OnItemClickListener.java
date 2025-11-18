package kr.re.kitech.tractorinspectionrobot.views.recyclerView.robot.adapter;

import android.view.View;

public interface OnItemClickListener
{
    void onItemClick(View v, int pos);
    void onItemDeleteClick(View v, int pos);
}