package com.demondevelopers.notewidget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RemoteViews;


/*
 * Note: Intent sourceBounds for widget clicks set as of Android 2.1_r1
 * https://github.com/android/platform_frameworks_base/commit/7597065d6b0877ffc460b443fdb1595965ccd7b2 
 * 
 */
public class ConfigActivity extends Activity
{
	private static final String TAG = ConfigActivity.class.getSimpleName();
	
	private int      mAppWidgetId;
	private EditText mNoteText;
	
	private int mTransitionTime;
	private TransitionDrawable mWindowDrawable;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(0, 0);
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
		
		mTransitionTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
		
		mNoteText = (EditText)findViewById(R.id.note_text);
		
		if(savedInstanceState == null){
			NoteWidgetProvider.applyNoteViewAttrs(this, mNoteText, 
				NoteStorage.getNote(this, mAppWidgetId));
		}
		
		if(!animateBoundsIfPossible()){
			//mNoteText.setVisibility(View.VISIBLE);
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private boolean animateBoundsIfPossible()
	{
		Rect sourceBounds = getIntent().getSourceBounds();
		if(sourceBounds != null && !sourceBounds.isEmpty()){
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
				AnimateChangeBounds changeBounds = AnimateChangeBounds.startFromScreen(mNoteText, sourceBounds);
				changeBounds.setAnimatorListener(new AnimatorListenerAdapter()
				{
					@Override
					public void onAnimationStart(Animator animation)
					{
						blankAppWidget(mAppWidgetId);
					}
				});
				mWindowDrawable = new TransitionDrawable(new Drawable[] {
					new ColorDrawable(Color.TRANSPARENT),
					new ColorDrawable(getResources().getColor(R.color.note_list_bg))
				});
				getWindow().setBackgroundDrawable(mWindowDrawable);
				mWindowDrawable.setCrossFadeEnabled(true);
				mWindowDrawable.startTransition(mTransitionTime);
				return true;
			}
		}
		return false;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private boolean animateClosed()
	{
		Rect sourceBounds = getIntent().getSourceBounds();
		if(sourceBounds != null && !sourceBounds.isEmpty()){
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
				AnimateChangeBounds changeBounds = AnimateChangeBounds.endAtScreen(mNoteText, sourceBounds);
				changeBounds.setAnimatorListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation)
					{
						NoteWidgetProvider.forceWidgetUpdate(ConfigActivity.this, mAppWidgetId);
						setResultAndFinish();
					}
				});
				if(mWindowDrawable != null){
					mWindowDrawable.reverseTransition(mTransitionTime);
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void finish()
	{
		NoteStorage.putNote(this, mAppWidgetId, mNoteText.getText().toString());
		if(!animateClosed()){
			NoteWidgetProvider.forceWidgetUpdate(this, mAppWidgetId);
			setResultAndFinish();
		}
	}
	
	private void setResultAndFinish()
	{
		setResult(RESULT_OK, new Intent().putExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID, 
			mAppWidgetId));
		super.finish();
		overridePendingTransition(0, 0);
	}
	
	private void blankAppWidget(int appWidgetId)
	{
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.appwidget_note);
		views.setImageViewBitmap(R.id.appwidget_note, null);
		AppWidgetManager.getInstance(this).updateAppWidget(appWidgetId, views);
	}
	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class AnimateChangeBounds
	{
		private final ViewGroup  mParent;
		private final View       mView;
		private final Mode       mMode;
		
		private Rect             mStart;
		private Rect             mEnd;
		private ObjectAnimator   mObjectAnimator;
		
		private AnimatorListener mAnimatorListener;
		private long             mDuration;
		
		private boolean mCancelled = false;
		
		
		public static AnimateChangeBounds startFromScreen(View view, Rect start)
		{
			return new AnimateChangeBounds(view, new Rect(start), Mode.START_ABS);
		}
		
		public static AnimateChangeBounds endAtScreen(View view, Rect end)
		{
			return new AnimateChangeBounds(view, new Rect(end), Mode.END_ABS);
		}
		
		private AnimateChangeBounds(View view, Rect rect, Mode mode)
		{
			mParent = (ViewGroup)view.getParent();
			mView = view;
			mMode = mode;
			
			switch(mode){
				case START_ABS:
				case START_REL: mStart = rect; break;
				case END_ABS:
				case END_REL:   mEnd   = rect; break;
			}
			
			mDuration = view.getContext().getResources().getInteger(android.R.integer.config_longAnimTime);
			
			mParent.getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
		}
		
		public void setAnimatorListener(AnimatorListener listener)
		{
			mAnimatorListener = listener;
			if(mObjectAnimator != null){
				mObjectAnimator.removeAllListeners();
				mObjectAnimator.addListener(listener);
			}
		}
		
		public void setDuration(long duration)
		{
			mDuration = duration;
			if(mObjectAnimator != null){
				mObjectAnimator.setDuration(duration);
			}
		}
		
		public void cancel()
		{
			mCancelled = true;
			if(mObjectAnimator != null){
				mObjectAnimator.cancel();
			}
		}
		
		private ViewTreeObserver.OnPreDrawListener mPreDrawListener = new ViewTreeObserver.OnPreDrawListener()
		{
			@Override
			public boolean onPreDraw()
			{
				mParent.getViewTreeObserver().removeOnPreDrawListener(mPreDrawListener);
				
				// Convert given rect coordinates to relative
				if(mMode == Mode.START_ABS || mMode == Mode.END_ABS){
					final int[] location = new int[2]; 
					ViewGroup parent = (ViewGroup)mView.getParent();
					parent.getLocationOnScreen(location);
					
					if(mMode == Mode.START_ABS){
						mStart.offset(-location[0], -location[1]);
					}else{
						mEnd.offset(-location[0], -location[1]);
					}
				}
				
				Rect viewRect = new Rect(
					mView.getLeft(),  mView.getTop(),
					mView.getRight(), mView.getBottom()
				);
				
				switch(mMode){
					case START_ABS:
					case START_REL: mEnd   = viewRect; break;
					case END_ABS:
					case END_REL:   mStart = viewRect; break;
				}
				
				if(mCancelled){
					return true;
				}
				
				mObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(mView, 
					PropertyValuesHolder.ofInt("left",   mStart.left,   mEnd.left),
					PropertyValuesHolder.ofInt("top",    mStart.top,    mEnd.top),
					PropertyValuesHolder.ofInt("right",  mStart.right,  mEnd.right),
					PropertyValuesHolder.ofInt("bottom", mStart.bottom, mEnd.bottom));
				
				setAnimatorListener(mAnimatorListener);
				
				mObjectAnimator.setDuration(mDuration);
				mObjectAnimator.start();
				
				return false;
			}
		};
		
		private enum Mode {
			START_ABS, END_ABS, START_REL, END_REL;
		}
	}
}
