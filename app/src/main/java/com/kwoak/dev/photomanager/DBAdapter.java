package com.kwoak.dev.photomanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by HEEJU on 2016-08-30.
 */
public class DBAdapter {
    private static final String DATABASE_NAME = "photo_story.db";
    private static final String DATABASE_TABLE = "story";
    private static final int DATABASE_VERSION = 1;
    private final Context context;

    private static DatabaseHelper dbHelper;
    private static SQLiteDatabase db;

    //테이블 생성
    private static final String DATABASE_CREATE =
            "CREATE TABLE " + DATABASE_TABLE + " " +
                    "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Time TEXT, PhotoPaths TEXT,  Title TEXT, Memo TEXT)";

    //테이블 드랍
    private static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS " + DATABASE_NAME;

    public static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //데이터베이스 최초 생성될때 실행 디비가 생성될때 실행된다
            Log.d("NormalDBAdapter : ", "onCreate DATABSE_CREATE");
            db.execSQL(DATABASE_CREATE);
        }

        /**
         *
         * @param db         The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        @Override
        //데이터베이스 업그레이드가 필요할때
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_TABLE);
        }
    }

    // 열기
    public void open() throws SQLException {

        dbHelper = new DatabaseHelper(context);

        //DB가 없다면 onCreate가 호출 후 생성, version이 바뀌었다면 onUpgrade 메소드 호출
        db = dbHelper.getWritableDatabase();

        // 권한부여 읽고 쓰기를 위해
        db = dbHelper.getWritableDatabase();
        Log.d("DBAdapter : ","open");
    }

    public DBAdapter(Context context) {
        this.context = context;
    }

    // 닫기
    public void close() {
        dbHelper.close();
    }

    // 넣기
    public long insert(StoryData data) {
        ContentValues insertValues = new ContentValues();
        insertValues.put("PhotoPaths", data.paths);
        insertValues.put("Time", data.time);
        insertValues.put("Title", data.title);
        insertValues.put("Memo", data.memo);
        Log.d("DBAdapter : ", "insert suc");

        return db.insert(DATABASE_TABLE, null, insertValues);
    }

    // 업데이트
    public long update(StoryData data, String time) {
        ContentValues updateValues = new ContentValues();
        updateValues.put("PhotoPaths", data.paths);
        updateValues.put("Time", data.time);
        updateValues.put("Title", data.title);
        updateValues.put("Memo", data.memo);
        Log.d("DBAdapter : ", "update suc");

        return db.update(DATABASE_TABLE, updateValues, "Time=" + time, null);
    }

    // 한 개 삭제
    public boolean deleteRow(String time) {
        String whereClause = "Time" + "=?";
        String[] whereArgs = new String[] { time };
        int deletedNum = db.delete(DATABASE_TABLE, whereClause, whereArgs);
        return deletedNum > 0;
//        return db.delete(DATABASE_TABLE, "Time = ", time, null) > 0;
    }

    // 전부 삭제
    public boolean deleteAll() {
        return db.delete(DATABASE_TABLE, null, null) > 0;
    }

    public Cursor AllRows() {
        return db.query(DATABASE_TABLE, null, null, null, null, null, null);
    }

    // 해당 row의 정보 가져오기
    public StoryData getStoryData(String time) {
        // Select Row Query
        String selectQuery = "select * from " + DATABASE_TABLE + " WHERE Time = " + time;

        SQLiteDatabase tempDB = dbHelper.getWritableDatabase();
        Cursor cursor = tempDB.rawQuery(selectQuery, null);
        cursor.moveToFirst();

        StoryData data = new StoryData();
        data.time = cursor.getString(1);
        data.paths = cursor.getString(2);
        data.title = cursor.getString(3);
        data.memo = cursor.getString(4);

        return data;
    }

    // 모든 StoryData 정보 가져오기
    public List<StoryData> getAllStoryDatas() {
        List<StoryData> storyDataList = new ArrayList<StoryData>();
        // Select All Query
        String selectQuery = "select * from " + DATABASE_TABLE + " ORDER BY Time DESC";

        SQLiteDatabase tempDB = dbHelper.getWritableDatabase();
        Cursor cursor = tempDB.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                StoryData data = new StoryData();
                data.time = cursor.getString(1);
                data.paths = cursor.getString(2);
                data.title = cursor.getString(3);
                data.memo = cursor.getString(4);
                // Adding StoryData to list
                storyDataList.add(data);
            } while (cursor.moveToNext());
        }

        // return contact list
        return storyDataList;
    }
}
