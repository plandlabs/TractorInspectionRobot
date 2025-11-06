package kr.re.kitech.tractorinspectionrobot.helper;

public interface ItemTouchHelper {
    void onItemMove(int fromPos, int targetPos);
    void onItemDismiss(int pos); //swipe 사용하려면 주석 해제
}
