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

    int size = 0;
    int position = 0;
    ArrayList<String> pathList;

//    LinearLayout gallery;
    ViewPager gallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_expand);

        size = getIntent().getIntExtra("size", 0);
        position = getIntent().getIntExtra("position", 0);
        pathList = getIntent().getStringArrayListExtra("pathList");

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
//            return super.instantiateItem(container, position);
            ImageView image = new ImageView(getApplicationContext());
            image.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            Picasso.with(getApplicationContext()).load(new File(pathList.get(position))).fit().into(image);
            container.addView(image);
            return image;
        }
    }
}
