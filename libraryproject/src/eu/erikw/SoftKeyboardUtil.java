package eu.erikw;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

public  final class SoftKeyboardUtil
{
	public static final String TAG = "SoftKeyboardUtil";

	public void observeSoftKeyBoard(Activity activity, final OnSoftKeyBoardHideListener listener)
	{

		final View decorView = activity.getWindow().getDecorView();
		decorView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener()
				{
					@Override
					public void onGlobalLayout()
					{

						Rect rect = new Rect();
						decorView.getWindowVisibleDisplayFrame(rect);
						int displayHight = rect.bottom - rect.top;
						int hight = decorView.getHeight();
						boolean hide = (double) displayHight / hight > 0.8;
						if (Log.isLoggable(TAG, Log.DEBUG))
						{
							Log.d(TAG, "DecorView display hight = " + displayHight);
							Log.d(TAG, "DecorView hight = " + hight);
							Log.d(TAG, "softkeyboard visible = " + !hide);
						}

						listener.onSoftKeyBoardVisible(!hide);

					}
				});
	}
	
	public interface OnSoftKeyBoardHideListener
	{
		void onSoftKeyBoardVisible(boolean visible);
	}

}
