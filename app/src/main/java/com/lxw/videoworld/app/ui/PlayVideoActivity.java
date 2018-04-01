package com.lxw.videoworld.app.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.Transition;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.lxw.videoworld.R;
import com.lxw.videoworld.app.model.KeyValueModel;
import com.lxw.videoworld.app.service.DownloadManager;
import com.lxw.videoworld.framework.base.BaseActivity;
import com.lxw.videoworld.framework.util.StatusBarUtil;
import com.lxw.videoworld.framework.util.ToastUtil;
import com.lxw.videoworld.framework.widget.OnTransitionListener;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.model.GSYVideoModel;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.ListGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 单独的视频播放页面
 */
public class PlayVideoActivity extends BaseActivity {

    public final static String IMG_TRANSITION = "IMG_TRANSITION";
    public final static String TRANSITION = "TRANSITION";

    @BindView(R.id.video_player)
    ListGSYVideoPlayer videoPlayer;
    @BindView(R.id.recyclerview_source_links)
    RecyclerView recyclerviewSourceLinks;
    @BindView(R.id.drawerlayout_source_links)
    DrawerLayout drawerlayoutSourceLinks;

    private BaseQuickAdapter<KeyValueModel, BaseViewHolder> sourceLinkAdapter;

    OrientationUtils orientationUtils;

    private boolean isTransition;

    private Transition transition;
    private String url;
    private ArrayList<String> urlList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 修改 状态栏、导航栏的颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //设置状态栏颜色
            window.setStatusBarColor(getResources().getColor(R.color.color_000000));
            //设置导航栏颜色
            window.setNavigationBarColor(getResources().getColor(R.color.color_000000));
            ViewGroup contentView = ((ViewGroup) findViewById(android.R.id.content));
            View childAt = contentView.getChildAt(0);
            if (childAt != null) {
                childAt.setFitsSystemWindows(true);
            }
//            contentView.setPadding(0, getStatusBarHeight(this), 0, 0);
        }
//        StatusBarUtil.hideStatusBar(this);
        setContentView(R.layout.activity_play_video);
        ButterKnife.bind(this);
        isTransition = getIntent().getBooleanExtra(TRANSITION, false);
        url = getIntent().getStringExtra("url");
        urlList = getIntent().getStringArrayListExtra("urlList");
        if (urlList == null) urlList = new ArrayList<>();
        if (TextUtils.isEmpty(url) && urlList.size() == 0) {
            url = "";
            ToastUtil.showMessage("无效链接");
        } else if (TextUtils.isEmpty(url) && !TextUtils.isEmpty(urlList.get(0)))
            url = urlList.get(0);
        else if (TextUtils.isEmpty(url)) url = "";
        if (urlList.isEmpty()) urlList.add(url);
        setUpView();
    }

    private void setUpView() {
        GSYVideoManager.instance().setTimeOut(30000, false);

        //设置返回键
        videoPlayer.getBackButton().setVisibility(View.VISIBLE);

        //设置旋转
        orientationUtils = new OrientationUtils(this, videoPlayer);

        //设置全屏按键功能,这是使用的是选择屏幕，而不是全屏
        videoPlayer.getFullscreenButton().setScaleType(ImageView.ScaleType.FIT_CENTER);
        videoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StatusBarUtil.hideStatusBar(PlayVideoActivity.this);
                orientationUtils.resolveByClick();
            }
        });

        //videoPlayer.setBottomProgressBarDrawable(getResources().getDrawable(R.drawable.video_new_progress));
        //videoPlayer.setDialogVolumeProgressBar(getResources().getDrawable(R.drawable.video_new_volume_progress_bg));
        //videoPlayer.setDialogProgressBar(getResources().getDrawable(R.drawable.video_new_progress));
        //videoPlayer.setBottomShowProgressBarDrawable(getResources().getDrawable(R.drawable.video_new_seekbar_progress),
        //getResources().getDrawable(R.drawable.video_new_seekbar_thumb));
        //videoPlayer.setDialogProgressColor(getResources().getColor(R.color.colorAccent), -11);

        //是否可以滑动调整
        videoPlayer.setIsTouchWiget(true);
        // 滑动快进的比例，默认1。数值越大，滑动的产生的seek越小
        videoPlayer.setSeekRatio(5);

        //设置返回按键功能
        videoPlayer.getBackButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        //全屏
        videoPlayer.setIfCurrentIsFullscreen(false);

        List<KeyValueModel> sourceList = new ArrayList<>();
        for (int i = 0; i < urlList.size(); i++){
            boolean isSelected = i == urlList.indexOf(url);
            sourceList.add(new KeyValueModel("(" + (i + 1) +") " + urlList.get(i),  urlList.get(i), isSelected));
        }
        recyclerviewSourceLinks.setLayoutManager(new LinearLayoutManager(PlayVideoActivity.this));
        sourceLinkAdapter = new BaseQuickAdapter<KeyValueModel, BaseViewHolder>(R.layout.item_link_selector, sourceList) {
            @Override
            protected void convert(final BaseViewHolder helper, final KeyValueModel item) {
                helper.setText(R.id.selector, item.getKey());
                helper.addOnClickListener(R.id.content);
                if (item.isSelected()) {
                    helper.setTextColor(R.id.selector, getCustomColor(R.styleable.BaseColor_com_assist_A));
                } else
                    helper.setTextColor(R.id.selector, getCustomColor(R.styleable.BaseColor_com_font_A));
            }
        };
        sourceLinkAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                if (drawerlayoutSourceLinks.isDrawerOpen(GravityCompat.END)) {
                    drawerlayoutSourceLinks.closeDrawers();
                }
                KeyValueModel keyValueModel = (KeyValueModel) adapter.getData().get(position);
                if (!keyValueModel.isSelected()) {
                    String positionUrl = ((KeyValueModel) adapter.getData().get(position)).getValue().trim();
                    if (positionUrl.startsWith("ftp") || positionUrl.startsWith("thunder") || positionUrl.startsWith("ed2k") || positionUrl.startsWith("magnet")){
                        DownloadManager.addNormalTask(PlayVideoActivity.this, positionUrl, true, false, urlList);
                    }else {
                        for (int i = 0; i < adapter.getData().size(); i++) {
                            ((KeyValueModel) adapter.getData().get(i)).setSelected(i == position);
                        }
                        adapter.notifyDataSetChanged();
                        List<GSYVideoModel> list = new ArrayList<>();
                        for (int i = 0; i < urlList.size(); i++) {
                            list.add(new GSYVideoModel(urlList.get(i), ""));
                        }
                        videoPlayer.setUp(list, false, urlList.indexOf(url));
                        initTransition();
                    }
                }
            }
        });
        recyclerviewSourceLinks.setAdapter(sourceLinkAdapter);

        // 播放
        if (urlList.indexOf(url) >= 0) {
            List<GSYVideoModel> list = new ArrayList<>();
            for (int i = 0; i < urlList.size(); i++) {
                list.add(new GSYVideoModel(urlList.get(i), ""));
            }
            videoPlayer.setUp(list, false, urlList.indexOf(url));
        } else videoPlayer.setUp(url, false, "");
        //过渡动画
        initTransition();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        url = intent.getStringExtra("url");
        urlList = intent.getStringArrayListExtra("urlList");
        if (urlList == null) urlList = new ArrayList<>();
        if (TextUtils.isEmpty(url) && urlList.size() == 0) {
            url = "";
            ToastUtil.showMessage("无效链接");
        } else if (TextUtils.isEmpty(url) && !TextUtils.isEmpty(urlList.get(0)))
            url = urlList.get(0);
        else if (TextUtils.isEmpty(url)) url = "";
        if (urlList.isEmpty()) urlList.add(url);
        setUpView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }

    @Override
    public void onBackPressed() {
        //先返回正常状态
        if (orientationUtils.getScreenType() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            StatusBarUtil.showSystemBar(PlayVideoActivity.this);
            videoPlayer.getFullscreenButton().performClick();
            return;
        }
        //释放所有
        videoPlayer.setStandardVideoAllCallBack(null);
        GSYVideoPlayer.releaseAllVideos();
        if (isTransition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onBackPressed();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                    overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
                }
            }, 500);
        }
    }


    private void initTransition() {
        if (isTransition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
            ViewCompat.setTransitionName(videoPlayer, IMG_TRANSITION);
            addTransitionListener();
            startPostponedEnterTransition();
        } else {
            videoPlayer.startPlayLogic();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean addTransitionListener() {
        transition = getWindow().getSharedElementEnterTransition();
        if (transition != null) {
            transition.addListener(new OnTransitionListener() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    super.onTransitionEnd(transition);
                    videoPlayer.startPlayLogic();
                    transition.removeListener(this);
                }
            });
            return true;
        }
        return false;
    }

}
