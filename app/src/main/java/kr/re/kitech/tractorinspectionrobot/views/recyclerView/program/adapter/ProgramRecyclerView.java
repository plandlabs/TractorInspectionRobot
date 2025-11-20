package kr.re.kitech.tractorinspectionrobot.views.recyclerView.program.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.helper.ItemTouchHelper;
import kr.re.kitech.tractorinspectionrobot.mqtt.shared.item.RobotState;

public class ProgramRecyclerView extends RecyclerView.Adapter<ProgramRecyclerView.ViewHolder>
        implements ItemTouchHelper {

    private final Context context;
    private final ArrayList<RobotState> data;
    private final LayoutInflater inflater;
    public int selectPosition = 0;
    private final Vibrator mVibrator;

    private static OnItemClickListener mListener = null;
    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    private static OnItemLongClickListener mLongListener = null;
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mLongListener = listener;
    }

    public ProgramRecyclerView(ArrayList<RobotState> data, Context context) {
        this.data = data;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    // 드래그 이동
    @Override
    public void onItemMove(int fromPos, int targetPos) {
        if (mVibrator != null) mVibrator.vibrate(10);
        if (fromPos < targetPos) {
            for (int i = fromPos; i < targetPos; i++) {
                Collections.swap(data, i, i + 1);
            }
        } else {
            for (int i = fromPos; i > targetPos; i--) {
                Collections.swap(data, i, i - 1);
            }
        }
        notifyItemMoved(fromPos, targetPos);
    }

    @Override
    public void onItemDismiss(int pos) {
        // 스와이프 삭제 안 쓰면 비워둠
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_program, parent, false);
        return new ViewHolder(view);
    }

    // ⚠ 기본 onBindViewHolder가 비어 있으면 안 됨 → payload 버전으로 위임
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        onBindViewHolder(holder, position, new ArrayList<>()); // payloads 비어있는 걸로 전달
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position,
                                 @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            RobotState item = data.get(position);

            // 기본 데이터 바인딩
            holder.num.setText(String.valueOf(item.getNum()));
            holder.x.setText(String.valueOf(item.getX()));
            holder.y.setText(String.valueOf(item.getY()));
            holder.z.setText(String.valueOf(item.getZ()));
            holder.s1.setText(String.valueOf(item.getS1()));
            holder.s2.setText(String.valueOf(item.getS2()));
            holder.s3.setText(String.valueOf(item.getS3()));

            // 홀짝 줄 배경
            if (item.getNum() % 2 == 1) {
                holder.background.setBackgroundResource(R.color.rowBackground);
            } else {
                holder.background.setBackgroundResource(android.R.color.transparent);
            }

            // move 상태 표시
            int move = item.getMove();
            if (move == 1) {
                holder.move.setText("대기");
                holder.move.setTextColor(
                        ContextCompat.getColor(context, R.color.menu_color));
            } else if (move == 2) {
                holder.move.setText("이동중");
                holder.move.setTextColor(
                        ContextCompat.getColor(context, R.color.feedValueMax));
            } else if (move == 3) {
                holder.move.setText("실행중");
                holder.move.setTextColor(
                        ContextCompat.getColor(context, R.color.houseBoxBackground));
            } else if (move == 4) {
                holder.move.setText("완료");
                holder.move.setTextColor(
                        ContextCompat.getColor(context, R.color.feedValueMin));
            } else {
                holder.move.setText("");
                // 필요하면 기본 색 지정
                // holder.move.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            }

            // 클릭 리스너
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && mListener != null) {
                    mListener.onItemClick(v, pos);
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && mListener != null) {
                    mListener.onItemDeleteClick(v, pos);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && mLongListener != null) {
                    mLongListener.onItemLongClick(v, pos);
                    return true;
                }
                return false;
            });

        } else {
            // payload로 부분 갱신하고 싶으면 여기에서 처리
            // 지금은 전체 바인딩과 큰 차이 없으니 생략
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void toggleSelection(int pos) {
        selectPosition = pos;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView num, move, x, y, z, s1, s2, s3;
        TableRow background;
        LinearLayout btnDelete;

        public ViewHolder(View view) {
            super(view);
            num = view.findViewById(R.id.num);
            move = view.findViewById(R.id.move);
            x = view.findViewById(R.id.x);
            y = view.findViewById(R.id.y);
            z = view.findViewById(R.id.z);
            s1 = view.findViewById(R.id.s1);
            s2 = view.findViewById(R.id.s2);
            s3 = view.findViewById(R.id.s3);
            background = view.findViewById(R.id.background);
            btnDelete = view.findViewById(R.id.btnDelete);
        }
    }
}
