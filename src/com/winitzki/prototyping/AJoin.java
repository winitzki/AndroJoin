package com.winitzki.prototyping;

import java.util.UUID;
import java.util.concurrent.Semaphore;


public class AJoin {

	// name of asynchronous molecule
	public static abstract class M_A {
		protected String moleculeName;
		protected AJoin ownerJoin;
		protected Object value;

		public String getName() {
			return moleculeName;
		}
		public M_A(String name) {
			assign(name, null);
			value = null;
			// a non-null owner join has to be assigned if this molecule were to be injected
		}
		public M_A() {
			assign(UUID.randomUUID().toString(), null);
			value = null;
		}
		protected M_A makeCopy(Object value) {
			try {
				M_A newCopy = this.getClass().newInstance();
				newCopy.assign(this.moleculeName, this.ownerJoin);
				newCopy.value = value;
				return newCopy;
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		protected void assign(String name, AJoin join) {
			moleculeName = name;
			ownerJoin = join;
		}
		protected void putInternal(Object value) {
			ownerJoin.inject(makeCopy(value));
		}
	}
	/*
	// fully constructed, asynchronous molecule
	private static abstract class MF_A {
		protected String moleculeName;
		protected AJoin ownerJoin;
		public MF_A(String name, AJoin join) {
			moleculeName = name;
			ownerJoin = join;
		}
	}
	*/
	
	// name of synchronous molecule
	public static abstract class M_S extends M_A {
		protected Object resultValue;
		protected Semaphore semaphore;
		public M_S(String name) {
			super(name);
			// a non-null owner join has to be assigned if this molecule were to be injected
		}
		public M_S() {
			super();
		}
		protected M_S makeCopy(Object value) {
			return (M_S)super.makeCopy(value);
		}
		public M_S reply(final Object value) {
			resultValue = value;
			return this;
		}
		
		protected Object putSyncInternal(Object value) {
			return ownerJoin.injectSyncAndReturnValue(makeCopy(value));
		}
	}
	
	/*
	// fully constructed, synchronous molecule
	private static abstract class MF_S {
		protected String moleculeName;
		protected AJoin ownerJoin;
		public MF_S(String name, AJoin join) {
			moleculeName = name;
			ownerJoin = join;
		}
	}
	*/
	
	public static class M_int extends M_A {

		public M_int(String name) {
			super(name);
		}
		public M_int() {
			super();
		}
		
		public void put(int value) {
			putInternal((Integer)value);
		}
		
		
	}
	public static class M_empty extends M_A {
		public M_empty(String name) {
			super(name);
		}
		public M_empty() {
			super();
		}
		
		public void put() {
			putInternal(null);
		}
		
	}
	public static class M_empty_int extends M_S {
		public M_empty_int(String name) {
			super(name);
		}
		public M_empty_int() {
			super();
		}
		
		public int put() {
			return (int)(Integer)putSyncInternal(null);
		}

	}

	public static class M_float_int extends M_S {
		public M_float_int(String name) {
			super(name);
		}
		public M_float_int() {
			super();
		}
		public int put(float value) {
			return (int)(Integer)putSyncInternal((Float)value);
		}
	}

	
	public abstract static class ReactionBody {
		public void to(M_S m) {
			// TODO implement
			// the result value is already set on the molecule.
			
			// need to use the reaction context in order to obtain the actual molecule instance to be replied to; "m" is not necessarily the right instance.
			// this is done using some private value from this instance of ReactionBody that will be set when the reaction is run. (We need a new instance of the reaction, too!)
			// then we need to set the result value on that instance, and also set it to null on "m" just in case
			// and raise the semaphore on the instance.
		}
	}
	public static class Reaction {
		M_A[] inputMolecules;
		M_A[] nominalInputMolecules;
	}
	
	/**
	 * Convenience function, to circumvent Java syntax restrictions
	 *
	 */
	
	public static M_A[] inputs(M_A... m_As) {
//		m_As[0].ownedBy = null;
		return m_As;	
	}

	private void inject(M_A fullM) {
		// TODO implement
		
	}
	private Object injectSyncAndReturnValue(M_S fullM) {
		// TODO implement
		return fullM;
	}

	/**
	 * Convenience function, to circumvent Java syntax restrictions
	 *
	 */
	
	public static Reaction reaction(M_A[] inputs, ReactionBody body) {
		// TODO implement
		return null;	
	}
	
	
	/**
	 * Define a new set of reactions, using input molecule objects that are already defined but not yet bound to a join definition.
	 *
	 */
	
	public static void define(Reaction... reactions) {
		// TODO Auto-generated method stub
		
	}
}
