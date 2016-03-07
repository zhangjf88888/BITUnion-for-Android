package bit.ihainan.me.bitunionforandroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.squareup.picasso.Picasso;
import com.umeng.analytics.MobclickAgent;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bit.ihainan.me.bitunionforandroid.R;
import bit.ihainan.me.bitunionforandroid.adapters.PostListAdapter;
import bit.ihainan.me.bitunionforandroid.models.ThreadReply;
import bit.ihainan.me.bitunionforandroid.ui.assist.SimpleDividerItemDecoration;
import bit.ihainan.me.bitunionforandroid.ui.assist.SwipeActivity;
import bit.ihainan.me.bitunionforandroid.utils.network.BUApi;
import bit.ihainan.me.bitunionforandroid.utils.CommonUtils;
import bit.ihainan.me.bitunionforandroid.utils.Global;
import bit.ihainan.me.bitunionforandroid.utils.network.ExtraApi;
import bit.ihainan.me.bitunionforandroid.utils.ui.HtmlUtil;
import jp.wasabeef.picasso.transformations.BlurTransformation;

public class ThreadDetailActivity extends SwipeActivity {
    private final static String TAG = ThreadDetailActivity.class.getSimpleName();

    // UI references
    private AppBarLayout mAppbar;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
    private CollapsingToolbarLayout mCollapsingToolbar;
    private ImageView mBackdrop;

    // Bundle tags
    public final static String THREAD_ID_TAG = "THREAD_ID_TAG";
    public final static String THREAD_NAME_TAG = "THREAD_NAME_TAG";
    public final static String THREAD_AUTHOR_NAME_TAG = "THREAD_AUTHOR_NAME_TAG";
    public final static String THREAD_REPLY_COUNT_TAG = "THREAD_REPLY_COUNT_TAG";

    private void getExtra() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mTid = bundle.getLong(THREAD_ID_TAG);
        mThreadName = bundle.getString(THREAD_NAME_TAG);
        mReplyCount = bundle.getLong(THREAD_REPLY_COUNT_TAG);
        mAuthorName = bundle.getString(THREAD_AUTHOR_NAME_TAG);
        if (mTid == null) {
            Global.readConfig(this);
            mTid = 10609296l;
        }
    }

    // Data
    private Long mTid, mReplyCount;
    private String mThreadName, mAuthorName;
    private long mCurrentPosition = 0;
    private boolean mIsLoading = false;
    private List<bit.ihainan.me.bitunionforandroid.models.ThreadReply> mThreadPostList = new ArrayList<>();
    private PostListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_detail);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get thread name and id
        getExtra();

        // Toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Title
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        mCollapsingToolbar.setTitle(Html.fromHtml(mThreadName));
        mCollapsingToolbar.setExpandedTitleTextAppearance(R.style.ExpandedAppBar);

        if (mThreadName == null || mReplyCount == null) {
            // TODO: 获取标题信息和回复数目信息
        } else {
        }

        // Cover
        mBackdrop = (ImageView) findViewById(R.id.backdrop);
        // fillBackdrop();

        // Setup RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.detail_recycler_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.home_swipe_refresh_layout);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecyclerView.smoothScrollToPosition(0);
            }
        });
        setupRecyclerView();
        setupSwipeRefreshLayout();

        // Swipe to back
        setSwipeAnyWhere(false);
    }


    private void fillBackdrop() {
        if (!Global.ascendingOrder && CommonUtils.isWifi(this) || !Global.saveDataMode) {
            BUApi.getPostReplies(this, mTid, 0, 1, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (BUApi.checkStatus(response)) {

                        try {
                            JSONArray newListJson = response.getJSONArray("postlist");

                            List<bit.ihainan.me.bitunionforandroid.models.ThreadReply> threads = BUApi.MAPPER.readValue(newListJson.toString(),
                                    new TypeReference<List<ThreadReply>>() {
                                    });

                            if (threads.size() > 0) {
                                ThreadReply firstReply = threads.get(0);
                                if (firstReply.attachext != null && Integer.valueOf(firstReply.filesize) / 1000 <= 300 && (firstReply.attachext.equals("png") || firstReply.attachext.equals("jpg")
                                        || firstReply.attachext.equals("jpeg"))) {
                                    String imageURL = CommonUtils.getRealImageURL(CommonUtils.decode(firstReply.attachment));
                                    Picasso.with(ThreadDetailActivity.this).load(imageURL).transform(new BlurTransformation(ThreadDetailActivity.this)).into(mBackdrop);
                                }
                            }
                        } catch (Exception e) {
                            String message = getString(R.string.error_parse_json) + "\n" + response;
                            Log.e(TAG, message, e);
                            CommonUtils.debugToast(ThreadDetailActivity.this, message);
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String message = getString(R.string.error_network);
                    Log.e(TAG, message, error);
                    CommonUtils.debugToast(ThreadDetailActivity.this, message);
                }
            });
        }
    }

    private MenuItem mFavorItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.thread_detail_menu, menu);
        setMenuIcon(menu.findItem(R.id.change_order));
        mFavorItem = menu.findItem(R.id.favor);
        getFavoriteStatus();
        return true;
    }

    private void setMenuIcon(MenuItem menuItem) {
        if (Global.ascendingOrder)
            menuItem.setIcon(R.drawable.ic_low_priority_white_24dp);
        else
            menuItem.setIcon(R.drawable.ic_high_priority_white_24dp);
    }

    private boolean favorClickable = true;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_order:
                Global.ascendingOrder = !Global.ascendingOrder;
                setMenuIcon(item);
                Snackbar.make(mRecyclerView, (Global.ascendingOrder ? "升序" : "降序") + "显示回帖列表", Snackbar.LENGTH_SHORT).show();
                Global.saveConfig(ThreadDetailActivity.this);

                reloadData();
                break;

            case R.id.favor:
                if (favorClickable) {
                    favorClickable = !favorClickable;   // 不允许重复点击
                    hasFavor = !hasFavor;
                    if (hasFavor) {
                        // 之前是删除，想要添加
                        mFavorItem.setIcon(R.drawable.ic_favorite_white_24dp);
                        mFavorItem.setTitle("取消收藏");
                        addFavorite();
                    } else {
                        mFavorItem.setTitle("添加收藏");
                        mFavorItem.setIcon(R.drawable.ic_favorite_border_white_24dp);
                        delFavorite();
                    }
                }
                break;
        }

        return true;
    }

    public boolean hasFavor = false;


    private void getFavoriteStatus() {
        ExtraApi.getFavoriteStatus(this, mTid, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getInt("code") == 0) {
                        hasFavor = response.getBoolean("data");
                        CommonUtils.debugToast(ThreadDetailActivity.this, "hasFavor = " + hasFavor);
                        if (hasFavor) {
                            mFavorItem.setTitle("取消收藏");
                            mFavorItem.setIcon(R.drawable.ic_favorite_white_24dp);
                        } else {
                            mFavorItem.setTitle("添加收藏");
                            mFavorItem.setIcon(R.drawable.ic_favorite_border_white_24dp);
                        }
                    } else {
                        String message = "获取收藏状态失败，失败原因 " + response.getString("message");
                        if (Global.debugMode) {
                            CommonUtils.debugToast(ThreadDetailActivity.this, message);
                        }
                        Log.w(TAG, message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, getString(R.string.error_parse_json) + ": " + response, e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "getFavoriteStatus >> " + getString(R.string.error_network), error);
            }
        });
    }

    private Response.Listener mFavorListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            favorClickable = !favorClickable;
            try {
                if (response.getInt("code") == 0) {
                    // 成功添加 / 删除收藏，皆大欢喜
                    String message = hasFavor ? "添加收藏成功" : "删除收藏成功";
                    Log.d(TAG, "mFavorListener >> " + message);
                    Toast.makeText(ThreadDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    // Oh no!!!
                    String message = (hasFavor ? "添加" : "删除") + "收藏失败";
                    String debugMessage = message + " - " + response.get("message");
                    if (Global.debugMode) {
                        CommonUtils.debugToast(ThreadDetailActivity.this, debugMessage);
                    } else {
                        Toast.makeText(ThreadDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    Log.w(TAG, debugMessage);

                    hasFavor = !hasFavor;
                    if (hasFavor) {
                        mFavorItem.setIcon(R.drawable.ic_favorite_white_24dp);
                        mFavorItem.setTitle("取消收藏");
                    } else {
                        mFavorItem.setTitle("添加收藏");
                        mFavorItem.setIcon(R.drawable.ic_favorite_border_white_24dp);
                    }
                }
            } catch (JSONException e) {
                String message = (hasFavor ? "添加" : "删除") + "收藏失败";
                String debugMessage = message + " - " + getString(R.string.error_parse_json) + " " + response;
                if (Global.debugMode) {
                    CommonUtils.debugToast(ThreadDetailActivity.this, debugMessage);
                } else {
                    Toast.makeText(ThreadDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, debugMessage, e);

                hasFavor = !hasFavor;
                if (hasFavor) {
                    mFavorItem.setIcon(R.drawable.ic_favorite_white_24dp);
                    mFavorItem.setTitle("取消收藏");
                } else {
                    mFavorItem.setTitle("添加收藏");
                    mFavorItem.setIcon(R.drawable.ic_favorite_border_white_24dp);
                }
            }
        }
    };

    private Response.ErrorListener mFavorErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            String message = (hasFavor ? "取消收藏失败，" : "添加收藏失败") + "无法连接到服务器";
            Snackbar.make(mRecyclerView, message, Snackbar.LENGTH_INDEFINITE).setAction("RETRY", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (hasFavor) addFavorite();
                    else delFavorite();
                }
            }).show();
        }
    };

    private void addFavorite() {
        ExtraApi.addFavorite(this, mTid, mThreadName, mAuthorName, mFavorListener, mFavorErrorListener);
    }

    private void delFavorite() {
        ExtraApi.delFavorite(this, mTid, mFavorListener, mFavorErrorListener);
    }

    private void setupRecyclerView() {
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(ThreadDetailActivity.this));
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Adapter
        mAdapter = new PostListAdapter(this, mThreadPostList, mAuthorName, mReplyCount);
        mRecyclerView.setAdapter(mAdapter);

        // 自动加载
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int mLastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
                if (dy > 0 && mLastVisibleItem >= mThreadPostList.size() - 2 && !mIsLoading) {
                    loadMore(true);
                }
            }
        });
    }

    private void setupSwipeRefreshLayout() {
        mSwipeRefreshLayout.setDistanceToTriggerSync(Global.SWIPE_LAYOUT_TRIGGER_DISTANCE);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 重新加载数据
                reloadData();
            }
        });

        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                // 第一次加载数据
                reloadData();
            }
        });
    }

    /**
     * 重新拉取数据
     */
    private void reloadData() {
        mIsLoading = true;
        mSwipeRefreshLayout.setRefreshing(true);
        mThreadPostList.clear();
        mCurrentPosition = Global.ascendingOrder ? 0 : mReplyCount - Global.LOADING_REPLIES_COUNT;
        loadMore(false);
    }

    private void loadMore(boolean isAddProgressBar) {
        // 拉取数据，显示进度
        Log.i(TAG, "onScrolled >> 即将到底，准备请求新数据");
        if (isAddProgressBar) {
            mThreadPostList.add(null);
            mAdapter.notifyItemInserted(mThreadPostList.size() - 1);
            mIsLoading = true;
        }

        refreshData(mCurrentPosition, mCurrentPosition + Global.LOADING_REPLIES_COUNT - 1); // 0 - 9, 10 - 19
    }

    /**
     * 更新列表数据
     */
    private void refreshData(final long from, final long to) {
        long newFrom = from < 0 ? 0 : from;
        long newTo = to > mReplyCount - 1 ? mReplyCount - 1 : to;
        BUApi.getPostReplies(this, mTid, newFrom, newTo + 1,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mSwipeRefreshLayout.setRefreshing(false);

                        if (BUApi.checkStatus(response)) {
                            try {
                                JSONArray newListJson = response.getJSONArray("postlist");
                                List<bit.ihainan.me.bitunionforandroid.models.ThreadReply> newThreads = BUApi.MAPPER.readValue(newListJson.toString(),
                                        new TypeReference<List<ThreadReply>>() {
                                        });

                                // 成功拿到数据，删除 Loading Progress Bar
                                if (mThreadPostList.size() > 0) {
                                    mThreadPostList.remove(mThreadPostList.size() - 1);
                                    mAdapter.notifyItemRemoved(mThreadPostList.size());
                                }

                                CommonUtils.debugToast(ThreadDetailActivity.this, "Loaded " + newThreads.size() + " more item(s)");

                                // 处理数据
                                for (ThreadReply reply : newThreads) {
                                    // 处理正文
                                    String body = CommonUtils.decode(reply.message);
                                    reply.useMobile = body.contains("From BIT-Union Open API Project");
                                    HtmlUtil htmlUtil = new HtmlUtil(CommonUtils.decode(reply.message));
                                    reply.message = htmlUtil.makeAll();

                                    // 获取设备信息
                                    getDeviceName(reply);
                                }

                                // 更新 RecyclerView
                                if (!Global.ascendingOrder) Collections.reverse(newThreads); // 倒序
                                mCurrentPosition += (Global.ascendingOrder ? Global.LOADING_REPLIES_COUNT : -Global.LOADING_REPLIES_COUNT);
                                mThreadPostList.addAll(newThreads);
                                mAdapter.notifyDataSetChanged();

                                // 判断是否到头
                                if (!(Global.ascendingOrder && to >= mReplyCount - 1
                                        || !Global.ascendingOrder && from <= 0)) {
                                    mIsLoading = false;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, getString(R.string.error_parse_json) + "\n" + response, e);

                                if (mThreadPostList.size() > 0) {
                                    mThreadPostList.remove(mThreadPostList.size() - 1);
                                    mAdapter.notifyItemRemoved(mThreadPostList.size());
                                }

                                Snackbar.make(mRecyclerView, getString(R.string.error_parse_json),
                                        Snackbar.LENGTH_INDEFINITE).setAction("RETRY", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        loadMore(true);
                                    }
                                }).show();
                            }
                        } else {
                            Log.i(TAG, "refreshData >> " + getString(R.string.error_unknown_json) + "" + response);

                            if (mThreadPostList.size() > 0) {
                                mThreadPostList.remove(mThreadPostList.size() - 1);
                                mAdapter.notifyItemRemoved(mThreadPostList.size());
                            }

                            Snackbar.make(mRecyclerView, getString(R.string.error_unknown_json), Snackbar.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // 服务器请求失败，说明网络不好，只能通过 RETRY 来重新拉取数据
                        if (mThreadPostList.size() > 0) {
                            mThreadPostList.remove(mThreadPostList.size() - 1);
                            mAdapter.notifyItemRemoved(mThreadPostList.size());
                        }

                        mSwipeRefreshLayout.setRefreshing(false);

                        Snackbar.make(mRecyclerView, getString(R.string.error_network), Snackbar.LENGTH_INDEFINITE).setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                loadMore(true);
                            }
                        }).show();

                        Log.e(TAG, getString(R.string.error_network), error);
                    }
                });
    }


    private void getDeviceName(ThreadReply reply) {
        // Log.d(TAG, "getDeviceName >> " + reply.message);
        String[] regexStrArray = new String[]{"<a .*?>\\.\\.::发自(.*?)::\\.\\.</a>$",
                "<br><br>发送自 <a href='.*?' target='_blank'><b>(.*?) @BUApp</b></a>",
                "<i>来自傲立独行的(.*?)客户端</i>$",
                "<br><br><i>发自联盟(.*?)客户端</i>$",
                "<a href='.*?>..::发自联盟(.*?)客户端::..</a>"};
        if (reply.message.contains("客户端") || reply.message.contains("发自"))
            Log.d(TAG, "getDeviceName >> " + reply.message);
        for (String regex : regexStrArray) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(reply.message);
            while (matcher.find()) {
                // 找到啦！
                reply.deviceName = matcher.group(1);
                if (reply.deviceName.equals("WindowsPhone8"))
                    reply.deviceName = "Windows Phone 8";
                if (reply.deviceName.equals("联盟iOS客户端"))
                    reply.deviceName = "iPhone";
                Log.d(TAG, "deviceName >> " + reply.deviceName);
                reply.message = reply.message.replace(matcher.group(0), "");
                reply.message = HtmlUtil.replaceOther(reply.message);
                return;
            }
        }

        reply.deviceName = "";
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 友盟 SDK
        if (Global.uploadData)
            MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 友盟 SDK
        if (Global.uploadData)
            MobclickAgent.onPause(this);
    }
}