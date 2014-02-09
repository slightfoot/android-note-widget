package com.demondevelopers.notewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.GridView;
import android.widget.TextView;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class MainActivity extends FragmentActivity implements LoaderCallbacks<Cursor>
{
	private NotesAdapter mAdapter;
	private GridView     mGridView;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().setBackgroundDrawableResource(R.color.note_list_bg);
		mAdapter  = new NotesAdapter(this);
		mGridView = (GridView)findViewById(R.id.note_grid);
		mGridView.setAdapter(mAdapter);
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		return NoteStorage.getAllNotes(this);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data)
	{
		if(data != null){
			mAdapter.changeCursor(data);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		mAdapter.changeCursor(null);
	}
	
	
	private static class NotesAdapter extends CursorAdapter implements View.OnClickListener
	{
		private static final String TAG = MainActivity.NotesAdapter.class.getSimpleName();
		
		public  final LayoutInflater mInflater;
		private final int mPadding;
		
		
		public NotesAdapter(Context context)
		{
			super(context, null, true);
			mInflater = LayoutInflater.from(context);
			mPadding = context.getResources().getDimensionPixelSize(R.dimen.appwidget_note_padding);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			FrameLayout layout = new FrameLayout(context);
			layout.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
			layout.setPadding(mPadding, mPadding, mPadding, mPadding / 2);
			mInflater.inflate(R.layout.note_view, layout, true);
			return layout;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			TextView note = (TextView)view.findViewById(R.id.note);
			NoteWidgetProvider.applyNoteViewAttrs(context, note, 
				cursor.getString(cursor.getColumnIndex(NoteStorage.Columns.NOTE)));
			note.setTag(R.id.app_widget_id, 
				cursor.getInt(cursor.getColumnIndex(NoteStorage.Columns._ID)));
			note.setOnClickListener(this);
			note.setTextColor(Color.WHITE);
		}
		
		@Override
		public void onClick(View v)
		{
			Context context = v.getContext();
			Intent intent = new Intent(context, ConfigActivity.class)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
					(Integer)v.getTag(R.id.app_widget_id));
			context.startActivity(intent);
		}
		
		@Override
		protected void onContentChanged()
		{
			super.onContentChanged();
			Log.e(TAG, "onContentChanged");
		}
	}
}
