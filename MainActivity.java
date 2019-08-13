package com.sample.notificationcenter;

import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        MessageAdapter.OnItemClickListener, ADBReceiver.BoardcastListener {

    private SlideRecyclerView recyclerView;//

    private List<MessageBean> list;//数据源
    private MessageAdapter adapter;

    private int position;

    private Button refresh;
    private View visibilityLayout;
    private TextView newsTitleText;
    private TextView newsContentText;
    private Button leftButton;
    private Button rightButton;

    private TextView noMessage;
    private TextView tvUnread;
    private Button all;
    private Button read;
    private Button delete;
    private Button edit;

    //点击了全选
    private boolean checkAll;
    //编辑模式
    private boolean editMode;
    //对应全选按钮
    private boolean clicked;
    //区分是不是删除确认界面
    private boolean confirm;
    //区分是不是侧滑删除
    private boolean deleteItem;

    private List<Integer> selectedPosition = new ArrayList<>();//选中的位置集合

    private int unread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        updateUnRead();
    }

    private void initView() {
        noMessage = findViewById(R.id.no_message);
        visibilityLayout = findViewById(R.id.visibility_layout);
        newsTitleText = findViewById(R.id.news_title);
        newsContentText = findViewById(R.id.news_content);
        leftButton = findViewById(R.id.left_button);
        rightButton = findViewById(R.id.right_button);

        tvUnread = findViewById(R.id.unread);
        all = findViewById(R.id.selectall_button);
        read = findViewById(R.id.readed_button);
        delete = findViewById(R.id.delete_main_button);
        edit = findViewById(R.id.edit_button);

        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);
        all.setOnClickListener(this);
        read.setOnClickListener(this);
        delete.setOnClickListener(this);
        edit.setOnClickListener(this);

        refresh = findViewById(R.id.refresh_button);
        refresh.setOnClickListener(this);
        recyclerView = findViewById(R.id.message_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //增加或减少条目动画效果，不要就注掉
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        if (MessageDAO.getNews(this) == null) {
            list = new ArrayList<>();
        } else {
            list = MessageDAO.getNews(this);
        }
        adapter = new MessageAdapter(this, list);
        adapter.setOnItemClickListener(this);
        receiver.setBoardcastListensr(this);
        recyclerView.setAdapter(adapter);
        if (MessageDAO.getNews(this).size() == 0) {
            edit.setEnabled(false);
            setNoMessage();
        } else {
            edit.setEnabled(true);
            setMessage();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.refresh_button:
                setData();
                updateUnRead();
                break;
            case R.id.left_button:
            case R.id.right_button:
                buttonConfirm(id);
                break;
            case R.id.selectall_button:
                selectAll();
                break;
            case R.id.readed_button:
                messageRead();
                break;
            case R.id.delete_main_button:
                deleteMessage(false);
                break;
            case R.id.edit_button:
                editMessage();
                break;
        }
    }

    /**
     * 列表item点击事件处理
     *
     * @param position
     */
    @Override
    public void onItemClick(int position) {
        checkAll = false;
        all.setText("全选");
        clicked = false;
        this.position = position;
        MessageBean bean = list.get(position);
        if (!editMode) {
            //非编辑模式点击item
            refresh(bean.getTitle(), bean.getMessage(), bean.getType());
            bean.setRead(true);
            bean.setFlag(1);
            adapter.notifyItemChanged(position, bean.isRead());
        } else {
            //编辑模式下点击item
            boolean isChecked = bean.isChecked();
            isChecked = !isChecked;
            bean.setChecked(isChecked);
            adapter.notifyItemChanged(position, bean.isChecked());
        }
        updateUnRead();
    }

    //设置无消息时的页面布局
    public void setNoMessage() {
        noMessage.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
    }

    //设置有消息时的页面布局
    public void setMessage() {
        noMessage.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    //更改未读消息数量
    private void updateUnRead() {
        unread = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getFlag() == 0) {
                unread++;
            }
        }
        if (unread == 0) {
            tvUnread.setVisibility(View.GONE);
        } else {
            tvUnread.setVisibility(View.VISIBLE);
            tvUnread.setText(String.valueOf(unread));
        }
    }

    /**
     * 列表中checkbox选中状态变化的处理
     *
     * @param position
     */
    @Override
    public void onChecked(int position) {
        this.position = position;
        MessageBean bean = list.get(position);
        setCheckData(bean.isChecked());
    }

    @Override
    public void onDelete(final int position) {
        deleteMessage(true);
    }

    /**
     * 右边界面的左右两个按钮的点击事件处理
     *
     * @param id
     */
    private void buttonConfirm(int id) {
        if (!confirm) {
            //不是删除页面，只需要显示信息内容
            int type = list.get(position).getType();
            showToast(id, type);
        } else {
            //需要显示删除页面
            confirm = false;
            if (id == R.id.left_button) {

                if (!deleteItem) {
                    //确认删除消息
                    List<MessageBean> removeList = new ArrayList<>();
                    for (int i = 0; i < selectedPosition.size(); i++) {
                        int position = selectedPosition.get(i);
                        MessageBean bean = list.get(position);
                        removeList.add(bean);
                    }
                    //删除选中item对应的数据
                    list.removeAll(removeList);
                    //清空已选中item的position数据
                    selectedPosition.clear();
                    updateUnRead();
                } else {
                    list.remove(position);
                    visibilityLayout.setVisibility(View.INVISIBLE);
                    updateUnRead();
                    adapter.notifyItemRemoved(position);

                    if (list.size() == 0) {
                        showEdit(View.INVISIBLE);
                        edit.setText("编辑");
                        edit.setEnabled(false);
                        editMode = false;
                        refresh.setEnabled(true);
                        adapter.editMode = false;
                        setNoMessage();
                    }
                    return;
                }
            } else if (id == R.id.right_button) {
                visibilityLayout.setVisibility(View.INVISIBLE);
                recyclerView.closeMenu();
                return;
            }

            //删除后如果没数据了 隐藏功能键 且编辑按钮不可点击
            if (list.size() == 0) {
                showEdit(View.INVISIBLE);
                edit.setText("编辑");
                edit.setEnabled(false);
                editMode = false;
                refresh.setEnabled(true);
                adapter.editMode = false;
                updateUnRead();
                setNoMessage();
            }

            visibilityLayout.setVisibility(View.INVISIBLE);
            //删除后选中项为0，"已读"、"删除"按钮置灰
            read.setEnabled(false);
            delete.setEnabled(false);

            all.setText("全选");
            checkAll = false;

            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 设为已读按钮
     */
    private void messageRead() {
        int index = 0;
        //设置选中item的已读状态
        for (int i = 0; i < selectedPosition.size(); i++) {
            MessageBean bean = list.get(selectedPosition.get(i));
            if (bean.getFlag() == 0) {
                index++;
            }
            bean.setRead(true);
            bean.setFlag(1);
        }
        adapter.notifyDataSetChanged();
        updateUnRead();
        Toast.makeText(this, "完成" + index + "条信息已读！", Toast.LENGTH_SHORT).show();
    }

    /**
     * 删除
     *
     * @param deleteItem true ? 侧滑删除 ： 选择删除
     */
    private void deleteMessage(boolean deleteItem) {
        confirm = true;//进入删除模式
        this.deleteItem = deleteItem;
        visibilityLayout.setVisibility(View.VISIBLE);
        newsTitleText.setVisibility(View.INVISIBLE);
        newsContentText.setText("确定删除所选（有）信息吗");
        rightButton.setVisibility(View.VISIBLE);
        leftButton.setVisibility(View.VISIBLE);
        leftButton.setText("确定删除");
        rightButton.setText("取消");
    }

    /**
     * 编辑按钮
     */
    private void editMessage() {
        editMode = !editMode;
        if (editMode) {
            //编辑状态
            showEdit(View.VISIBLE);
            refresh.setEnabled(false);
            edit.setText("取消");
            visibilityLayout.setVisibility(View.INVISIBLE);
        } else {
            //取消编辑
            showEdit(View.INVISIBLE);
            refresh.setEnabled(true);
            edit.setText("编辑");
            visibilityLayout.setVisibility(View.VISIBLE);

            all.setText("全选");
            clicked = false;
            checkAll = false;
            for (MessageBean bean : list) {
                bean.setChecked(false);
            }
            visibilityLayout.setVisibility(View.INVISIBLE);
        }
        adapter.editMode = editMode;
        adapter.notifyDataSetChanged();
    }

    /**
     * 全选按钮
     */
    private void selectAll() {
        clicked = !clicked;
        visibilityLayout.setVisibility(View.INVISIBLE);
        if (clicked) {
            //全选
            all.setText("取消全选");
            checkAll = true;

            if (!read.isEnabled()) {
                read.setEnabled(true);
                delete.setEnabled(true);
            }

            for (int i = 0; i < list.size(); i++) {
                MessageBean bean = list.get(i);
                bean.setChecked(true);
                selectedPosition.add(i);
            }

        } else {
            selectedPosition.clear();

            read.setEnabled(false);
            delete.setEnabled(false);
            //反选
            all.setText("全选");
            checkAll = false;

            for (MessageBean bean : list) {
                bean.setChecked(false);
            }
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * 设置列表中的选中项
     *
     * @param checked
     */
    private void setCheckData(boolean checked) {
        if (checkAll) {
            return;
        }

        all.setText("全选");
        clicked = false;

        if (checked) {
            //checkbox为选中状态
            selectedPosition.add(position);
            if (!read.isEnabled()) {
                read.setEnabled(true);
                delete.setEnabled(true);
            }
        } else {
            int size = selectedPosition.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    if (selectedPosition.get(i) == position) {
                        selectedPosition.remove(i);
                        break;
                    }
                }
            }

            if (selectedPosition.size() == 0) {
                read.setEnabled(false);
                delete.setEnabled(false);
            }
        }

        if (selectedPosition.size() == 0) {
            all.setText("全选");
            clicked = false;
        } else if (selectedPosition.size() == list.size()) {
            all.setText("取消全选");
            clicked = true;
        }
    }


    /**
     * 点击编辑后显示各个功能按钮
     *
     * @param visibility
     */
    private void showEdit(int visibility) {
        all.setVisibility(visibility);
        read.setVisibility(visibility);
        delete.setVisibility(visibility);
        read.setEnabled(false);
        delete.setEnabled(false);
    }

    /**
     * 右边按钮点击后对应的Toast
     *
     * @param id
     * @param type
     */
    private void showToast(int id, int type) {
        String text = "";
        if (id == R.id.left_button) {
            switch (type) {
                case 5:
                    text = "导航成功";
                    break;
                case 6:
                    text = "成功车检";
                    break;
                case 3:
                    text = "保养成功";
                    break;
                case 4:
                    text = "查看详情";
                    break;
            }
        } else if (id == R.id.right_button) {
            if (type == 6) {
                text = "查看详情";
            }
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * 根据不同type来设置右边界面的显示
     *
     * @param newsTitle
     * @param newsContent
     * @param type
     */
    private void refresh(String newsTitle, String newsContent, int type) {
        visibilityLayout.setVisibility(View.VISIBLE);

        newsTitleText.setText(newsTitle);//刷新新闻标题
        newsContentText.setText(newsContent);//刷新新闻内容
        switch (type) {
            case 2:
                rightButton.setVisibility(View.INVISIBLE);
                leftButton.setVisibility(View.INVISIBLE);
                break;
            case 5:
                leftButton.setText("导航");
                leftButton.setVisibility(View.VISIBLE);
                rightButton.setVisibility(View.INVISIBLE);
                break;
            case 6:
                leftButton.setText("我已车检");
                rightButton.setText("查看详情");
                leftButton.setVisibility(View.VISIBLE);
                rightButton.setVisibility(View.VISIBLE);
                break;
            case 3:
                leftButton.setText("前去保养");
                leftButton.setVisibility(View.VISIBLE);
                rightButton.setVisibility(View.INVISIBLE);
                break;
            case 4:
                leftButton.setText("查看详情");
                leftButton.setVisibility(View.VISIBLE);
                rightButton.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * 手动添加数据
     */
    private void setData() {
        list.add(0, MessageDAO.getMessage());
        if (adapter != null) {
            if (list.size() == 1) {
                edit.setEnabled(true);
                setMessage();
            }
//            int lastPosition = list.size() - 1;
            //刷新列表数据
            recyclerView.closeMenu();
            adapter.notifyItemInserted(0);
            /**
             * 如果不需要动画效果
             * 就删掉 adapter.notifyItemInserted(lastPosition);
             * 用 adapter.notifyDataSetChanged();
             */
            recyclerView.scrollToPosition(0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (MessageDAO.getNews(this) != null) {
                for (MessageBean messageBean : MessageDAO.getNews(this)) {
                    MessageDAO.delete(this, messageBean);
                }
            }
            if (list != null) {
                for (MessageBean messageBean : list) {
                    MessageDAO.saveMessage(this, messageBean);
                }
            }
            list.clear();
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private ADBReceiver receiver = new ADBReceiver();

    @Override
    public void insertMessage() {
        setData();
    }
}
