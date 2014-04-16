package eu.erikw.pulltorefresh.sample;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import eu.erikw.PullToAddListView;
import eu.erikw.PullToAddListView;
import eu.erikw.PullToAddListView.OnAddingFinishListener;


import eu.erikw.SoftKeyboardUtil.OnSoftKeyBoardHideListener;
import eu.erikw.pulltorefresh.sample.PullToAddListViewSampleActivity.PullToAddListViewSampleAdapter.ViewHolder;
import eu.erikw.pulltorefresh.*;
import java.util.ArrayList;
import eu.erikw.SoftKeyboardUtil;

public class PullToAddListViewSampleActivity extends Activity
{

	private static final String TAG = "PullToAddListViewSampleActivity";

	private PullToAddListView listView;

	private PullToAddListViewSampleAdapter adapter;

	// IDs for the context menu actions
	private final int idEdit = 1;

	private final int idDelete = 2;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		listView = (PullToAddListView) findViewById(R.id.pull_to_add_listview);

		listView.setOnAddingFinishListener(new OnAddingFinishListener()
		{
			
			@Override
			public void onAddingFinish(String newEditText)
			{
				Log.d(TAG,"onAddingFinish():"+newEditText);
				addNewWordToListView(newEditText);
				
			}
		});
	

		adapter = new PullToAddListViewSampleAdapter()
		{
		};
		listView.setAdapter(adapter);

		// Request the adapter to load the data
		adapter.loadData();

		// click listener
		listView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{

				ViewHolder viewHolder = (ViewHolder) arg1.getTag();
				if (viewHolder.name != null)
				{
					Toast.makeText(PullToAddListViewSampleActivity.this,
							viewHolder.name.getText(), Toast.LENGTH_SHORT).show();
				}
			}
		});

		// Register the context menu for actions
		registerForContextMenu(listView);
	}

	public void addNewWordToListView(String newWord)
	{

		if (!newWord.equals(""))
		{
			Log.d(TAG, "newWord:" + newWord);
			adapter.addData(newWord);
		}
	}

	public void dispatchDoneKey()
	{

		dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
		dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{

		super.onConfigurationChanged(newConfig);

		// Checks whether a hardware keyboard is available
		if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
		{

			Log.d(TAG, "onConfigurationChanged HARDKEYBOARDHIDDEN_NO");
			Toast.makeText(this, "keyboard visible", Toast.LENGTH_SHORT).show();
		}
		else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES)
		{
			Log.d(TAG, "onConfigurationChanged HARDKEYBOARDHIDDEN_YES");
			Toast.makeText(this, "keyboard hidden", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Create the context menu with the Edit and Delete options
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{

		super.onCreateContextMenu(menu, v, menuInfo);

		// Add any actions you need. Implement the logic in
		// onContextItemSelected
		menu.add(Menu.NONE, idEdit, Menu.NONE, R.string.edit);
		menu.add(Menu.NONE, idDelete, Menu.NONE, R.string.delete);
	}

	/**
	 * Event called after an option from the context menu is selected
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId())
		{
		case idEdit:

			// Put your code here for Edit action
			// just as an example a toast message
			Toast.makeText(this,
					getString(R.string.edit) + " " + adapter.getItem(info.position - 1),
					Toast.LENGTH_SHORT).show();
			return true;
		case idDelete:

			// Put your code here for Delete action
			// just as an example a toast message
			Toast.makeText(this,
					getString(R.string.delete) + " " + adapter.getItem(info.position - 1),
					Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * The adapter used to display the results in the list
	 * 
	 */
	public abstract class PullToAddListViewSampleAdapter extends android.widget.BaseAdapter
	{

		private ArrayList<String> items = new ArrayList<String>();;

		public class ViewHolder
		{
			public String id;

			public TextView name;
		}

		/**
		 * Loads the data.
		 */
		public void loadData()
		{

			// Here add your code to load the data for example from a webservice
			// or DB

			items = new ArrayList<String>();

			items.add("Ajax Amsterdam");
			items.add("Barcelona");
			items.add("Manchester United");
			items.add("Chelsea");
			items.add("Real Madrid");
			items.add("Bayern Munchen");
			items.add("Internazionale");
			items.add("Valencia");
			items.add("Arsenal");
			items.add("AS Roma");
			items.add("Tottenham Hotspur");
			items.add("PSV");
			items.add("Olympique Lyon");
			items.add("AC Milan");
			items.add("Dortmund");
			items.add("Schalke 04");
			items.add("Twente");
			items.add("Porto");
			items.add("Juventus");

			// MANDATORY: Notify that the data has changed
			notifyDataSetChanged();
		}

		public void loadData2()
		{

			// Here add your code to load the data for example from a webservice
			// or DB

			/* items = new ArrayList<String>(); */

			items.add(0, "new data0");
			items.add(0, "new data1");
			items.add(0, "new data2");

			// MANDATORY: Notify that the data has changed
			notifyDataSetChanged();
		}

		public void addData(String data)
		{

			items.add(0, data);
			notifyDataSetChanged();
		}

		@Override
		public int getCount()
		{

			return items.size();
		}

		@Override
		public Object getItem(int position)
		{

			return items.get(position);
		}

		@Override
		public long getItemId(int position)
		{

			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{

			View rowView = convertView;

			String record = (String) getItem(position);

			LayoutInflater inflater = PullToAddListViewSampleActivity.this.getLayoutInflater();

			ViewHolder viewHolder = new ViewHolder();

			if (convertView == null)
			{
				rowView = inflater.inflate(R.layout.list_item, null);

				viewHolder.name = (TextView) rowView.findViewById(R.id.textView1);

				rowView.setTag(viewHolder);
			}

			final ViewHolder holder = (ViewHolder) rowView.getTag();

			holder.name.setText(record);

			return rowView;
		}
	}
}
