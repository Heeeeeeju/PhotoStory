package com.kwoak.dev.photomanager;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class PhotoExpandActivity extends AppCompatActivity {

    int size = 0;   // 사진 총 개수
    int position = 0;   // 상세뷰에서 누른 사진의 번호
    ArrayList<String> pathList;

    // 슬라이드 화면을 위한 뷰페이져
    ViewPager gallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_expand);

        size = getIntent().getIntExtra("size", 0);
        position = getIntent().getIntExtra("position", 0);
        pathList = getIntent().getStringArrayListExtra("pathList");

        // 뷰페이져 초기화 및 어뎁터 등록
        gallery = (ViewPager) findViewById(R.id.photoexpand_gallery);
        gallery.setAdapter(new GalleryPagerAdapter());
        gallery.setCurrentItem(position);
    }

    public class GalleryPagerAdapter extends PagerAdapter {
        public GalleryPagerAdapter() {
            super();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
        }

        @Override
        public int getCount() {
            return pathList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return (view == object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // 이미지 출력
            ImageView image = new ImageView(getApplicationContext());
            image.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            Picasso.with(getApplicationContext()).load(new File(pathList.get(position))).fit().into(image);
            container.addView(image);
            return image;
        }
    }
}
