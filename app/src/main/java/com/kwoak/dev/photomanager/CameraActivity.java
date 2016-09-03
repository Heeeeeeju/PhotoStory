package com.kwoak.dev.photomanager;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by HEEJU on 2016-08-29.
 */
public class CameraActivity extends Activity {

    // 카메라 화면을 담기 위한 변수들
    protected static final String TAG = null;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private boolean inProgress;

    private TextView buttonExit;
    private ImageView buttonTakePhoto;
    private TextView buttonFinish;

    // 스토리에 사용될 사진 경로들
    ArrayList<String> photoPaths;

    // 찍은 사진의 개수
    int numOfPhoto = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);

        photoPaths = new ArrayList<String>();

        // 카메라 화면을 담을 서피스뷰 초기화
        surfaceView =  (SurfaceView) findViewById(R.id.camera_surface);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceListener);

        // 종료 버튼 클릭 리스너 등록
        buttonExit = (TextView) findViewById(R.id.camera_exit);
        buttonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // 사진 촬영 버튼 클릭 리스너 등록
        buttonTakePhoto = (ImageView) findViewById(R.id.camera_take_photo);
        buttonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(camera !=null && inProgress == false) {
                    camera.takePicture(null, null, takePicture);
                    inProgress = true;
                }
            }
        });

        // 촬영 완료 버튼 클릭 리스너 등록
        buttonFinish = (TextView) findViewById(R.id.camera_finish);
        buttonFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 다른 버튼들 막기
                buttonExit.setClickable(false);
                buttonExit.setTextColor(Color.GRAY);
                buttonTakePhoto.setClickable(false);
                buttonFinish.setClickable(false);
                buttonFinish.setTextColor(Color.GRAY);

                // 편집뷰로 이동
                Intent intent = new Intent(CameraActivity.this, StoryEditActivity.class);
                intent.putExtra("paths", android.text.TextUtils.join(",", photoPaths.toArray()));
                startActivity(intent);
                StoryListActivity.activity.finish();
                finish();
            }
        });
    }

    // 카메라 촬영 함수
    private Camera.PictureCallback takePicture = new Camera.PictureCallback() {

        // 사진을 저장하기 위한 파일
        private File imageFile;

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, "셔터 클릭");

            try {
                // convert byte array into bitmap
                Bitmap loadedImage = null;
                Bitmap rotatedBitmap = null;
                loadedImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                // rotate Image
                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(90);
                rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(), loadedImage.getHeight(), rotateMatrix, false);
                String state = Environment.getExternalStorageState();
                File folder;
                if (state.contains(Environment.MEDIA_MOUNTED)) {
                    folder = new File(Environment.getExternalStorageDirectory() + "/PhtoStory");
                } else {
                    folder = new File(Environment.getExternalStorageDirectory() + "/PhotoStory");
                }

                boolean success = true;
                if (!folder.exists()) {
                    success = folder.mkdirs();
                }
                if (success) {
                    java.util.Date date = new java.util.Date();
                    String path = folder.getAbsolutePath()
                            + File.separator
                            + "PhotoStory_"
                            + date.getTime()
                            + ".jpg";
                    photoPaths.add(path);
                    imageFile = new File(path);
                    imageFile.createNewFile();
                } else {
                    Toast.makeText(getBaseContext(), "Image Not saved", Toast.LENGTH_SHORT).show();
                    return;
                }

                ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                // 촬영한 사진 갤러리에 저장
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);

                FileOutputStream fout = new FileOutputStream(imageFile);
                fout.write(ostream.toByteArray());
                fout.close();
                ContentValues values = new ContentValues();

                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.MediaColumns.DATA, imageFile.getAbsolutePath());

                CameraActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 찍은 사진의 개수 표시
            buttonFinish.setText(String.format("완료(%d)", ++numOfPhoto));

            // 카메라 다시 시작
            camera.startPreview();
            inProgress = false;
        }
    };

    private SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "카메라 기능 해제");
            camera.release();
            camera = null;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "카메라 미리보기 활성");
            camera = Camera.open();
            try {
                camera.setPreviewDisplay(holder);
                camera.setDisplayOrientation(90);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
            Log.i(TAG,"카메라 미리보기 활성");

            Camera.Parameters parameters = camera.getParameters();

            parameters.setPreviewSize(width, height);
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            parameters.setJpegQuality(100);
            parameters.setRotation(90);
            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            if (display.getRotation() == Surface.ROTATION_0) {
                camera.setDisplayOrientation(90);
            } else if (display.getRotation() == Surface.ROTATION_270) {
                camera.setDisplayOrientation(180);
            }

            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            Camera.Size size = sizes.get(0);
            for(int i=0;i<sizes.size();i++) {
                if(sizes.get(i).width > size.width)
                    size = sizes.get(i);
            }
            parameters.setPictureSize(size.width, size.height);

            camera.setParameters(parameters);
            camera.startPreview();
        }
    };
}
