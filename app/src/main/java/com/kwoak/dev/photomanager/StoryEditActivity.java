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

    String[] photoPaths;    // 이전 액티비티에서 받아온 사진들 경로를 저장하는 변수
    ListView listView;
    ListViewAdapter adapter;

    EditText title; // 스토리 제목
    EditText memo;  // 스토리 메모
    TextView buttonFinish;  // 완료 버튼

    int screenWidth;

    boolean isClickFinish = false;  // 완료 버튼을 누르면 다른 동작들 무시하게 하기 위한 변수

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_edit);

        // 인스턴스 초기화
        activity = this;

        // 핸드폰 화면의 너비 구하기
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;

        // 리스트뷰 초기화 및 어뎁터 등록
        adapter = new ListViewAdapter(getApplicationContext());
        listView = (ListView) findViewById(R.id.storyedit_list);
        listView.setAdapter(adapter);

        // 받아온 사진 경로를 이용해 리스트뷰에 사진 등록
        photoPaths = getIntent().getStringExtra("paths").split(",");
        for (int i = 0; i< photoPaths.length; i++) {
            Log.d("StoryEdit", photoPaths[i]);
            adapter.addItem(photoPaths[i]);
        }
        setListViewHeightBasedOnChildren(listView);

        // 받아온 제목 값을 넣어줌
        title = (EditText) findViewById(R.id.storyedit_title);
        title.setText(getIntent().getStringExtra("title"));

        // 받아온 메모 값을 넣어줌
        memo = (EditText) findViewById(R.id.storyedit_memo);
        memo.setText(getIntent().getStringExtra("memo"));

        // 편집 완료 버튼 클릭 리스너 등록
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
                data.time = Long.toString(System.currentTimeMillis());
                // 이미 작성한 글을 수정하는 경우엔 DB Update 해주고
                boolean isBeforeDetail = getIntent().getBooleanExtra("isBeforeDetail", false);
                if (isBeforeDetail) {
                    dbAdapter.update(data, getIntent().getStringExtra("time"));
                // 처음 작성하는 글이면 DB Insert 해줌
                } else {
//                     TODO : 나중에 지워야 됨
//                     테스트를 위해 임시로 시간 0~5달 랜덤으로 뺌
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.setTimeInMillis(System.currentTimeMillis());
//                    calendar.add(Calendar.MONTH, -(new Random()).nextInt(6));
//                    data.time = Long.toString(calendar.getTimeInMillis());
                    dbAdapter.insert(data);
                }
                dbAdapter.close();

                // 이전 액티비티로 이동
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

        // 리스트뷰 아이템 추가 함수
        public void addItem(String path) {
            StoryEditData addInfo = new StoryEditData();

            addInfo.path = path;

            listData.add(addInfo);
        }

        // 리스트뷰 아이템 삭제 함수
        public void removeItem(int position) {
            listData.remove(position);
            adapter.notifyDataSetChanged();
            setListViewHeightBasedOnChildren(listView);
        }
    }

    // 리스트뷰의 높이 구하는 함수
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
