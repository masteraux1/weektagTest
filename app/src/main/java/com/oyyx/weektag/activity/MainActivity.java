package com.oyyx.weektag.activity;


import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Explode;
import android.transition.Slide;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.oyyx.weektag.R;
import com.oyyx.weektag.adapter.HistoryAdapter;
import com.oyyx.weektag.callback.DialogCallBack;
import com.oyyx.weektag.dateBase.TransactionLab;
import com.oyyx.weektag.dateBase.Transactionn;
import com.oyyx.weektag.utils.CalendarUtils;

import org.litepal.LitePal;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, DialogCallBack {

    //打开本人的qq临时会话uri
    private final String qqUrl = "mqqwpa://im/chat?chat_type=wpa&uin=768471488&version=1";


    private static final int SELECT_FROM_ALBUM = 0;
    private static final int TAKE_PHOTOS = 1;
    private static final int USER_DATA = 2;
    private final static int SORT_DEFAULT = 3;
    private final static int SORT_BY_TIME = 4;

    private static int sortFlag = SORT_DEFAULT;

    private RecyclerView mRecyclerView;
    private HistoryAdapter mHistoryAdapter;
    private FrameLayout emptyView;

    private TextView username;
    private TextView emailaddress;
    private CircleImageView userImage;

    private List<Transactionn> transactionns;

    private String uri;

    //存储用户名及其邮箱
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        LitePal.initialize(this);

        startService(new Intent("android.appwidget.action.WIDGET_SERVICE").setPackage("com.oyyx.weektag"));


        transactionns = TransactionLab.get().getTransactionnsByDefault();


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //侧边栏头部布局初始化
        View view = navigationView.inflateHeaderView(R.layout.nav_header_main);
        username = (TextView) view.findViewById(R.id.username);
        emailaddress = (TextView) view.findViewById(R.id.email_address);
        userImage = (CircleImageView) view.findViewById(R.id.user_image);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    getWindow().setExitTransition(new Explode());
                    startActivity(new Intent(MainActivity.this, TransactionActivity.class),
                            ActivityOptions
                                    .makeSceneTransitionAnimation(MainActivity.this).toBundle());
                } else {
                    startActivity(new Intent(MainActivity.this, TransactionActivity.class));
                }
            }
        });

        username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), USER_DATA);
            }
        });

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence[] items = {"从相册中选择", "拍照"};
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("选择图片来源")
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == SELECT_FROM_ALBUM) {
                                    //从相册选择图片
                                    Intent intent = new Intent(Intent.ACTION_PICK);
                                    intent.setType("image/*");
                                    startActivityForResult(Intent.createChooser(intent, "选择图片"), SELECT_FROM_ALBUM);
                                } else {
                                    //拍摄照片
                                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    String photoName = "IMG" + UUID.randomUUID().toString() + ".jpg";
                                    //设置照片存储路径
                                    Uri photoPath = Uri.fromFile(new File(MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), photoName));
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoPath);
                                    uri = photoPath.toString();
                                    startActivityForResult(intent, TAKE_PHOTOS);
                                }
                            }
                        }).create().show();
            }
        });

        emptyView = (FrameLayout) findViewById(R.id.empty);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);


        if (sortFlag == SORT_DEFAULT) {
            UpdateUI();
        } else if (sortFlag == SORT_BY_TIME) {
            UpdateUIByTime();
        }
        sp = getApplicationContext().getSharedPreferences("userInfo", MODE_PRIVATE);
        setUserInfo(sp);


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sortFlag == SORT_DEFAULT) {
            UpdateUI();
        } else if (sortFlag == SORT_BY_TIME) {
            UpdateUIByTime();
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_sort_by_time) {
            UpdateUIByTime();
            sortFlag = SORT_BY_TIME;
            return true;
        } else if (id == R.id.action_sort_default) {
            UpdateUI();
            sortFlag = SORT_DEFAULT;
            return true;
        } else if (id == R.id.action_delete_all_transactions) {
            new AlertDialog.Builder(this)
                    .setTitle("确认")
                    .setMessage("即将删除所有事件")
                    .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            TransactionLab.get().deleteTransactions();
                            UpdateUI();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_import) {
            transactionns = CalendarUtils.getCalendarEvent(this, transactionns);
            if (sortFlag == SORT_DEFAULT) {
                UpdateUI();
            } else if (sortFlag == SORT_BY_TIME) {
                UpdateUIByTime();
            }
        } else if (id == R.id.nav_robot) {
//TODO
        } else if (id == R.id.nav_today_in_history) {
//TODO
        }else if(id==R.id.nav_change_theme){
            //TODO
        } else if (id == R.id.nav_feedback) {

            if (isAppInstalled(this, "com.tencent.mobileqq")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(qqUrl)));
            } else {
                Snackbar.make(getWindow().getDecorView(), "没有安装QQ", Snackbar.LENGTH_LONG).show();
            }

        } else if (id == R.id.nav_info) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Info")
                    .setView(R.layout.dialog_info);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == USER_DATA) {
                String userName = data.getStringExtra("username");
                String emailAddress = data.getStringExtra("emailaddress");
                if (userName == null || emailAddress == null) {
                    username.setText("未知用户");
                    emailaddress.setText("未知邮箱");
                } else {
                    username.setText(userName);
                    emailaddress.setText(emailAddress);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("username", userName);
                    editor.putString("emailaddress", emailAddress);
                    editor.apply();
                    Snackbar.make(getWindow().getDecorView(), "欢迎，" + userName, Snackbar.LENGTH_LONG).show();
                }
            } else if (requestCode == SELECT_FROM_ALBUM) {
                Uri path = data.getData();
                Glide.with(this)
                        .load(path)
                        .into(userImage);
                uri = path.toString();
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("userimage", uri);
                editor.apply();
            } else if (requestCode == TAKE_PHOTOS) {
                //相机的回传
                if (uri != null) {
                    Glide.with(this)
                            .load(uri)
                            .into(userImage);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("userimage", uri);
                    editor.apply();

                }
            }
        }
    }

    private void setUserInfo(SharedPreferences sp) {
        username.setText(sp.getString("username", "未知用户"));
        emailaddress.setText(sp.getString("emailaddress", "未知邮箱"));
        String path = sp.getString("userimage", null);
        if (path!=null){
            Glide.with(this).load(path).into(userImage);
        }
    }

    private void UpdateUI() {
        TransactionLab transactionLab = TransactionLab.get();
        transactionns = transactionLab.getTransactionnsByDefault();

        if (transactionns.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        if (mHistoryAdapter == null) {
            mHistoryAdapter = new HistoryAdapter(transactionns, this);
            mRecyclerView.setAdapter(mHistoryAdapter);
        } else {
            mHistoryAdapter.setTransactionns(transactionns);
            mHistoryAdapter.notifyDataSetChanged();

        }

    }

    private void UpdateUIByTime() {
        TransactionLab transactionLab = TransactionLab.get();
        transactionns = transactionLab.getTransactionnsByTime();

        if (transactionns.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }


        if (mHistoryAdapter == null) {
            mHistoryAdapter = new HistoryAdapter(transactionns, this);
            mRecyclerView.setAdapter(mHistoryAdapter);
        } else {
            mHistoryAdapter.setTransactionns(transactionns);
            mHistoryAdapter.notifyDataSetChanged();
        }
    }

    //查找特定app是否被安装
    private boolean isAppInstalled(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        List<String> pName = new ArrayList<String>();
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName;
                pName.add(pn);
            }
        }
        return pName.contains(packageName);
    }

    @Override
    public void updateUIFromDeleteDialog() {
        if (sortFlag == SORT_DEFAULT) {
            UpdateUI();
        } else if (sortFlag == SORT_BY_TIME) {
            UpdateUIByTime();
        }
    }
}

