package ru.olegartys.simplechat;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import java.util.ArrayList;

/**
 * Created by olegartys on 19.03.15.
 */
public class MessageListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<ListItem> messagesItems;

    public MessageListAdapter(Context context, ArrayList<ListItem> messagesItems) {
        this.context = context;
        this.messagesItems = messagesItems;
    }

    @Override
    public int getCount() {
        return messagesItems.size();
    }

    @Override
    public Object getItem(int position) {
        return messagesItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        ListItem it = messagesItems.get(position);
        if (it.isSelf()) {
            convertView = inflater.inflate(R.layout.right_item_message, null);
        } else
            convertView = inflater.inflate(R.layout.left_item_message, null);

        TextView tv = (TextView)convertView.findViewById(R.id.lblMsgFrom);
        TextView tv1 = (TextView)convertView.findViewById(R.id.txtMsg);

        tv.setText(it.getLogin());
        tv1.setText(it.getMsg());

        return convertView;
    }
}
