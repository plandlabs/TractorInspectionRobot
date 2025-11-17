package kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.model.RobotItem;

public class RobotRecyclerViewAdapter extends RecyclerView.Adapter<RobotRecyclerViewAdapter.RobotViewHolder> {
    private Context context;
    private ArrayList<RobotItem> list;
    private LayoutInflater inflater;
    private Vibrator mVibrator;

    // 굳이 static일 필요는 없지만, 기존 코드 유지
    private static OnItemClickListener mListener = null;
    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }
    private static OnItemLongClickListener mLongListener = null;
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mLongListener = listener;
    }

    public RobotRecyclerViewAdapter(ArrayList<RobotItem> list, Context context) {
        this.list = list;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public RobotItem getItem(int position) {
        return list.get(position);
    }

    @NonNull
    @Override
    public RobotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_robot, parent, false);
        return new RobotViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RobotViewHolder holder, @SuppressLint("RecyclerView") int position) {
        RobotItem item = list.get(position);

        // ✅ 연결 상태에 따라 이름/색상 다르게 표시
        if (item.isConnected()) {
            holder.textDeviceName.setText(item.getDeviceName() + " (연결중)");
            holder.textDeviceName.setTextColor(Color.parseColor("#4CAF50")); // 초록색
        } else {
            holder.textDeviceName.setText(item.getDeviceName());
            holder.textDeviceName.setTextColor(Color.WHITE);
        }

        holder.textBroker.setText(item.getMqttConnectUrl() + ":" + item.getPort());
        holder.textTopic.setText(item.getMqttRootTopic() + "/" + item.getMqttBaseTopic());

        holder.itemView.setOnClickListener(v -> {
            if (position != RecyclerView.NO_POSITION && mListener != null) {
                mListener.onItemClick(v, position);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (position != RecyclerView.NO_POSITION && mListener != null) {
                mListener.onItemDeleteClick(v, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (position != RecyclerView.NO_POSITION && mLongListener != null) {
                mLongListener.onItemLongClick(v, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class RobotViewHolder extends RecyclerView.ViewHolder {

        TextView textDeviceName;
        TextView textBroker;
        TextView textTopic;
        Button btnDelete;

        public RobotViewHolder(@NonNull View itemView) {
            super(itemView);
            textDeviceName = itemView.findViewById(R.id.textDeviceName);
            textBroker = itemView.findViewById(R.id.textBroker);
            textTopic = itemView.findViewById(R.id.textTopic);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
