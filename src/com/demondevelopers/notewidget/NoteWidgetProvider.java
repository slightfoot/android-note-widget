package com.demondevelopers.notewidget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Selection;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;


public class NoteWidgetProvider extends AppWidgetProvider
{
	private Context           mViewContext;
	private SparseArray<View> mWidgetViews = new SparseArray<View>();
	private SparseIntArray    mWidths  = new SparseIntArray();
	private SparseIntArray    mHeights = new SparseIntArray();
	
	
	@Override
	public void onEnabled(Context context)
	{
		super.onEnabled(context);
		mViewContext = getViewContext(context);
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, 
		int appWidgetId, Bundle newOptions)
	{
		updateAppWidgetSize(context, appWidgetManager, appWidgetId);
		onUpdate(context, appWidgetManager, new int[] { appWidgetId });
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		for(int i = 0; i < appWidgetIds.length; i++){
			updateWidget(context, appWidgetIds[i]);
		}
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds)
	{
		for(int i = 0; i < appWidgetIds.length; i++){
			NoteStorage.deleteNote(context, appWidgetIds[i]);
		}
	}
	
	@Override
	public void onDisabled(Context context)
	{
		super.onDisabled(context);
	}
	
	public static void forceWidgetUpdate(Context context, int appWidgetId)
	{
		context.sendBroadcast(new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
			.setPackage(context.getPackageName())
			.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {
				appWidgetId
			}));
	}
	
	@SuppressLint("NewApi")
	private void updateAppWidgetSize(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
	{
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		int width, height;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
			Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
			width  = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
			height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
		}
		else{
			AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
			width  = info.minWidth;
			height = info.minHeight;
		}
		mWidths.put(appWidgetId, (int)TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP, width, metrics));
		mHeights.put(appWidgetId, (int)TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP, height, metrics));
	}
	
	@SuppressLint("NewApi")
	public void updateWidget(Context context, int appWidgetId)
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		
		int width  = mWidths.get(appWidgetId);
		int height = mHeights.get(appWidgetId);
		if(width == 0 || height == 0){
			updateAppWidgetSize(context, appWidgetManager, appWidgetId);
			width  = mWidths.get(appWidgetId);
			height = mHeights.get(appWidgetId);
		}
		
		EditText view = (EditText)mWidgetViews.get(appWidgetId);
		if(view == null){
			view = createNoteView(context);
			mWidgetViews.put(appWidgetId, view);
		}
		view.setLayoutParams(new LayoutParams(width, height));
		
		String text = NoteStorage.getNote(context, appWidgetId);
		applyNoteViewAttrs(context, view, text);
		view.setTextColor(Color.WHITE);
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_note);
		views.setImageViewBitmap(R.id.appwidget_note, getViewImage(view));
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
			views.setContentDescription(R.id.appwidget_note, text);
		}
		
		Intent configIntent =  new Intent(context, ConfigActivity.class)
			.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		
		views.setOnClickPendingIntent(R.id.appwidget_note, PendingIntent
			.getActivity(context, appWidgetId, configIntent, 0));
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
	
	private Context getViewContext(Context context)
	{
		if(mViewContext == null){
			mViewContext = new ContextThemeWrapper(context, getApplicationTheme(context));
		}
		return mViewContext;
	}
	
	private static int getApplicationTheme(Context context)
	{
		try{
			ApplicationInfo appInfo = context.getPackageManager()
				.getApplicationInfo(context.getPackageName(), 0);
			return appInfo.theme;
		}
		catch(NameNotFoundException e){
			return android.R.style.Theme_Light;
		}
	}
	
	@SuppressLint("InflateParams")
	private EditText createNoteView(Context context)
	{
		EditText view = (EditText)LayoutInflater.from(getViewContext(context))
			.inflate(R.layout.note_view, null, false);
		view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
		view.setDrawingCacheEnabled(true);
		return view;
	}
	
	@SuppressLint("InlinedApi")
	public static TextView applyNoteViewAttrs(Context context, EditText note, CharSequence text)
	{
		TextPaint paint = note.getPaint();
		paint.setFlags(TextPaint.LINEAR_TEXT_FLAG | TextPaint.ANTI_ALIAS_FLAG | TextPaint.SUBPIXEL_TEXT_FLAG);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			paint.setFlags(paint.getFlags() | TextPaint.HINTING_ON);
		}
		note.setTypeface(Typeface.createFromAsset(context.getAssets(), "comic.ttf"));
		note.setText(text);
		Selection.removeSelection(note.getText());
		return note;
	}
	
	public Bitmap getViewImage(View view)
	{
		LayoutParams lp = view.getLayoutParams();
		view.measure(
			MeasureSpec.makeMeasureSpec(lp.width,  MeasureSpec.EXACTLY),
			MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY));
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		view.buildDrawingCache();
		return view.getDrawingCache();
	}
}
