package eu.erikw;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.*;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.TextView.OnEditorActionListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import eu.erikw.SoftKeyboardUtil;
import eu.erikw.SoftKeyboardUtil.OnSoftKeyBoardHideListener;


public class PullToAddListView extends ListView {

	private static final String TAG = "PullToAddListView";
    private static final float PULL_RESISTANCE                 = 1.7f;
    private static final int   BOUNCE_ANIMATION_DURATION       = 700;
    private static final int   BOUNCE_ANIMATION_DELAY          = 100;
    private static final float BOUNCE_OVERSHOOT_TENSION        = 1.4f;
    private static final int   ROTATE_ARROW_ANIMATION_DURATION = 250;

    private static final int   MESSAGE_RESET_LIST_STATE      = 500;
    private static final int   CHECK_LIST_STATE_TIME      = 800; // unit: millisecond
    
    public static enum State{
        PULL_TO_ADD_ROW,
        RELEASE_TO_ADD_ROW,
        ADDING
    }

    /**
     * Interface to implement when you want to get notified of 'input text finish'
     * events.
     * Call setOnAddingFinishListener(..) to activate an OnAddingFinishListener.
     */
    public interface OnAddingFinishListener{

        /**
         * Method to be called when a Adding is Finished
         */
        public void onAddingFinish(String newEditText);
    }
    
    private static int measuredHeaderHeight;

    private boolean scrollbarEnabled;
    private boolean bounceBackHeader;
    private boolean lockScrollWhileAdding;
    private boolean showLastUpdatedText;
    private String  pullToAddText;
    private String  releaseToAddText;
    private String  addingText;
    private String  lastUpdatedText;
    private SimpleDateFormat lastUpdatedDateFormat = new SimpleDateFormat("dd/MM HH:mm");

    private float                   previousY;
    private int                     headerPadding;
    private boolean                 hasResetHeader;
    private long                    lastUpdated = -1;
    public State                   state;
    private LinearLayout            headerContainer;
    private RelativeLayout          header;
    private RotateAnimation         flipAnimation;
    private RotateAnimation         reverseFlipAnimation;
    private ImageView               image;
    private ProgressBar             spinner;
    private TextView                text;
    public 	 EditText                addItemEditText;
    private TextView                lastUpdatedTextView;
    private OnItemClickListener     onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnAddingFinishListener       onAddingFinishListener;

    private float mScrollStartY;
    private final int IDLE_DISTANCE = 5;
    public String newEditTextString;
    private boolean isSoftKeyboardShown = false;
    SoftKeyboardUtil softKeyboardUtil = new SoftKeyboardUtil();
    
	Handler resetListStateHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{

			switch (msg.what)
			{
			case MESSAGE_RESET_LIST_STATE:
				if (!isSoftKeyboardShown && getState() == state.ADDING)
				{
					getAddItemEditText().setText("");
					onAddingComplete();
				}

				Log.d(TAG, "resetListStateHandler onAddingComplete()");
				break;

			}
			super.handleMessage(msg);
		}
	};

    public PullToAddListView(Context context){
        super(context);
        init();
    }

    public PullToAddListView(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public PullToAddListView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener){
        this.onItemLongClickListener = onItemLongClickListener;
    }

    /**
     * Activate an setOnAddingFinishListener to get notified on 'input text finished'
     * events.
     *
     * @param OnAddingFinishListener The OnAddingFinishListener to get notified
     */
    public void setOnAddingFinishListener(OnAddingFinishListener onAddingFinishListener){
        this.onAddingFinishListener = onAddingFinishListener;
    }
    
    /**
     * @return If the list is in 'Adding' state
     */
    public boolean isAdding(){
        return state == State.ADDING;
    }

    /**
     * Default is false. When lockScrollWhileAdding is set to true, the list
     * cannot scroll when in 'Adding' mode. It's 'locked' on Adding.
     *
     * @param lockScrollWhileAdding
     */
    public void setLockScrollWhileAdding(boolean lockScrollWhileAddhing){
        this.lockScrollWhileAdding = lockScrollWhileAddhing;
    }

    /**
     * Default is false. Show the last-updated date/time in the 'Pull to Add'
     * header. See 'setLastUpdatedDateFormat' to set the date/time formatting.
     *
     * @param showLastUpdatedText
     */
    public void setShowLastUpdatedText(boolean showLastUpdatedText){
        this.showLastUpdatedText = showLastUpdatedText;
        if(!showLastUpdatedText) lastUpdatedTextView.setVisibility(View.GONE);
    }

    /**
     * Default: "dd/MM HH:mm". Set the format in which the last-updated
     * date/time is shown. Meaningless if 'showLastUpdatedText == false (default)'.
     * See 'setShowLastUpdatedText'.
     *
     * @param lastUpdatedDateFormat
     */
    public void setLastUpdatedDateFormat(SimpleDateFormat lastUpdatedDateFormat){
        this.lastUpdatedDateFormat = lastUpdatedDateFormat;
    }

    /**
     * Explicitly set the state to adding. This
     * is useful when you want to show the spinner and 'Refreshing' text when
     * the refresh was not triggered by 'pull to refresh', for example on start.
     */
    public void setAdding(){
        state = State.ADDING;
        scrollTo(0, 0);
        setUiAdding();
        setHeaderPadding(0);

    }

    /**
     * Set the state back to 'pull to add'. Call this method when adding
     * the data is finished.
     */
    public void onAddingComplete(){
        state = State.PULL_TO_ADD_ROW;
        resetHeader();
        lastUpdated = System.currentTimeMillis();       
		Thread closeSoftwareKeyboardThread = new Thread()
		{
			@Override
			public void run()
			{

				if (((Activity) getContext()).getCurrentFocus() != null
						&& ((Activity) getContext()).getCurrentFocus().getWindowToken() != null)
				{
					InputMethodManager imm = (InputMethodManager) ((Activity) getContext()).getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(((Activity) getContext()).getCurrentFocus()
							.getWindowToken(), 0);
				}

			}
		};
		closeSoftwareKeyboardThread.start(); 
    }

    /**
     * Change the label text on state 'Pull to add'
     *
     * @param pullToAddText Text
     */
    public void setTextPullToAdd(String pullToAddText){
        this.pullToAddText = pullToAddText;
        if(state == State.PULL_TO_ADD_ROW){
            text.setText(pullToAddText);
        }
    }

    /**
     * Change the label text on state 'Release to Refresh'
     *
     * @param releaseToRefreshText Text
     */
    public void setTextReleaseToRefresh(String releaseToRefreshText){
        this.releaseToAddText = releaseToRefreshText;
        if(state == State.RELEASE_TO_ADD_ROW){
            text.setText(releaseToRefreshText);
        }
    }

    /**
     * Change the label text on state 'Refreshing'
     *
     * @param refreshingText Text
     */
    public void setTextRefreshing(String refreshingText){
        this.addingText = refreshingText;
        if(state == State.ADDING){
            text.setText(refreshingText);
        }
    }

    private void init(){
    	Effects.getInstance().init(getContext());
    	
    	softKeyboardUtil.observeSoftKeyBoard((Activity)getContext(), new OnSoftKeyBoardHideListener()
    	{

    		@Override
    		public void onSoftKeyBoardVisible(boolean visible)
    		{

    			isSoftKeyboardShown = visible;
    			Log.d(TAG, "softKeyboardUtil visible:" + visible);
    			if (!visible && getState() == state.ADDING)
    			{

    				try
    				{
    					Thread thread = new Thread()
    					{
    						@Override
    						public void run()
    						{

    							Message msg = new Message();
    							msg.what = MESSAGE_RESET_LIST_STATE;
    							resetListStateHandler.removeMessages(MESSAGE_RESET_LIST_STATE);
    							resetListStateHandler.sendMessageDelayed(msg, CHECK_LIST_STATE_TIME);

    						}
    					};

    					thread.start();

    				}
    				catch (Exception e)
    				{
    					e.printStackTrace();
    				}

    				Log.d(TAG, "softKeyboardUtil send new message ");
    			}
    		}

    	});
    	
    	

    	
    	setScrollContainer(false); // add for slow open / close input method
    	
    	
        setVerticalFadingEdgeEnabled(false);

        headerContainer = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.ptr_header, null);
        header = (RelativeLayout) headerContainer.findViewById(R.id.ptr_id_header);
        
        addItemEditText = (EditText) header.findViewById(R.id.ptr_id_edittext);
        text = (TextView) header.findViewById(R.id.ptr_id_text);
        lastUpdatedTextView = (TextView) header.findViewById(R.id.ptr_id_last_updated);
        image = (ImageView) header.findViewById(R.id.ptr_id_image);
//        spinner = (ProgressBar) header.findViewById(R.id.ptr_id_spinner);

        pullToAddText = getContext().getString(R.string.ptr_pull_to_refresh);
        releaseToAddText = getContext().getString(R.string.ptr_release_to_refresh);
        addingText = getContext().getString(R.string.ptr_adding);
        lastUpdatedText = getContext().getString(R.string.ptr_last_updated);

        flipAnimation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        flipAnimation.setInterpolator(new LinearInterpolator());
        flipAnimation.setDuration(ROTATE_ARROW_ANIMATION_DURATION);
        flipAnimation.setFillAfter(true);

        reverseFlipAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        reverseFlipAnimation.setInterpolator(new LinearInterpolator());
        reverseFlipAnimation.setDuration(ROTATE_ARROW_ANIMATION_DURATION);
        reverseFlipAnimation.setFillAfter(true);

        addHeaderView(headerContainer);
        setState(State.PULL_TO_ADD_ROW);
        scrollbarEnabled = isVerticalScrollBarEnabled();

        ViewTreeObserver vto = header.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new PTROnGlobalLayoutListener());

        super.setOnItemClickListener(new PTROnItemClickListener());
        super.setOnItemLongClickListener(new PTROnItemLongClickListener());

    }

    private void setHeaderPadding(int padding){
        headerPadding = padding;

        MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) header.getLayoutParams();
        mlp.setMargins(0, Math.round(padding), 0, 0);
        header.setLayoutParams(mlp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(lockScrollWhileAdding
                && (state == State.ADDING || getAnimation() != null && !getAnimation().hasEnded())){
            return true;
        }

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(getFirstVisiblePosition() == 0){
                	previousY = event.getY();
                }
                else {
                	previousY = -1;
                }
                
                // Remember where have we started
                mScrollStartY = event.getY();
                
                break;

            case MotionEvent.ACTION_UP:
                if(previousY != -1 && (state == State.RELEASE_TO_ADD_ROW || getFirstVisiblePosition() == 0)){
                    switch(state){
                        case RELEASE_TO_ADD_ROW:
                            setState(State.ADDING);
                            bounceBackHeader();
                            Effects.getInstance().playSound(Effects.SOUND_1);
                            Log.d("test","play sound");
                            break;

                        case PULL_TO_ADD_ROW:
                            resetHeader();
                            break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if(previousY != -1 && getFirstVisiblePosition() == 0 && Math.abs(mScrollStartY-event.getY()) > IDLE_DISTANCE){
                    float y = event.getY();
                    float diff = y - previousY;
                    if(diff > 0) diff /= PULL_RESISTANCE;
                    previousY = y;

                    int newHeaderPadding = Math.max(Math.round(headerPadding + diff), -header.getHeight());

                    if(newHeaderPadding != headerPadding && state != State.ADDING){
                        setHeaderPadding(newHeaderPadding);

                        if(state == State.PULL_TO_ADD_ROW && headerPadding > 0){
                            setState(State.RELEASE_TO_ADD_ROW);

                            image.clearAnimation();
                            image.startAnimation(flipAnimation);
                        }else if(state == State.RELEASE_TO_ADD_ROW && headerPadding < 0){
                            setState(State.PULL_TO_ADD_ROW);

                            image.clearAnimation();
                            image.startAnimation(reverseFlipAnimation);
                        }
                    }
                }

                break;
        }

        return super.onTouchEvent(event);
    }

    private void bounceBackHeader(){
        int yTranslate = state == State.ADDING ?
                header.getHeight() - headerContainer.getHeight() :
                -headerContainer.getHeight() - headerContainer.getTop() + getPaddingTop();;

        TranslateAnimation bounceAnimation = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, yTranslate);

        bounceAnimation.setDuration(BOUNCE_ANIMATION_DURATION);
        bounceAnimation.setFillEnabled(true);
        bounceAnimation.setFillAfter(false);
        bounceAnimation.setFillBefore(true);
        bounceAnimation.setInterpolator(new OvershootInterpolator(BOUNCE_OVERSHOOT_TENSION));
        bounceAnimation.setAnimationListener(new HeaderAnimationListener(yTranslate));

        startAnimation(bounceAnimation);
    }

    private void resetHeader(){
        if(getFirstVisiblePosition() > 0){
            setHeaderPadding(-header.getHeight());
            setState(State.PULL_TO_ADD_ROW);
            return;
        }

        if(getAnimation() != null && !getAnimation().hasEnded()){
            bounceBackHeader = true;
        }else{
            bounceBackHeader();
        }
    }

    private void setUiAdding(){
//        spinner.setVisibility(View.VISIBLE);
        image.clearAnimation();
        image.setVisibility(View.INVISIBLE);
//        text.setText(refreshingText);
        text.setVisibility(View.GONE);
        addItemEditText.setVisibility(View.VISIBLE);
       
        addItemEditText.requestFocus();

    }

    private void setState(State state){
        this.state = state;
        switch(state){
            case PULL_TO_ADD_ROW:
//                spinner.setVisibility(View.INVISIBLE);
                image.setVisibility(View.VISIBLE);
                text.setVisibility(View.VISIBLE);
                text.setText(pullToAddText);
                addItemEditText.setVisibility(View.GONE);

                if(showLastUpdatedText && lastUpdated != -1){
                    lastUpdatedTextView.setVisibility(View.VISIBLE);
                    lastUpdatedTextView.setText(String.format(lastUpdatedText, lastUpdatedDateFormat.format(new Date(lastUpdated))));
                }

                break;

            case RELEASE_TO_ADD_ROW:
//                spinner.setVisibility(View.INVISIBLE);
                image.setVisibility(View.VISIBLE);
                text.setVisibility(View.VISIBLE);
                text.setText(releaseToAddText);
                addItemEditText.setVisibility(View.GONE);
                break;

            case ADDING:
                setUiAdding();

                lastUpdated = System.currentTimeMillis();
                if(onAddingFinishListener == null){
                    setState(State.PULL_TO_ADD_ROW);
                }else{
                	
    				showSoftwareKeyboard();

                    getAddItemEditText().setOnEditorActionListener(
    						new OnEditorActionListener()
    						{
    							@Override
    							public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
    							{
    
    								if ((event != null && v != null)
    										&& actionId == EditorInfo.IME_ACTION_SEARCH
    										|| actionId == EditorInfo.IME_ACTION_DONE
    										|| event.getAction() == KeyEvent.ACTION_DOWN
    										&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
    								{
    
    									Log.d(TAG, "getAddItemEditText input done!");
    									
    									String newWord = getAddItemEditText().getText().toString();
    									onAddingFinishListener.onAddingFinish(newWord);
    									setNewEditTextString(newWord);
    									
    									getAddItemEditText().setText("");
    									onAddingComplete();
    									return true; // consume.
    									// }
    								}
    								return false; // pass on to other listeners.
    							}
    
    						});
                }

                break;
        }
    }

	public void showSoftwareKeyboard()
	{
		InputMethodManager imm = (InputMethodManager) ((Activity) getContext()).getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(getAddItemEditText(), InputMethodManager.SHOW_IMPLICIT);
	}

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt){
        super.onScrollChanged(l, t, oldl, oldt);

        if(!hasResetHeader){
            if(measuredHeaderHeight > 0 && state != State.ADDING){
                setHeaderPadding(-measuredHeaderHeight);
            }

            hasResetHeader = true;
        }
    }

    private class HeaderAnimationListener implements AnimationListener{

        private int height, translation;
        private State stateAtAnimationStart;

        public HeaderAnimationListener(int translation){
            this.translation = translation;
        }

        @Override
        public void onAnimationStart(Animation animation){
            stateAtAnimationStart = state;

            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            height = lp.height;
            lp.height = getHeight() - translation;
            setLayoutParams(lp);

            if(scrollbarEnabled){
                setVerticalScrollBarEnabled(false);
            }
        }

        @Override
        public void onAnimationEnd(Animation animation){
            setHeaderPadding(stateAtAnimationStart == State.ADDING ? 0 : -measuredHeaderHeight - headerContainer.getTop());
            setSelection(0);

            android.view.ViewGroup.LayoutParams lp = getLayoutParams();
            lp.height = height;
            setLayoutParams(lp);

            if(scrollbarEnabled){
                setVerticalScrollBarEnabled(true);
            }

            if(bounceBackHeader){
                bounceBackHeader = false;

                postDelayed(new Runnable(){

                    @Override
                    public void run(){
                        resetHeader();
                    }
                }, BOUNCE_ANIMATION_DELAY);
            }else if(stateAtAnimationStart != State.ADDING){
                setState(State.PULL_TO_ADD_ROW);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation){}
    }

    private class PTROnGlobalLayoutListener implements OnGlobalLayoutListener{

        @Override
        public void onGlobalLayout(){
            int initialHeaderHeight = header.getHeight();

            if(initialHeaderHeight > 0){
                measuredHeaderHeight = initialHeaderHeight;

                if(measuredHeaderHeight > 0 && state != State.ADDING){
                    setHeaderPadding(-measuredHeaderHeight);
                    requestLayout();
                }
            }

            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    private class PTROnItemClickListener implements OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id){
            hasResetHeader = false;

            if(onItemClickListener != null && state == State.PULL_TO_ADD_ROW){
                // Passing up onItemClick. Correct position with the number of header views
                onItemClickListener.onItemClick(adapterView, view, position - getHeaderViewsCount(), id);
            }
        }
    }

    private class PTROnItemLongClickListener implements OnItemLongClickListener{

        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id){
            hasResetHeader = false;

            if(onItemLongClickListener != null && state == State.PULL_TO_ADD_ROW){
                // Passing up onItemLongClick. Correct position with the number of header views
                return onItemLongClickListener.onItemLongClick(adapterView, view, position - getHeaderViewsCount(), id);
            }

            return false;
        }
    }

	public EditText getAddItemEditText()
	{
	
		return addItemEditText;
	}

	public void setAddItemEditText(EditText addItemEditText)
	{
	
		this.addItemEditText = addItemEditText;
	}

	public State getState()
	{
	
		return state;
	}

	public String getNewEditTextString()
	{
	
		return newEditTextString;
	}

	public void setNewEditTextString(String newEditTextString)
	{
	
		this.newEditTextString = newEditTextString;
	}
}
