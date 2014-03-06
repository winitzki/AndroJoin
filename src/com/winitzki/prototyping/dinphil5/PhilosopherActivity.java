package com.winitzki.prototyping.dinphil5;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class PhilosopherActivity extends Activity implements PhilController {

	TextView[] philosopherLabels;
	final String[] names = new String[]{"A", "B", "C", "D", "E"};
	final String[] stateNames = new String[]{"Thinking", "Hungry", "Eating"};
	final int[] colors = new int[3];
	final DiningPhilosophicalLogic logic = new DiningPhilosophicalLogic();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_philosopher);
		
		philosopherLabels = new TextView[] {
				(TextView) findViewById(R.id.tv0),
				(TextView) findViewById(R.id.tv1),
				(TextView) findViewById(R.id.tv2),
				(TextView) findViewById(R.id.tv3),
				(TextView) findViewById(R.id.tv4),
				};
		colors[DiningPhilosophicalLogic.Hungry] = (0xFFFF0000);
		colors[DiningPhilosophicalLogic.Thinking] = (0xFF00FF00);
		colors[DiningPhilosophicalLogic.Eating] = (0xFF0000FF);
		
		logic.controller = this;
		logic.initialize();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.philosopher, menu);
		return true;
	}

	@Override
	public void setPhilosopherState(int state, int philosopher) {
		philosopherLabels[philosopher].setText(names[philosopher] + " is " + stateNames[state]);
		philosopherLabels[philosopher].setBackgroundColor(colors[state]);
	}

}
