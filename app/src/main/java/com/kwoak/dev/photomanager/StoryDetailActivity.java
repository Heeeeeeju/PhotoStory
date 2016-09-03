package com.kwoak.dev.photomanager;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class StoryDetailActivity extends AppCompatActivity {

    TextView buttonEdit;    // 편집뷰로 이동하기 위한 버튼
    TextView title;
    TextView memo;
    TextView time;

    // 하단에 축소한 썸네일 리스트를 보여주는 뷰
    LinearLayout gallery;

    // 카메라에서 찍은 사진들의 경로 배열
    String[] paths;

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_detail);

        // 인스턴스 등록
        activity = this;

        // DB 열기
        DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
        dbAdapter.open();

        // 최종 수정 시간 가져오기
        String timeString = getIntent().getStringExtra("time");
        final StoryData rowData = dbAdapter.getStoryData(timeString);

        // 편집 버튼 클릭 리스너 등록
        buttonEdit = (TextView) findViewById(R.id.storydetail_edit);
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StoryDetailActivity.this, StoryEditActivity.class);
                intent.putExtra("isBeforeDetail", true);
                intent.putExtra("title", title.getText().toString());
                intent.putExtra("memo", memo.getText().toString());
                intent.putExtra("paths", android.text.TextUtils.join(",", paths));
                intent.putExtra("time", rowData.time);
                startActivity(intent);
            }
        });

        // 받아온 제목 입력
        title = (TextView) findViewById(R.id.storydetail_title);
        title.setText(rowData.title);

        // 받아온 메모 입력
        memo = (TextView) findViewById(R.id.storydetail_memo);
        memo.setText(rowData.memo);

        // 받아온 시간을 이용해 최종 수정 시간 표시
        long timeLong = Long.parseLong(rowData.time);
        Date date = new Date(timeLong);
        SimpleDateFormat transFormat = new SimpleDateFormat("수정 yyyy년 MM월 dd일 HH시 mm분");
        time = (TextView) findViewById(R.id.storydetail_time);
        time.setText(transFormat.format(date));

        // 겔러리 초기화 및 이미지들 출력
        gallery = (LinearLayout) findViewById(R.id.storydetail_gallery);
        paths = rowData.paths.split(",");
        final int imageSize = paths.length;
        final ArrayList<String> pathList = new ArrayList<String>(Arrays.asList(paths));
        final int imageWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
        for (int i=0; i<imageSize; i++) {
            final int index = i;
            ImageView image = new ImageView(getApplicationContext());
            image.setLayoutParams(new LinearLayout.LayoutParams(imageWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 눌렀을 때 원본 이미지를 확인할 수 있는 뷰로 이동하기 위한 클릭 리스너 등록
                    Intent intent = new Intent(StoryDetailActivity.this, PhotoExpandActivity.class);
                    intent.putExtra("size", imageSize);
                    intent.putExtra("position", index);
                    intent.putStringArrayListExtra("pathList", pathList);
                    startActivity(intent);
                }
            });
            if (i < imageSize - 1) {
                image.setPadding(0, 0, 10, 0);
            }
            Picasso.with(getApplicationContext()).load(new File(paths[i])).fit().into(image);
            gallery.addView(image);
        }
    }

    @Override
    public void onBackPressed() {
        StoryListActivity.activity.finish();
        Intent intent = new Intent(StoryDetailActivity.this, StoryListActivity.class);
        startActivity(intent);
        finish();
    }
}
