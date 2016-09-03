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

    TextView buttonEdit;
    TextView title;
    TextView memo;
    TextView time;

    LinearLayout gallery;

    String[] paths;

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_detail);

        activity = this;

        // DB 열기
        DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
        dbAdapter.open();

        String timeTemp = getIntent().getStringExtra("time");
        final StoryData rowData = dbAdapter.getStoryData(timeTemp);

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

        title = (TextView) findViewById(R.id.storydetail_title);
        title.setText(rowData.title);

        memo = (TextView) findViewById(R.id.storydetail_memo);
        memo.setText(rowData.memo);

        long timeLong = Long.parseLong(rowData.time);
        Date date = new Date(timeLong);
        SimpleDateFormat transFormat = new SimpleDateFormat("수정 yyyy년 MM월 dd일 HH시 mm분");
        time = (TextView) findViewById(R.id.storydetail_time);
        time.setText(transFormat.format(date));

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
