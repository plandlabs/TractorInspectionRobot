package kr.re.kitech.tractorinspectionrobot.views.recyclerView.robot.adapter;

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
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.robot.model.RobotItem;

public class RobotRecyclerViewAdapter extends RecyclerView.Adapter<RobotRecyclerViewAdapter.RobotViewHolder> {
    private Context context;
    private ArrayList<RobotItem> list;
    private LayoutInflater inflater;
    private Vibrator mVibrator;
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
            holder.robot.setBackgroundColor(Color.parseColor("#454545"));
            holder.textDeviceName.setBackgroundColor(Color.parseColor("#222222"));
            holder.textDeviceName.setText(item.getDeviceName() + " (연결중)");
        } else {
            holder.robot.setBackgroundColor(Color.parseColor("#777777"));
            holder.textDeviceName.setBackgroundColor(Color.parseColor("#555555"));
            holder.textDeviceName.setText(item.getDeviceName());
        }

        holder.textHost.setText(item.getMqttConnectUrl());
        holder.textPort.setText(String.valueOf(item.getPort()));
        holder.textUsername.setText(item.getMqttUsername());

        holder.textRootTopic.setText(item.getMqttRootTopic());
        holder.textBaseTopic.setText(item.getMqttBaseTopic());

        String pw = item.getMqttPassword();
        String masked = "";

        if (pw != null) {
            if (pw.length() <= 3) {
                // 3글자 이하라면 그대로 표시
                masked = pw;
            } else {
                int hiddenCount = pw.length() - 3;
                StringBuilder sb = new StringBuilder();
                sb.append(pw.substring(0, 3));          // 앞 3글자
                for (int i = 0; i < hiddenCount; i++) {
                    sb.append("*");                    // 나머지는 *로
                }
                masked = sb.toString();
            }
        }

        holder.textPassword.setText(masked);

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

        View robot;
        TextView textDeviceName, textHost, textPort, textUsername, textPassword, textRootTopic, textBaseTopic;
        Button btnDelete;

        public RobotViewHolder(@NonNull View itemView) {
            super(itemView);
            robot = itemView.findViewById(R.id.robot);
            textDeviceName = itemView.findViewById(R.id.textDeviceName);
            textHost = itemView.findViewById(R.id.textHost);
            textPort = itemView.findViewById(R.id.textPort);
            textUsername = itemView.findViewById(R.id.textUsername);
            textPassword = itemView.findViewById(R.id.textPassword);
            textRootTopic = itemView.findViewById(R.id.textRootTopic);
            textBaseTopic = itemView.findViewById(R.id.textBaseTopic);

            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
