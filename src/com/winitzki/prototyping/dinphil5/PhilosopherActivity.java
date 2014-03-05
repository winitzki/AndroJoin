package com.winitzki.prototyping.dinphil5;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class PhilosopherActivity extends Activity {

	TextView tv1, tv2, tv3, tv4, tv5;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_philosopher);
	}

	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		View v = super.onCreateView(name, context, attrs);
	
		tv1 = (TextView)v.findViewById(R.id.tv1);
		tv2 = (TextView)v.findViewById(R.id.tv2);
		tv3 = (TextView)v.findViewById(R.id.tv3);
		tv4 = (TextView)v.findViewById(R.id.tv4);
		tv5 = (TextView)v.findViewById(R.id.tv5);
		return v;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.philosopher, menu);
		return true;
	}

}
