/**
 * 
 */
package com.winitzki.prototyping.androjoin;

import junit.framework.TestCase;

import static com.winitzki.prototyping.androjoin.AJoin.*;
/**
 * @author user
 *
 */
public class AJoinTests extends TestCase {

	M_int counter;
	M_empty inc;
	M_empty_int getValue;
	/**
	 * @throws java.lang.Exception
	 */
	
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	
	public void tearDown() throws Exception {
	}

	
	public void test1() {
		counter = new M_int("counter");
		inc = new M_empty("increment");
		getValue = new M_empty_int("get value");

		define(
				reaction(consume(counter, inc), new ReactionBody() {
					
					public void run(int n) {
						counter.put(n+1);
					}
				}), 
			
				reaction(consume(counter, getValue), new ReactionBody() {
					public void run(int n) {
						to(getValue.reply(n));
						counter.put(n);
					}
				})
				);
		
		counter.put(0);
		inc.put();
		inc.put();
		int x = getValue.put();
		assertEquals(x, 2);
	}


}
