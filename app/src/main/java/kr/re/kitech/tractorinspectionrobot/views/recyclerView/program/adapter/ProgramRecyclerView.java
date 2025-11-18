package kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.helper.ItemTouchHelper;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgramRecyclerView extends RecyclerView.Adapter<ProgramRecyclerView.ViewHolder>
        implements ItemTouchHelper {
    private Context context;
    private ArrayList<RobotState> data;
    private LayoutInflater inflater;
    public int selectPosition = 0;
    private Vibrator mVibrator;
    private static OnItemClickListener mListener = null;
    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.mListener = listener;
    }
    private static OnItemLongClickListener mLongListener = null;
    public void setOnItemLongClickListener(OnItemLongClickListener listener)
    {
        this.mLongListener = listener;
    }

    public ProgramRecyclerView(ArrayList<RobotState> data, Context context) {
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_program, parent, false);

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
            RobotState item = data.get(position);
            // 기본 뷰 설정
            holder.num.setText(String.valueOf(item.getNum()));
            holder.x.setText(String.valueOf(item.getX()));
            holder.y.setText(String.valueOf(item.getY()));
            holder.z.setText(String.valueOf(item.getZ()));
            holder.s1.setText(String.valueOf(item.getS1()));
            holder.s2.setText(String.valueOf(item.getS2()));
            holder.s3.setText(String.valueOf(item.getS3()));
            if(item.getNum() % 2 == 1) {
                holder.background.setBackgroundResource(R.color.rowBackground);
            }else{
                holder.background.setBackgroundResource(android.R.color.transparent);
            }
            holder.itemView.setOnClickListener(v -> {
                if (position != RecyclerView.NO_POSITION && mListener != null) {
                    mListener.onItemClick(v, position);
                }
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (position != RecyclerView.NO_POSITION && mLongListener != null) {
                    mLongListener.onItemLongClick(v, position);
                    return true;
                }
                return false;
            });
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
        TextView num, x, y, z, s1, s2, s3;
        TableRow background;

        public ViewHolder(View view) {
            super(view);

            num = (TextView) view.findViewById(R.id.num);
            x = (TextView) view.findViewById(R.id.x);
            y = (TextView) view.findViewById(R.id.y);
            z = (TextView) view.findViewById(R.id.z);
            s1 = (TextView) view.findViewById(R.id.s1);
            s2 = (TextView) view.findViewById(R.id.s2);
            s3 = (TextView) view.findViewById(R.id.s3);
            background = (TableRow) view.findViewById(R.id.background);
        }
    }
}
