package com.winitzki.prototyping.dinphil5;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class PhilosopherActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_philosopher);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.philosopher, menu);
		return true;
	}

}
