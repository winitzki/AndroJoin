package com.winitzki.prototyping.dinphil5;


import static com.winitzki.prototyping.androjoin.AJoin.consume;
import static com.winitzki.prototyping.androjoin.AJoin.define;
import static com.winitzki.prototyping.androjoin.AJoin.randomWait;
import static com.winitzki.prototyping.androjoin.AJoin.reaction;
import static com.winitzki.prototyping.androjoin.AJoin.reactionUI;

import com.winitzki.prototyping.androjoin.AJoin.M_empty;
import com.winitzki.prototyping.androjoin.AJoin.M_int;
import com.winitzki.prototyping.androjoin.AJoin.ReactionBody;

public class DiningPhilosophicalLogic {
	public PhilController controller;

		public static final int Thinking = 0, Hungry=1, Eating=2;
	
	public static void randomDelay() {
		randomWait(1000, 2000);
	}
	
	public void initialize() {
		// at this point, controller is already assigned.
		
		final M_empty tA = new M_empty("tA");
		final M_empty tB = new M_empty("tB");
		final M_empty tC = new M_empty("tC");
		final M_empty tD = new M_empty("tD");
		final M_empty tE = new M_empty("tE");
		
		final M_empty hA = new M_empty("hA");
		final M_empty hB = new M_empty("hB");
		final M_empty hC = new M_empty("hC");
		final M_empty hD = new M_empty("hD");
		final M_empty hE = new M_empty("hE");
		
		final M_empty fAB = new M_empty("fAB");
		final M_empty fBC = new M_empty("fBC");
		final M_empty fCD = new M_empty("fCD");
		final M_empty fDE = new M_empty("fDE");
		final M_empty fEA = new M_empty("fEA");
		
		final M_int state = new M_int("state");  // int value = 10*philosopher + state

		define(
				reaction(consume(tA), new ReactionBody() {
					
					public void run() {
						randomDelay();
						hA.put();
						state.put(Hungry + 10*0);
					}
				}), 
			
				reaction(consume(hA, fEA, fAB), new ReactionBody() {
					public void run() {
						state.put(Eating + 10*0);
						randomDelay();
						tA.put(); fEA.put(); fAB.put();
						state.put(Thinking + 10*0);
					}
				}),
				reaction(consume(tB), new ReactionBody() {
					
					public void run() {
						randomDelay();
						hB.put();
						state.put(Hungry + 10*1);
					}
				}), 
			
				reaction(consume(hB, fAB, fBC), new ReactionBody() {
					public void run() {
						state.put(Eating + 10*1);
						randomDelay();
						tB.put(); fAB.put(); fBC.put();
						state.put(Thinking + 10*1);
					}
				}),
				reaction(consume(tC), new ReactionBody() {
					
					public void run() {
						randomDelay();
						hC.put();
						state.put(Hungry + 10*2);
					}
				}), 
			
				reaction(consume(hC, fBC, fCD), new ReactionBody() {
					public void run() {
						state.put(Eating + 10*2);
						randomDelay();
						tC.put(); fBC.put(); fCD.put();
						state.put(Thinking + 10*2);
					}
				}),
				reaction(consume(tD), new ReactionBody() {
					
					public void run() {
						randomDelay();
						hD.put();
						state.put(Hungry + 10*3);
					}
				}), 
			
				reaction(consume(hD, fCD, fDE), new ReactionBody() {
					public void run() {
						state.put(Eating + 10*3);
						randomDelay();
						tD.put(); fCD.put(); fDE.put();
						state.put(Thinking + 10*3);
					}
				}),
				reaction(consume(tE), new ReactionBody() {
					
					public void run() {
						randomDelay();
						hE.put();
						state.put(Hungry + 10*4);
					}
				}), 
			
				reaction(consume(hE, fDE, fEA), new ReactionBody() {
					public void run() {
						state.put(Eating + 10*4);
						randomDelay();
						tE.put(); fDE.put(); fEA.put();
						state.put(Thinking + 10*4);
					}
				}),
				
				reactionUI(consume(state), new ReactionBody() {
					public void run(int n) {
						controller.setPhilosopherState(n%10, (int)(n/10));
					}
				})
				);
		
		tA.put(); tB.put(); tC.put(); tD.put(); tE.put();
		fAB.put(); fBC.put(); fCD.put(); fDE.put(); fEA.put();
		
		controller.setPhilosopherState(Thinking, 0);
		controller.setPhilosopherState(Thinking, 1);
		controller.setPhilosopherState(Thinking, 2);
		controller.setPhilosopherState(Thinking, 3);
		controller.setPhilosopherState(Thinking, 4);
		
	}
}
