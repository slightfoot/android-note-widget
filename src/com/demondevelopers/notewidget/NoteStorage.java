package com.demondevelopers.notewidget;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;


public final class NoteStorage
{
	public static CursorLoader getAllNotes(Context context)
	{
		return new CursorLoader(context, Provider.allNotesUri(), 
			null, null, null, null);
	}
	
	public static Uri putNote(Context context, int appWidgetId, String text)
	{
		Cursor cursor = context.getContentResolver().query(
			Provider.allNotesUri(), new String[] { "COUNT(*)" }, Columns._ID + " = ?", 
				new String[] { String.valueOf(appWidgetId) }, null);
		
		if(cursor != null){
			if(cursor.moveToFirst()){
				if(cursor.getLong(0) > 0){
					updateNote(context, appWidgetId, text);
				}
				else{
					insertNote(context, appWidgetId, text);
				}
			}
			cursor.close();
			
			return Provider.noteUri(appWidgetId);
		}
		
		return null;
	}
	
	public static String getNote(Context context, int appWidgetId)
	{
		String text = null;
		
		Cursor cursor = context.getContentResolver().query(
			Provider.noteUri(appWidgetId), null, null, null, null);
		if(cursor != null){
			if(cursor.moveToFirst()){
				text = cursor.getString(cursor.getColumnIndex(Columns.NOTE));
			}
			cursor.close();
		}
		
		return text;
	}
	
	public static Uri insertNote(Context context, int appWidgetId, String text)
	{
		ContentValues values = new ContentValues(2);
		values.put(Columns._ID, appWidgetId);
		values.put(Columns.NOTE, text);
		
		return context.getContentResolver()
			.insert(Provider.allNotesUri(), values);
	}
	
	public static boolean updateNote(Context context, int appWidgetId, String text)
	{
		ContentValues values = new ContentValues(1);
		values.put(Columns.NOTE, text);
		
		return (context.getContentResolver()
			.update(Provider.noteUri(appWidgetId), 
				values, null, null) == 1);
	}
	
	public static boolean deleteNote(Context context, int appWidgetId)
	{
		return (context.getContentResolver().delete(
			Provider.noteUri(appWidgetId), null, null) > 0);
	}
	
	
	public static interface Columns extends BaseColumns
	{
		public static final String NOTE = "note";
	}
	
	public static class Provider extends ContentProvider
	{
		private static final String AUTHORITY   = "com.demondevelopers.notewidget.store";
		private static final Uri    CONTENT_URI = new Uri.Builder()
			.scheme(ContentResolver.SCHEME_CONTENT)
			.authority(AUTHORITY)
			.build();
		
		private Store mStore;
		
		private static final int    CODE_NOTES     = 0;
		private static final int    CODE_NOTES_ID  = 1;
		
		private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		static {
			sUriMatcher.addURI(AUTHORITY, "notes",  CODE_NOTES);
			sUriMatcher.addURI(AUTHORITY, "notes/#", CODE_NOTES_ID);
		}
		
		
		public static Uri allNotesUri()
		{
			return CONTENT_URI.buildUpon()
				.appendPath("notes")
				.build();
		}
		
		public static Uri noteUri(long id)
		{
			return CONTENT_URI.buildUpon()
				.appendPath("notes")
				.appendPath(String.valueOf(id))
				.build();
		}
		
		public Provider()
		{
			//
		}
		
		@Override
		public boolean onCreate()
		{
			mStore = new Store(getContext());
			return true;
		}
		
		@Override
		protected void finalize() throws Throwable
		{
			mStore.close();
			super.finalize();
		}
		
		@Override
		public String getType(Uri uri)
		{
			switch(sUriMatcher.match(uri)){
				case CODE_NOTES:
					return "vnd.android.cursor.dir/notes";
					
				case CODE_NOTES_ID: 
					return "vnd.android.cursor.item/note";
					
				default:
					return null;
			}
		}
		
		@Override
		public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
		{
			int code = sUriMatcher.match(uri);
			if(code == -1){
				return null;
			}
			
			SQLiteDatabase db = mStore.getReadableDatabase();
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			
			switch(code){
				case CODE_NOTES:
					queryBuilder.setTables(Store.TABLE);
					break;
					
				case CODE_NOTES_ID:
					queryBuilder.setTables(Store.TABLE);
					queryBuilder.appendWhere(Columns._ID + " = " + uri.getLastPathSegment());
					break;
			}
			
			Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			
			return cursor;
		}
		
		@Override
		public Uri insert(Uri uri, ContentValues values)
		{
			if(sUriMatcher.match(uri) != CODE_NOTES){
				return null;
			}
			
			SQLiteDatabase db = mStore.getWritableDatabase();
			
			long id = db.insert(Store.TABLE, null, values);
			if(id != -1){
				getContext().getContentResolver().notifyChange(uri, null);
				return noteUri(id);
			}
			
			return null;
		}
		
		@Override
		public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
		{
			if(sUriMatcher.match(uri) != CODE_NOTES_ID){
				return -1;
			}
			
			SQLiteDatabase db = mStore.getWritableDatabase();
			
			int updated = db.update(Store.TABLE, values, Columns._ID + " = ?", 
				new String[] { uri.getLastPathSegment() });
			if(updated > 0){
				getContext().getContentResolver().notifyChange(uri, null);
			}
			
			return updated;
		}
	
		@Override
		public int delete(Uri uri, String selection, String[] selectionArgs)
		{
			if(sUriMatcher.match(uri) != CODE_NOTES_ID){
				return -1;
			}
			
			SQLiteDatabase db = mStore.getWritableDatabase();
			
			int deleted = db.delete(Store.TABLE, Columns._ID + " = ?", 
				new String[] { uri.getLastPathSegment() });
			if(deleted > 0){
				getContext().getContentResolver().notifyChange(uri, null);
			}
			
			return deleted;
		}
		
		
		private class Store extends SQLiteOpenHelper
		{
			private static final String DATABASE_FILE   = "notes.db";
			private static final int    DATABASE_VER    = 1;
			
			public static final String  TABLE = "notes";
			
			
			public Store(Context context)
			{
				super(context, DATABASE_FILE, null, DATABASE_VER);
			}
			
			@Override
			public void onCreate(SQLiteDatabase db)
			{
				db.execSQL(
					"CREATE TABLE " + TABLE + "(" + 
						Columns._ID  + " INTEGER PRIMARY KEY," +
						Columns.NOTE + " TEXT" +
					");");
			}
			
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
			{
				db.execSQL("DROP TABLE IF EXISTS " + TABLE); // FIXME
				onCreate(db);
			}
		}
	}
}
