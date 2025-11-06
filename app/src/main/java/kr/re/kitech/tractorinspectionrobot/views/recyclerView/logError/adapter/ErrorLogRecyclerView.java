package kr.re.kitech.tractorinspectionrobot.views.recyclerView.logError.adapter;

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
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.logError.model.ErrorLogItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ErrorLogRecyclerView extends RecyclerView.Adapter<ErrorLogRecyclerView.ViewHolder>
        implements ItemTouchHelper {
    private Context context;
    private ArrayList<ErrorLogItem> data;
    private LayoutInflater inflater;
    public int selectPosition = 0;
    private Vibrator mVibrator;

    public ErrorLogRecyclerView(ArrayList<ErrorLogItem> data, Context context) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log_error, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            ErrorLogItem item = data.get(position);

            // 기본 뷰 설정
            holder.num.setText(String.valueOf(item.getNum()));
            holder.datetime.setText(item.getDatetime());
            holder.addr.setText(String.valueOf(item.getAddr()));
            holder.msg.setText(item.getMessage());
            if(item.getNum() % 2 == 1) {
                holder.background.setBackgroundResource(R.color.rowBackground);
            }else{
                holder.background.setBackgroundResource(android.R.color.transparent);
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
        TextView num, datetime, addr, msg;
        TableRow background;

        public ViewHolder(View view) {
            super(view);

            num = (TextView) view.findViewById(R.id.num);
            datetime = (TextView) view.findViewById(R.id.datetime);
            addr = (TextView) view.findViewById(R.id.addr);
            msg = (TextView) view.findViewById(R.id.msg);
            background = (TableRow) view.findViewById(R.id.background);
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
