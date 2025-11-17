package kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.adapter;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import kr.re.kitech.tractorinspectionrobot.R;
import kr.re.kitech.tractorinspectionrobot.helper.ItemTouchHelper;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.callback.RobotDiffCallback;
import kr.re.kitech.tractorinspectionrobot.views.recyclerView.scanRobot.model.RobotItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RobotRecyclerView extends RecyclerView.Adapter<RobotRecyclerView.ViewHolder>
        implements ItemTouchHelper {
    private Context context;
    private ArrayList<RobotItem> data;
    private LayoutInflater inflater;
    public int selectPosition = 0;
    private Vibrator mVibrator;

    public RobotRecyclerView(ArrayList<RobotItem> data, Context context) {
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
    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.mListener = listener;
    }

    private static OnItemLongClickListener mLongListener = null;
    public void setOnItemLongClickListener(OnItemLongClickListener listener)
    {
        this.mLongListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_robot_connect, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            RobotItem item = data.get(position);

            // 기본 뷰 설정
            holder.viewItem.setVisibility(View.VISIBLE);
            holder.addViewItem.setVisibility(View.GONE);
            holder.progressCircle.setVisibility(View.GONE);

            String ssid = item.getSsid();
            String mac = item.getMac();
            holder.deviceName.setText(ssid);

            // 초기 뷰 상태
            holder.imgView.setVisibility(View.VISIBLE);
            holder.emptyView.setVisibility(View.GONE);
            holder.deleteBtn.setVisibility(View.GONE);
            holder.isConnected.setVisibility(View.GONE);
            holder.isNotNetworkConnected.setVisibility(View.GONE);
            holder.viewChildItem.setAlpha(1f);

            if (item.getIsSaved()) {
                // 저장된 항목
                holder.imgView.setVisibility(View.VISIBLE);
                holder.emptyView.setVisibility(View.GONE);

                if (item.getIsScan()) {
                    if (item.getIsConnected()) {
                        // 연결 로봇
                        holder.viewChildItem.setAlpha(1f);
                        holder.isConnected.setVisibility(View.VISIBLE);
                        holder.deleteBtn.setVisibility(View.GONE);
                    } else {
                        // 스캔되었지만 연결 안됨
                        holder.viewChildItem.setAlpha(0.95f);
                        holder.isConnected.setVisibility(View.GONE);
                        holder.deleteBtn.setVisibility(View.VISIBLE);
                    }
                } else {
                    // 저장만 되었고 스캔되지 않음
                    holder.viewChildItem.setAlpha(0.25f);
                    holder.deleteBtn.setVisibility(View.VISIBLE);
                }
            } else {
                // 저장 안된 항목
                holder.viewChildItem.setAlpha(0.55f);
                holder.imgView.setVisibility(View.GONE);
                holder.emptyView.setVisibility(View.VISIBLE);
            }
            if (item.getIsConnected()) {
                // 연결 로봇
                holder.isConnected.setVisibility(View.VISIBLE);
            } else {
                // 스캔되었지만 연결 안됨
                holder.isConnected.setVisibility(View.GONE);
            }

            // 소켓 연결되지 않은 상태 표시
            if (!item.getIsNotNetworkConnected()) {
                holder.isNotNetworkConnected.setVisibility(View.VISIBLE);
            } else {
                holder.isNotNetworkConnected.setVisibility(View.GONE);
            }

            // 완전 빈 항목 처리 (SSID, MAC 모두 없음)
            if ((ssid == null || ssid.isEmpty()) && (mac == null || mac.isEmpty())) {
                holder.viewItem.setVisibility(View.GONE);
                holder.addViewItem.setVisibility(View.VISIBLE);
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
        TextView deviceName;

        View imgView, deleteBtn, isNotNetworkConnected, emptyView;
        LinearLayout viewChildItem, addViewItem;
        RelativeLayout viewItem, isConnected;
        ProgressBar progressCircle;


        public ViewHolder(View view) {
            super(view);

            deviceName = (TextView) view.findViewById(R.id.device_name);
            imgView = (View) view.findViewById(R.id.imgView);
            emptyView = (View) view.findViewById(R.id.emptyView);
            deleteBtn = (View) view.findViewById(R.id.deleteBtn);
            isNotNetworkConnected = (View) view.findViewById(R.id.isNotNetworkConnected);
            viewItem = (RelativeLayout) view.findViewById(R.id.viewItem);
            isConnected = (RelativeLayout) view.findViewById(R.id.isConnected);
            viewChildItem = (LinearLayout) view.findViewById(R.id.viewChildItem);
            addViewItem = (LinearLayout) view.findViewById(R.id.addViewItem);
            progressCircle = (ProgressBar) view.findViewById(R.id.progressCircle);
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
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        // 리스너 객체의 메서드 호출.
                        if (mListener != null) {
                            mListener.onItemDeleteClick(v, pos);
                        }
                    }
                }
            });
            addViewItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        // 리스너 객체의 메서드 호출.
                        if (mListener != null) {
                            mListener.onItemAddClick(v, pos);
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

    public void updateList(List<RobotItem> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new RobotDiffCallback(this.data, newList)
        );
        this.data.clear();
        this.data.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }
}
