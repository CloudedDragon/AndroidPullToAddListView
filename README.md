Android "Pull to add" ListView library
========================
Android custom ListView for adding a row by pulling the top of listview

## Screenshot
![](https://raw.githubusercontent.com/CloudedDragon/AndroidPullToAddListView/master/effect.png)


## Statement
The project is just for research purpose,not as a commercial activity.



## example

``` java
// Set a listener to be invoked when the list should be added.

	PullToAddListView listView = (PullToAddListView) findViewById(R.id.pull_to_add_listview);

	listView.setOnAddingFinishListener(new OnAddingFinishListener()
	{
			
			@Override
			public void onAddingFinish(String newEditText)
			{
				Log.d(TAG,"onAddingFinish():"+newEditText);
				
				// Your code to get the new edit text string 
				
				// ...
				// 
				//example : add the text string to the adapter of the listview
				addNewWordToListView(newEditText);
				
			}
	});
		
	public void addNewWordToListView(String newWord)
	{

		if (!newWord.equals(""))
		{
			Log.d(TAG, "newWord:" + newWord);
			adapter.addData(newWord);
		}
	}

```


## Reference

This project is reference the https://github.com/erikwt/PullToRefresh-ListView

## License
Copyright (c) 2014 - CloudedDragon

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
