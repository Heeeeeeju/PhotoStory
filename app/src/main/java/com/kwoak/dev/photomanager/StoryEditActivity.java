package com.kwoak.dev.photomanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class StoryEditActivity extends AppCompatActivity {

    String[] photoPaths;
    ListView listView;
    ListViewAdapter adapter;

    EditText title;
    EditText memo;
    TextView buttonFinish;

    int screenWidth;

    boolean isClickFinish = false;

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_edit);

        activity = this;

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;

        adapter = new ListViewAdapter(getApplicationContext());
        listView = (ListView) findViewById(R.id.storyedit_list);
        listView.setAdapter(adapter);
        photoPaths = getIntent().getStringExtra("paths").split(",");

        for (int i = 0; i< photoPaths.length; i++) {
            Log.d("StoryEdit", photoPaths[i]);
            adapter.addItem(photoPaths[i]);
        }
        setListViewHeightBasedOnChildren(listView);

        title = (EditText) findViewById(R.id.storyedit_title);
        title.setText(getIntent().getStringExtra("title"));

        memo = (EditText) findViewById(R.id.storyedit_memo);
        memo.setText(getIntent().getStringExtra("memo"));

        buttonFinish = (TextView) findViewById(R.id.storyedit_finish);
        buttonFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isClickFinish) { return; }
                isClickFinish = true;
                buttonFinish.setTextColor(Color.GRAY);

                // DB 열기
                DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
                dbAdapter.open();

                // DB에 정보 저장
                StoryData data = new StoryData();
                String bindingPath = android.text.TextUtils.join(",", photoPaths);
                data.paths = bindingPath;
                data.title = title.getText().toString();
                data.memo = memo.getText().toString();
                // 이미 작성한 글을 수정하는 경우엔 DB Update 해주고
                boolean isBeforeDetail = getIntent().getBooleanExtra("isBeforeDetail", false);
                if (isBeforeDetail) {
                    data.time = Long.toString(System.currentTimeMillis());
                    dbAdapter.update(data, getIntent().getStringExtra("time"));
                // 처음 작성하는 글이면 DB Insert 해줌
                } else {
                    //data.time = Long.toString(System.currentTimeMillis());
                    // TODO : 나중에 지워야 됨
                    // 테스트를 위해 임시로 시간 0~5달 랜덤으로 뺌
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.add(Calendar.MONTH, -(new Random()).nextInt(6));
                    data.time = Long.toString(calendar.getTimeInMillis());
                    dbAdapter.insert(data);
                }
                dbAdapter.close();

                Intent intent;
                if (isBeforeDetail) {
                    intent = new Intent(StoryEditActivity.this, StoryDetailActivity.class);
                    intent.putExtra("time", data.time);
                    StoryDetailActivity.activity.finish();
                } else {
                    intent = new Intent(StoryEditActivity.this, StoryListActivity.class);
                    StoryListActivity.activity.finish();
                }
                startActivity(intent);
                finish();
            }
        });
    }

    // 커스텀 리스트뷰 홀더
    public class ViewHolder {
        public ImageView photo;
        public ImageView buttonDelete;
    }

    public class StoryEditData {
        public String path;
    }

    // 커스텀 리스트뷰 어뎁터
    public class ListViewAdapter extends BaseAdapter {
        private Context context = null;
        private ArrayList<StoryEditData> listData = new ArrayList<StoryEditData>();

        public ListViewAdapter(Context context) {
            super();
            this.context = context;
        }

        public ArrayList<StoryEditData> getListData() { return listData; }

        @Override
        public int getCount() {
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            return listData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();

                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.listview_item_storyedit, parent, false);

                holder.photo = (ImageView) convertView.findViewById(R.id.item_storyedit_photo);
                holder.buttonDelete = (ImageView) convertView.findViewById(R.id.item_storyedit_delete);
                convertView.setTag(holder);
            } else{
                holder = (ViewHolder) convertView.getTag();
            }

            final StoryEditData data = listData.get(position);

            // 촬영한 사진 썸네일 가져와서 등록
            Bitmap resized = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(data.path), screenWidth, screenWidth);
            holder.photo.setImageBitmap(resized);
            // 이미지 삭제
            holder.buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    photoPaths[position] = "";
                    removeItem(position);
                }
            });

            return convertView;
        }

        public void addItem(String path) {
            StoryEditData addInfo = new StoryEditData();

            addInfo.path = path;

            listData.add(addInfo);
        }

        public void removeItem(int position) {
            listData.remove(position);
            adapter.notifyDataSetChanged();
            setListViewHeightBasedOnChildren(listView);
        }
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListViewAdapter listAdapter = (ListViewAdapter) listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();

    }
}