package kr.re.kitech.tractorinspectionrobot.views.recyclerView.workField.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.helper.ItemTouchHelper;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.workField.model.WorkHoleItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkHoleRecyclerView extends RecyclerView.Adapter<WorkHoleRecyclerView.ViewHolder>
        implements ItemTouchHelper {
    private Context context;
    private ArrayList<WorkHoleItem> data;
    private LayoutInflater inflater;
    public int selectPosition = 0;
    private Vibrator mVibrator;

    public WorkHoleRecyclerView(ArrayList<WorkHoleItem> data, Context context) {
        this.data = data;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onItemMove(int fromPos, int targetPos) {
        mVibrator.vibrate(10);
        if (fromPos < targetPos) {
            for (int i = fromPos; i < targetPos; i++) {
                Collections.swap(data, i, i+1);
            }
        }else{
            for (int i = fromPos; i > targetPos; i--) {
                Collections.swap(data, i, i-1);
            }
        }
        notifyItemMoved(fromPos, targetPos);
    }

    @Override
    public void onItemDismiss(int pos) {

    }

    private static OnItemClickListener mListener = null;
    private static OnItemLongClickListener mLongListener = null;

    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.mListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener)
    {
        this.mLongListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cunstruct_hole, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            WorkHoleItem item = data.get(position);

            // 기본 뷰 설정

            holder.hole.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));;
            if(item.getType() == 1){
                holder.hole.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffff00")));;
            }else if(item.getType() == 2){
                holder.hole.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4472c4")));;
            }else if(item.getType() == 3){
                holder.hole.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7030a0")));;
            }else if(item.getType() == 4){
                holder.hole.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#c00000")));;
            }else if(item.getType() == 5){
                holder.hole.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#70ad47")));;
            }
            // 기존 색상 처리 ...
            holder.hole.setAlpha(1f); // 기본값 복구
            // 깜빡임 처리
            if (item.isWorking()) {
                // 기존 애니메이터가 있으면 취소
                if (holder.blinkAnimator != null) {
                    holder.blinkAnimator.cancel();
                }
                ObjectAnimator animator = ObjectAnimator.ofFloat(holder.hole, "alpha", 1f, 0.4f, 1f);
                animator.setDuration(1000);
                animator.setRepeatCount(ObjectAnimator.INFINITE);
                animator.setRepeatMode(ObjectAnimator.REVERSE);
                animator.start();
                holder.blinkAnimator = animator;
            } else {
                // 작동 중이 아니라면 애니메이션 중지 및 원상복구
                if (holder.blinkAnimator != null) {
                    holder.blinkAnimator.cancel();
                    holder.blinkAnimator = null;
                }
                holder.hole.setAlpha(1f);
            }
        } else {
            // Payload가 있을 경우, 선택적 처리 가능 (예: Partial Update)
            for (Object payload : payloads) {
                Log.e("payloads", String.valueOf(payload));
                // TODO: payload 타입별로 부분 갱신 처리
            }
        }
    }



    @Override
    public int getItemCount() {
        return data.size();
    }

    public void toggleSelection(int pos) {
        selectPosition = pos;
    }

    // 뷰 바인딩 부분을 한번만 하도록, ViewHolder 패턴 의무화
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView hole;
        ObjectAnimator blinkAnimator; // <== 추가

        public ViewHolder(View view) {
            super(view);

            hole = (ImageView) view.findViewById(R.id.hole);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        // 리스너 객체의 메서드 호출.
                        if (mListener != null) {
                            mListener.onItemClick(v, pos);
                            //toggleSelection(pos);
                        }
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        // 리스너 객체의 메서드 호출.
                        if (mListener != null) {
                            mListener.onItemClick(v, pos);
                        }
                    }
                    return true;
                }
            });
        }
    }
}
