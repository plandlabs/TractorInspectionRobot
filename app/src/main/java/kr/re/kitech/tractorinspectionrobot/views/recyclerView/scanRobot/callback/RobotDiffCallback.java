package kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.callback;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.model.RobotItem;

public class RobotDiffCallback extends DiffUtil.Callback {
    private final List<RobotItem> oldList;
    private final List<RobotItem> newList;

    public RobotDiffCallback(List<RobotItem> oldList, List<RobotItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // 고유 식별자(예: mac 주소 등)로 비교
        return oldList.get(oldItemPosition).getDeviceName()
                .equals(newList.get(newItemPosition).getDeviceName());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}

