package com.demondevelopers.notewidget;

import android.os.Bundle;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;
import android.view.View;


public class ConfigActivity extends Activity
{
	private static final String TAG = ConfigActivity.class.getSimpleName();
	
	private int      mAppWidgetId;
	private EditText mNoteText;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mAppWidgetId = getIntent().getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID);
		
		if(mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
			Log.e(TAG, "Bad AppWidgetId");
			finish();
			return;
		}
		
		setContentView(R.layout.appwidget_config);
		
		setResult(RESULT_CANCELED);
		
		mNoteText = (EditText)findViewById(R.id.note_text);
		if(savedInstanceState == null){
			mNoteText.setText(NoteStorage.getNote(this, mAppWidgetId));
		}
		
		findViewById(R.id.note_update).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Context context = getApplicationContext();
				
				NoteStorage.putNote(context, mAppWidgetId, mNoteText.getText().toString());
				
				NoteWidgetProvider.forceWidgetUpdate(context, mAppWidgetId);
				
				setResult(RESULT_OK, new Intent().putExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID, 
					mAppWidgetId));
				
				finish();
			}
		});
	}
}
