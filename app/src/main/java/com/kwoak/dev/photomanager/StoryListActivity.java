package com.kwoak.dev.photomanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class StoryListActivity extends AppCompatActivity {

    ListView listView;
    ListViewAdapter adapter;

    TextView activityTitle; // 최상단 스토리(%개수%)
    ImageView buttonDeleteStory;    // 스토리 삭제 (비)활성화 버튼
    ImageView buttonAddStory;   // 스토리 추가 버튼
    EditText searchView;    // 스토리 검색 뷰
    ImageView buttonInitSearch; // 검색 뷰 초기화 버튼
    LinearLayout deleteView;    // 삭제 활성화 됐을 시 최하단에 나타나는 취소, 확인 버튼을 담은 뷰
    Button buttonDeleteCancel;  // 위의 뷰에 담긴 취소 버튼
    Button buttonDeleteOk;      // 위의 뷰에 담긴 확인 버튼

    int[] storySize;    // 각 섹션별로 몇 개의 스토리가 저장되어있는지를 나타내는 변수
    boolean isDeleteMode = false;   // 스토리 삭제 활성화 여부
    Calendar calendar;

    public static Activity activity;

    DBAdapter dbAdapter;

    @Override
    public void onBackPressed() {
        // 삭제가 활성화였을 경우엔 비활성화 시키고
        if (isDeleteMode) {
            buttonDeleteCancel.callOnClick();
        // 비활성화였을 경우엔 앱 종료
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stroylist);

        // 인스턴스 등록
        activity = this;

        // DB 열기
        dbAdapter = new DBAdapter(getApplicationContext());
        dbAdapter.open();

        // 최상단 타이틀
        activityTitle = (TextView) findViewById(R.id.storylist_title);

        // 삭제 (비)활성화 버튼 클릭 리스너 등록
        buttonDeleteStory = (ImageView) findViewById(R.id.storylist_button_delete_story);
        buttonDeleteStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDeleteMode = true;
                deleteView.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
                // TODO : 리스트뷰 삭제 모드 활성화
            }
        });

        // 추가 버튼 클릭 리스너 등록
        buttonAddStory = (ImageView) findViewById(R.id.storylist_button_add_story);
        buttonAddStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StoryListActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        // 검색창 초기화
        searchView = (EditText) findViewById(R.id.storylist_search);
        searchView.addTextChangedListener(textWatcher);

        // 검색 단어 초기화 버튼
        buttonInitSearch = (ImageView) findViewById(R.id.storylist_init_search);
        buttonInitSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setText("");
            }
        });

        // 삭제 활성화 됐을 때 나타나는 뷰들
        deleteView = (LinearLayout) findViewById(R.id.storylist_delete_view);

        // 삭제 취소 버튼 클릭 리스너 등록
        buttonDeleteCancel = (Button) findViewById(R.id.storylist_delete_cancel);
        buttonDeleteCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDeleteMode = false;
                deleteView.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }
        });

        // 삭제 확인 버튼 클릭 리스너 등록
        buttonDeleteOk = (Button) findViewById(R.id.storylist_delete_ok);
        buttonDeleteOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDeleteMode = false;
                deleteView.setVisibility(View.GONE);
                int dataSize = adapter.listData.size();
                for (int i=0; i<dataSize; i++) {
                    // 섹션 헤더일 경우 넘어감
                    if (adapter.sectionHeader.contains(i)) { continue; }

                    // 체크 되어있는 스토리들 삭제
                    StoryListData data = adapter.listData.get(i);
                    View row = getViewByPosition(i, listView);
                    CheckBox deleteBox = (CheckBox) row.findViewById(R.id.item_storylist_delete_box);
                    if (deleteBox.isChecked()) {
                        dbAdapter.deleteRow(data.time);
                    }
                }
                // 삭제 후 리스트뷰 데이터들 재정리
                InitListDatas();
                adapter.notifyDataSetChanged();
            }
        });

        // 리스트뷰 초기화 및 어뎁터 등록
        adapter = new ListViewAdapter(getApplicationContext());
        listView = (ListView) findViewById(R.id.storylist_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 헤더를 클릭했으면 무시
                if (adapter.sectionHeader.contains(position)) { return; }
                // 삭제 활성화 됐을 때 무시
                if (isDeleteMode) { return; }

                Intent intent = new Intent(StoryListActivity.this, StoryDetailActivity.class);
                String time = adapter.listData.get(position).time;
                intent.putExtra("time", time);
                startActivity(intent);
            }
        });
        // 제일 처음 리스트뷰 데이터들 정리
        InitListDatas();

        // 처음에 키보드 숨김
        final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            }
        }, 100);
    }

    // 리스트뷰 데이터 초기화 메소드
    void InitListDatas() {
        // 기존에 데이터가 존재하면 다 삭제
        int oldSize = adapter.listData.size();
        if (oldSize > 0) {
            for (int i=0; i<oldSize; i++)
                adapter.removeItem(0);
        }

        // DB에서 정보 가져오기 & 섹션헤더 추가
        List<StoryData> storyDataArrayList = dbAdapter.getAllStoryDatas();
        calendar = Calendar.getInstance();
        int baseYear = -1, baseMonth = -1, count = 0;
        int size = storyDataArrayList.size();
        for (int index = 0; index < size; index++) {
            StoryData data = storyDataArrayList.get(index);
            String path = (data.paths.split(","))[0];
            String time = data.time;
            String title = data.title;
            String memo = data.memo;
            Log.d("StoryList memo", data.memo);

            // 시간 가져오기
            long timeLong = Long.parseLong(time);
            calendar.setTimeInMillis(timeLong);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);

            if (index == 0) {
                baseYear = year;
                baseMonth = month;
            }
            // 년, 월이 다를 경우 섹션 헤더 추가
            if ((baseYear != year || baseMonth != month)) {
                int headerIndex = adapter.listData.size() - count;
                adapter.addSectionHeaderView(headerIndex, String.format("%d년 %d월 (%d)", baseYear, baseMonth + 1, count));
                baseYear = year;
                baseMonth = month;
                count = 0;
            }
            count = count + 1;
            // 제일 마지막에 섹션 헤더 추가
            if (index == size - 1) {
                int headerIndex = (adapter.listData.size() + 1) - count;
                adapter.addSectionHeaderView(headerIndex, String.format("%d년 %d월 (%d)", baseYear, baseMonth + 1, count));
                baseYear = year;
                baseMonth = month;
                count = 0;
            }

            adapter.addItem(time, title, path, memo);
        }

        // 처음에 리스트뷰 아이템 전부 보이게 설정
        size = adapter.getCount();
        for (int index=0; index<size; index++) {
            ((StoryListData) adapter.getItem(index)).visible = true;
        }
        adapter.notifyDataSetChanged();

        // 타이틀에 스토리 개수 적용
        activityTitle.setText(String.format("스토리(%d)", storyDataArrayList.size()));

        // 검색을 위한 시간별 스토리 개수 구하기
        size = adapter.sectionHeader.size();
        storySize = new int[size];
        if (size > 0) {
            for (int i = 0; i < size - 1; i++) {
                storySize[i] = adapter.sectionHeader.get(i + 1) - adapter.sectionHeader.get(i) - 1;
            }
            storySize[size - 1] = adapter.listData.size() - (adapter.sectionHeader.get(size - 1) + 1);
        }
    }

    // 검색창에 글자 입력했을 경우 리스트뷰 필터링 해주는 리스너
    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            int size = adapter.getCount();
            int invisibleIndex = -1;
            int[] invisibleCount = new int[adapter.sectionHeader.size()];
            for (int i=0; i<size; i++) {
                // 섹션 헤더일 경우 넘어감
                if (adapter.sectionHeader.contains(i)) {
                    invisibleIndex++;
                    continue;
                }
                StoryListData data = (StoryListData) adapter.getItem(i);
                StoryListData header = ((StoryListData) adapter.getItem(adapter.sectionHeader.get(invisibleIndex)));
                String text = searchView.getText().toString();
                // 검색창에 아무것도 입력이 안돼있거나 입력된 것을 포함하는 경우 표시하고
                if (data.title.contains(text) || data.memo.contains(text) || text.equals("")) {
                    data.visible = true;
                    header.visible = true;
                // 포함하지 않으면 안보이게 설정
                } else {
                    data.visible = false;
                    invisibleCount[invisibleIndex]++;

                    if (invisibleCount[invisibleIndex] == storySize[invisibleIndex]) {
                        header.visible = false;
                    } else {
                        header.visible = true;
                    }
                }
                // 검색 결과에 따라 섹션헤더 제목 변경
                long timeLong = Long.parseLong(data.time);
                calendar.setTimeInMillis(timeLong);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                header.title = String.format("%d년 %d월 (%d)", year, month + 1, storySize[invisibleIndex] - invisibleCount[invisibleIndex]);
            }
            adapter.notifyDataSetChanged();
        }
    };

    // 해당 position의 리스트뷰 아이템을 갖고 오는 메소드
    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    // 커스텀 리스트뷰 홀더
    public class ViewHolder {
        public LinearLayout parentView;
        public ImageView photo;
        public TextView title;
        public TextView date;
        public CheckBox deleteBox;
    }

    // 커스텀 리스트뷰 어뎁터
    public class ListViewAdapter extends BaseAdapter {
        private Context context = null;
        public ArrayList<StoryListData> listData = new ArrayList<StoryListData>();
        public ArrayList<Integer> sectionHeader = new ArrayList<Integer>();
        LayoutInflater inflater;

        private static final int TYPE_ROW = 0;
        private static final int TYPE_HEADER = 1;

        public ListViewAdapter(Context context) {
            super();
            this.context = context;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        // 섹션 헤더를 추가하는 함수
        public void addSectionHeaderView(int index, String title) {
            StoryListData data = new StoryListData();
            data.title = title;
            data.path = null;
            data.time = null;
            listData.add(index, data);
            sectionHeader.add(index);
        }

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
        public int getViewTypeCount() { return 2; }

        @Override
        public int getItemViewType(int position) {
            return sectionHeader.contains(position)? TYPE_HEADER : TYPE_ROW;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            int rowType = getItemViewType(position);

            if (convertView == null) {
                holder = new ViewHolder();

                // 해당 position이 스토리일 경우
                if (rowType == TYPE_ROW) {
                    convertView = inflater.inflate(R.layout.listview_item_storylist, parent, false);

                    holder.parentView = (LinearLayout) convertView.findViewById(R.id.item_storylist_parent);
                    holder.photo = (ImageView) convertView.findViewById(R.id.item_storylist_photo);
                    holder.title = (TextView) convertView.findViewById(R.id.item_storylist_title);
                    holder.date = (TextView) convertView.findViewById(R.id.item_storylist_date);
                    holder.deleteBox = (CheckBox) convertView.findViewById(R.id.item_storylist_delete_box);
                // 해당 position이 섹션 헤더일 경우
                } else {
                    convertView = inflater.inflate(R.layout.listview_header_storylist, parent, false);

                    holder.parentView = (LinearLayout) convertView.findViewById(R.id.header_storylist_parent);
                    holder.title = (TextView) convertView.findViewById(R.id.header_storylist_date);
                }

                convertView.setTag(holder);
            } else{
                holder = (ViewHolder) convertView.getTag();
            }

            // position의 데이터 값들을 가져옴
            final StoryListData data = listData.get(position);
            // 검색 결과에 따라 (비)가시 설정
            if (data.visible) {
                holder.parentView.setVisibility(View.VISIBLE);
                holder.title.setVisibility(View.VISIBLE);
                holder.title.setText(data.title);

                if (rowType == TYPE_ROW) {
                    Picasso.with(context).load(new File(data.path)).resize(100, 100).into(holder.photo);
                    holder.photo.setVisibility(View.VISIBLE);

                    long time = Long.parseLong(data.time);
                    Date date = new Date(time);
                    SimpleDateFormat transFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");
                    String timeString = transFormat.format(date);
                    holder.date.setText(timeString);
                    holder.date.setVisibility(View.VISIBLE);

                    if (isDeleteMode) {
                        holder.deleteBox.setVisibility(View.VISIBLE);
                    } else {
                        holder.deleteBox.setVisibility(View.INVISIBLE);
                        holder.deleteBox.setChecked(false);
                    }
                }
            } else {
                holder.parentView.setVisibility(View.GONE);
                holder.title.setVisibility(View.GONE);

                if (rowType == TYPE_ROW) {
                    holder.photo.setVisibility(View.GONE);
                    holder.date.setVisibility(View.GONE);
                    holder.deleteBox.setVisibility(View.GONE);
                    if (!isDeleteMode) {
                        holder.deleteBox.setChecked(false);
                    }
                }
            }

            return convertView;
        }

        // 스토리 추가하는 함수
        public void addItem(String time, String title, String path, String memo) {
            StoryListData addInfo = new StoryListData();

            addInfo.time = time;
            addInfo.title = title;
            addInfo.path = path;
            addInfo.memo = memo;
            addInfo.visible = true;

            listData.add(addInfo);
        }

        // 스토리 삭제하는 함수
        public void removeItem(int position) {
            listData.remove(position);
        }
    }
}
