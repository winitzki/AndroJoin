/**
 * 
 */
package com.winitzki.prototyping;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.winitzki.prototyping.AJoin.M_empty;
import com.winitzki.prototyping.AJoin.M_empty_int;
import com.winitzki.prototyping.AJoin.M_int;

import static com.winitzki.prototyping.AJoin.*;

/**
 * @author user
 *
 */
public class AJoinTests {

	M_int counter;
	M_empty inc;
	M_empty_int getValue;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test1() {
		counter = new M_int("counter");
		inc = new M_empty("inc");
		getValue = new M_empty_int("getValue");

		define(
				reaction(inputs(counter, inc), new ReactionBody() {
					
					public void run(int n) {
						counter.put(n+1);
					}
				}), 
			
				reaction(inputs(counter, getValue), new ReactionBody() {
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
