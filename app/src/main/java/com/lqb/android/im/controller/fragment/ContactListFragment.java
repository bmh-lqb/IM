package com.lqb.android.im.controller.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.ui.EaseContactListFragment;
import com.hyphenate.exceptions.HyphenateException;
import com.lqb.android.im.R;
import com.lqb.android.im.controller.activity.AddContactActivity;
import com.lqb.android.im.controller.activity.ChatActivity;
import com.lqb.android.im.controller.activity.GroupListActivity;
import com.lqb.android.im.controller.activity.InviteActivity;
import com.lqb.android.im.model.Model;
import com.lqb.android.im.model.bean.UserInfo;
import com.lqb.android.im.utils.Constant;
import com.lqb.android.im.utils.SpUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactListFragment extends EaseContactListFragment {
    private ImageView iv_contact_red;
    private LinearLayout ll_contact_invite;
    private LocalBroadcastManager mLBM;
    private String mHxid;

    private BroadcastReceiver ContactInviteChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 更新红点显示
            iv_contact_red.setVisibility(View.VISIBLE);
            SpUtils.getInstance().save(SpUtils.IS_NEW_INVITE, true);
        }
    };

    private BroadcastReceiver ContactChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 刷新页面
            refreshContact();
        }
    };

    private BroadcastReceiver GroupChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 显示红点
            iv_contact_red.setVisibility(View.VISIBLE);
            SpUtils.getInstance().save(SpUtils.IS_NEW_INVITE, true);
        }
    };

    @Override
    protected void initView() {
        super.initView();

        // 布局显示加号
        titleBar.setRightImageResource(R.drawable.em_add);

        // 添加头布局
        View headerView = View.inflate(getActivity(), R.layout.header_fragment_contact, null);
        listView.addHeaderView(headerView);

        // 获取红点对象
        iv_contact_red = (ImageView) headerView.findViewById(R.id.iv_contact_red);

        // 获取邀请信息条目的对象
        ll_contact_invite = (LinearLayout) headerView.findViewById(R.id.ll_contact_invite);

        setContactListItemClickListener(new EaseContactListItemClickListener() {
            @Override
            public void onListItemClicked(EaseUser user) {
                if (user == null) {
                    return;
                }

                Intent intent = new Intent(getActivity(), ChatActivity.class);

                // 传递参数
                intent.putExtra(EaseConstant.EXTRA_USER_ID, user.getUsername());
                startActivity(intent);
            }
        });

        // 跳转到群组列表页面
        LinearLayout ll_contact_group = (LinearLayout) headerView.findViewById(R.id.ll_contact_group);
        ll_contact_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), GroupListActivity.class);

                startActivity(intent);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mLBM.unregisterReceiver(ContactInviteChangeReceiver);
        mLBM.unregisterReceiver(ContactChangeReceiver);
        mLBM.unregisterReceiver(GroupChangeReceiver);
    }

    @Override
    protected void setUpView() {
        super.setUpView();

        // 添加按钮的点击事件处理
        titleBar.setRightLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddContactActivity.class);
                startActivity(intent);
            }
        });

        // 初始化红点显示
        boolean isNewInvite = SpUtils.getInstance().getBoolean(SpUtils.IS_NEW_INVITE,false);
        iv_contact_red.setVisibility(isNewInvite ? View.VISIBLE : View.GONE);


        // 邀请信息条目的点击事件
        ll_contact_invite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 红点处理
                iv_contact_red.setVisibility(View.GONE);
                SpUtils.getInstance().save(SpUtils.IS_NEW_INVITE, false);

                // 跳转到邀请信息列表页面
                Intent intent = new Intent(getActivity(), InviteActivity.class);
                startActivity(intent);
            }
        });

        // 从环信服务器获取所有的联系人信息
        getContactsFromHxServer();

        // 绑定listView和contextmenu
        registerForContextMenu(listView);

        // 注册广播
        mLBM = LocalBroadcastManager.getInstance(getActivity());
        mLBM.registerReceiver(ContactInviteChangeReceiver, new IntentFilter(Constant.CONTACT_INVITE_CHANGED));
        mLBM.registerReceiver(ContactChangeReceiver, new IntentFilter(Constant.CONTACT_CHANGED));
        mLBM.registerReceiver(GroupChangeReceiver, new IntentFilter(Constant.GROUP_INVITE_CHANGED));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // 获取环信id
        int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;

        EaseUser easeUser = (EaseUser) listView.getItemAtPosition(position);

        mHxid = easeUser.getUsername();

        // 添加布局
        getActivity().getMenuInflater().inflate(R.menu.delete, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.contact_delete) {
            new  AlertDialog.Builder(getActivity())
                    .setTitle("确认" )
                    .setMessage("确定删除该联系人吗？" )
                    .setPositiveButton("是" ,  new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 执行删除选中的联系人操作
                            deleteContact();
                        }
                    } )
                    .setNegativeButton("否" , null)
                    .show();
        }

        return super.onContextItemSelected(item);
    }

    // 执行删除选中的联系人操作
    private void deleteContact() {
        Model.getInstance().getGlobalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 环信服务器删除联系人
                    EMClient.getInstance().contactManager().deleteContact(mHxid);

                    // 本地数据库的更新
                    Model.getInstance().getDbManager().getContactTableDao().deleteContactByHxId(mHxid);

                    if (getActivity() == null) {
                        return;
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 提示
                            Toast.makeText(getActivity(), "删除" + mHxid + "成功", Toast.LENGTH_SHORT).show();

                            // 刷新页面
                            refreshContact();
                        }
                    });
                } catch (HyphenateException e) {
                    e.printStackTrace();

                    if (getActivity() == null) {
                        return;
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "删除" + mHxid + "失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    // 从环信服务器获取所有联系人信息
    private void getContactsFromHxServer() {
        Model.getInstance().getGlobalThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                List<String> hxids = null;

                try {
                    // 获取所有好友的环信id
                    hxids = EMClient.getInstance().contactManager().getAllContactsFromServer();

                    // 校验
                    if (hxids != null && hxids.size() >= 0) {
                        List<UserInfo> contacts = new ArrayList<UserInfo>();

                        UserInfo userInfo = null;

                        // 转换
                        for (String hxid : hxids) {
                            userInfo = new UserInfo(hxid);
                            contacts.add(userInfo);
                        }

                        // 保存好友信息到本地数据库
                        Model.getInstance().getDbManager().getContactTableDao().saveContacts(contacts, true);

                        // 防止页面切换的时候，刚好getActivity为空，导致下面调用报空指针
                        if (getActivity() == null) {
                            return;
                        }

                        // 刷新页面
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 刷新页面的方法
                                refreshContact();
                            }
                        });
                    }
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 刷新页面
    private void refreshContact() {

        // 获取数据
        List<UserInfo> contacts = Model.getInstance().getDbManager().getContactTableDao().getContacts();

        // 校验
        if (contacts != null && contacts.size() >= 0) {

            // 设置数据，环信已经给我们做了刷新方法
            Map<String, EaseUser> contactsMap = new HashMap<>();

            EaseUser easeUser = null;

            // 转换
            for (UserInfo contact : contacts) {
                easeUser = new EaseUser(contact.getHxId());

                contactsMap.put(contact.getHxId(), easeUser);
            }

            setContactsMap(contactsMap);

            // 刷新页面
            refresh();
        }
    }
}
