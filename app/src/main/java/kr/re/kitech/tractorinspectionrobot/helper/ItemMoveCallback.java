package kr.re.kitech.tractorinspectionrobot.helper;

import static android.content.ContentValues.TAG;

import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.rxjava3.annotations.NonNull;

public class ItemMoveCallback extends androidx.recyclerview.widget.ItemTouchHelper.Callback {
    private final ItemTouchHelper adapter;
    public boolean mOrderChanged;

    public ItemMoveCallback(ItemTouchHelper adapter) {
        this.adapter = adapter;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        //이벤트의 방향 설정
        // 아이템 좌우 배치
        int flagDrag = androidx.recyclerview.widget.ItemTouchHelper.START | androidx.recyclerview.widget.ItemTouchHelper.END | androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN; //item drag
        int flagSwipe = androidx.recyclerview.widget.ItemTouchHelper.START | androidx.recyclerview.widget.ItemTouchHelper.END; //item swipe
        // 아이템 상하 배치
        /*
        int flagDrag = ItemTouchHelper.UP | ItemTouchHelper.DOWN; //item drag
        int flagSwipe = ItemTouchHelper.START | ItemTouchHelper.END;//item swipe
        */
        //return makeMovementFlags(flagDrag, flagSwipe); //drag & swipe 사용
        return makeMovementFlags(flagDrag, 0); //swipe 액션 중지
        // return makeMovementFlags(0, flagSwipe); //drag 액션 중지
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder fromHolder, @NonNull RecyclerView.ViewHolder targetHolder) {
        //adapter의 onItemMove 호출
        adapter.onItemMove(fromHolder.getAdapterPosition(), targetHolder.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        //adapter의 onItemDismiss 호출
        adapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                        int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
        Log.d(TAG, "onMoved");
        // this is calling every time, but I need only when user dropped item, not after every onMove function.
    }
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE && mOrderChanged) {
            Log.d(TAG, "onSelectedChanged");
            mOrderChanged = false;
        }
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true; //롱터치 입력허용
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false; //swipe 사용하려면 return true;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        float dragScale = .97f;
        float dragAlpha = .75f;
        float dropValue = 1f;
        if (isCurrentlyActive && actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG) {
            itemView.setAlpha(dragAlpha);
            itemView.setScaleY(dragScale);
            itemView.setScaleX(dragScale);
        }else {
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    itemView.animate()
                            .alpha(dropValue)
                            .scaleY(dropValue)
                            .scaleX(dropValue)
                            .setDuration(400)
                            .setListener(null);

                }
            }, 300);
            //super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}

