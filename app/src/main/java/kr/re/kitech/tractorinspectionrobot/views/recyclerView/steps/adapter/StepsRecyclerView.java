package kr.re.kitech.tractorinspectionrobot.views.recyclerView.steps.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.helper.ItemTouchHelper;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.steps.model.StepsItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StepsRecyclerView extends RecyclerView.Adapter<StepsRecyclerView.ViewHolder>
        implements ItemTouchHelper {
    private Context context;
    private ArrayList<StepsItem> data;
    private LayoutInflater inflater;
    public int selectPosition = 0;
    private Vibrator mVibrator;

    public StepsRecyclerView(ArrayList<StepsItem> data, Context context) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_step, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            StepsItem item = data.get(position);
            // 기본 뷰 설정
            holder.stepNum.setText("STEP " + item.getStepNum());
            holder.subject.setText(item.getSubject());
            if(item.isAngleBtn()){
                holder.btnAngle.setVisibility(View.VISIBLE);
            }else{
                holder.btnAngle.setVisibility(View.GONE);
            }
            holder.stepBg.setAlpha(1);
            holder.stepNum.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(context, R.color.background_color))));
            holder.subject.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(context, R.color.background_color))));
            if(item.isStepWorkingBool()) {
                if (item.isStepValueBool()) {
                    holder.stepBg.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.step_working)));
                } else {
                    holder.stepBg.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.step_ready)));
                    holder.stepBg.setAlpha(.35F);
                }
            }else{
                if (item.isStepValueBool()) {
                    holder.stepBg.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.step_select)));
                    holder.stepNum.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(context, R.color.feedValueMax))));
                    holder.subject.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(context, R.color.feedValueMax))));
                } else {
                    holder.stepBg.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.step_ready)));
                }
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
        TextView stepNum, subject, btnAngle;
        View stepBg;

        public ViewHolder(View view) {
            super(view);

            stepBg = view.findViewById(R.id.step_bg);
            stepNum = (TextView) view.findViewById(R.id.step_num);
            subject = (TextView) view.findViewById(R.id.subject);
            btnAngle = (TextView) view.findViewById(R.id.btn_angle);

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
            btnAngle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        // 리스너 객체의 메서드 호출.
                        if (mListener != null) {
                            mListener.onAngleBtnClick(v, pos);
                        }
                    }
                }
            });

        }
    }
}
