package com.example.rfid_test;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LayList extends BaseAdapter {

    private List<RfidView> mData;
    private Context context;
    private int currentItem = -1;//listview中显示位置，取默认值为-1。

    public LayList(List<RfidView> mData, Context context) {
        this.mData = mData;
        this.context = context;
    }

    public void clear() {
        if (mData != null){
            mData.clear();
        }
        notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }



    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.rfidlist,null);
            holder = new ViewHolder();
            holder.listItemID = (TextView)convertView.findViewById(R.id.rfidTagIDg);
            holder.listnumber = (TextView) convertView.findViewById(R.id.rfidTagIDnumberg);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder)convertView.getTag();
        }

        //赋值
        holder.listItemID.setText(mData.get(position).getName());
        holder.listnumber.setText(String.valueOf(mData.get(position).getNumber()));
        notifyDataSetChanged();
        return convertView;
    }

    public void additem(String data){
        if (mData == null) {
            mData = new LinkedList<>();
        }else {
            Boolean td = false;
            for (RfidView item:mData) {
                if (item.getName().equals(data)){
                    item.setNumber(item.getNumber()+1);
                    notifyDataSetChanged();
                    td = true;
                    break;
                }
            }
            if (td == false) mData.add(new RfidView(data,1));
            notifyDataSetChanged();
        }

    }

    void resetData(List<RfidView> data){
        mData.clear();
        loadMoreData9ArrayList(data);
    }

    void loadMoreData9ArrayList (List<RfidView> data){
        mData.addAll(data);
        notifyDataSetChanged();
    }

    public static List<RfidView> removeDuplicate(List<RfidView> list)
    {
        Set set = new LinkedHashSet<RfidView>();
        set.addAll(list);
        list.clear();
        list.addAll(set);
        return list;
    }

    public void alertitem(RfidView data){
        for (RfidView item : mData) {
            if (!(item.getName().equals(data.getName()))){
                //int index = item.getNumber();
               // item.setNumber(index++);

                notifyDataSetChanged();
            }
        }
        mData.add(data);
        HashSet<RfidView> hs = new HashSet<RfidView>(mData);
    }


    public class ViewHolder{
        TextView listItemID;
        TextView listnumber;
    }
}
