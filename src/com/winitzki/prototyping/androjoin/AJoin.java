package com.winitzki.prototyping.androjoin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;


public class AJoin {
	private Reaction[] definedReactions;
	private int joinID = 0;
	private static int globalJoinCount = 0;
	private Map<String,List<M_A>> availableMolecules;
	private Set<String> knownMoleculeNames;
	private boolean decideOnUIThread;
	
	// multithreading: all joins will be decided on one thread; all reactions will be executed on two threads
	private static final Handler uiHandler = new Handler(Looper.getMainLooper());
	private static final int numberOfCores = Runtime.getRuntime().availableProcessors();
	private static final ThreadPoolExecutor executorForJoins = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	private static final ThreadPoolExecutor executorForReactions = new ThreadPoolExecutor(2, Math.max(2, numberOfCores), 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	
	public AJoin(boolean uiThread) {
		joinID = ++globalJoinCount;
		decideOnUIThread = uiThread;
	}
	public static void randomWait(long minMillis, long maxMillis) {
		long randomTime = new Random().nextInt();
		if (randomTime < 0) randomTime = -randomTime; // avoid negative values here!
		randomTime = randomTime - (long)(Math.floor( randomTime / maxMillis )) * maxMillis; 
		long time = minMillis + randomTime;
		Log.d("AJoin", String.format("%d ms wait", (int)time) );

		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
	}
	// name of asynchronous molecule
	public static abstract class M_A {
		protected String moleculeName;
		protected AJoin ownerJoin;
		protected Object value;

		public String getName() {
			return moleculeName;
		}
		abstract protected Class<?> getValueClass();

		public M_A(String name) {
			assign(name, null);
			value = null;
			// a non-null owner join has to be assigned if this molecule were to be injected
		}
		public M_A() {
			assign(UUID.randomUUID().toString(), null);
			value = null;
		}
		private M_A makeCopy(Object value) {
			M_A newCopy = null;
			try {
				newCopy = this.getClass().newInstance();
				newCopy.assign(this.moleculeName, this.ownerJoin);
				newCopy.setValue(value);
				
			} catch (InstantiationException e) {
				
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				
				e.printStackTrace();
			}
			return newCopy;
		}
		private void setValue(Object value2) {
			this.value = value2;
			
		}
		protected void assign(String name, AJoin join) {
			moleculeName = name;
			ownerJoin = join;
		}
		protected void putInternal(Object value) {
			ownerJoin.inject(makeCopy(value));
		}
		public String toString() {
//			String joinIdString = "";
//			if (ownerJoin != null) joinIdString = "[j=" + ownerJoin.joinID + "]";
			String valueString = "()";
			if (value != null) valueString = "(" + value + ")";
			return moleculeName + valueString;// + joinIdString;
		}
	}
	
	// name of synchronous molecule
	public static abstract class M_S extends M_A {
		protected Object resultValue = null;
		protected Semaphore semaphore = null;
		public M_S(String name) {
			super(name);
			// a non-null owner join has to be assigned if this molecule were to be injected
		}
		public M_S() {
			super();
		}
		private M_S makeCopy(Object value) {
			return (M_S)super.makeCopy(value);
		}
		public M_S reply(final Object value) {
			resultValue = value;
			return this;
		}
		
		protected Object putSyncInternal(Object value) {
			M_S newCopy = makeCopy(value);
			newCopy.initializeSemaphore();
			return ownerJoin.injectSyncAndReturnValue(newCopy);
		}
		private void initializeSemaphore() {
			semaphore = new Semaphore(0, true);
			semaphore.drainPermits(); // documentation does not say how many "permits" I need to set initially. So let's drain them.
		}
	}
	
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
		@Override
		protected Class<?> getValueClass() {
			return int.class;
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
		@Override
		protected Class<?> getValueClass() {
			return null;
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
		@Override
		protected Class<?> getValueClass() {
			return null;
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
		@Override
		protected Class<?> getValueClass() {
			return float.class;
		}
		
	}

	
	public static class ReactionBody {
		private List<M_A> inputMolecules;
		private boolean scheduleOnUIThread;
		private ReactionBody givenBody;
		
		public String toString() {
			return "{input: " + inputMolecules.toString() + ", ui=" + scheduleOnUIThread + "}";
		}
		private M_A findMoleculeById(M_A m) {
			for (M_A inM : inputMolecules) {
				if (inM.getName().equals(m.getName())) return m;
			}
			return null;
		}
		private ReactionBody makeCopy(boolean scheduleOnUIThread) {
			ReactionBody newCopy = null;
			newCopy = new ReactionBody();
			newCopy.givenBody = this;
			newCopy.scheduleOnUIThread = scheduleOnUIThread;
			
			return newCopy;
		}
		public static Method findMethod(Class<?> instanceClass, String name) {
			for (Method m : instanceClass.getDeclaredMethods()) {
				if (m.getName().equals(name)) {
					return m;
				}
			}
			return null;
		}
		private void runReactionBody() { // input molecules must be already assigned at this point!
			final ReactionBody instance = this.givenBody;
			instance.inputMolecules = this.inputMolecules;
			final Class<?> instanceClass = this.givenBody.getClass();
			runOnUIThread(scheduleOnUIThread, new Runnable() {
				
				@Override
				public void run() {
					
					// introspect the types of all arguments of the overloaded "run" function.
					Method runMethod = findMethod(instanceClass, "run");
					Class<?>[] paramClasses = runMethod.getParameterTypes();
					// find the corresponding molecule values from the provided input molecules, using their known types, in the declared order.
					// construct the actual typed arguments for the reaction block, and execute "run" with these arguments.
					Object[] params = new Object[paramClasses.length];
					int paramNumber = -1;
					boolean paramError = false;
					for (M_A m : inputMolecules) {
						if (m.getValueClass() != null) {
							paramNumber++;
							if (paramNumber < paramClasses.length && m.getValueClass().equals(paramClasses[paramNumber])) {
								params[paramNumber] = m.value;
							} else {
								// error! mismatched types or number of parameters.
								paramError = true;
							}
						}
					}
					if (paramNumber +1 != paramClasses.length) {
						// error! too few parameters in the run() method of reaction body
						paramError = true;
					}
					if (!paramError) {
						try {
							runMethod.invoke(instance, params);
						} catch (IllegalAccessException e) {
						 
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
						 
							e.printStackTrace();
						} catch (InvocationTargetException e) {
						 
							e.printStackTrace();
						}
					}
					
				}
			}, executorForReactions);
		}

		protected void to(M_S m) {
			
			// the result value is already set on the molecule object m.
			M_S ms = (M_S)findMoleculeById(m);
			Object resultValue = m.resultValue;
			m.resultValue = null;
			ms.resultValue = resultValue;
			ms.semaphore.release();
			// need to use the reaction context in order to obtain the actual molecule instance to be replied to; "m" is not necessarily the right instance.
			// this is done using some private value from this instance of ReactionBody that will be set when the reaction is run. (We need a new instance of the reaction, too!)
			// then we need to set the result value on that instance, and also set it to null on "m" just in case
			// and raise the semaphore on the instance.
		}
	}
	/// this class is used only as a container for values specified by the user
	public static class Reaction {
		private M_A[] nominalInputMolecules;
		private ReactionBody reactionBody;
		private boolean scheduleOnUIThread;
		
		public Reaction(M_A[] nominalInputs, ReactionBody body, boolean uiThread) {
			nominalInputMolecules = nominalInputs;
			reactionBody = body;
			scheduleOnUIThread = uiThread;
		}
	}
	
	/**
	 * Convenience function, to circumvent Java syntax restrictions
	 *
	 */
	
	public static M_A[] consume(M_A... m_As) {
		return m_As;	
	}

	private boolean isMoleculeKnown(M_A m) {
		return knownMoleculeNames != null && knownMoleculeNames.contains(m.getName());
	}
	private void inject(M_A fullM) {
		if (!isMoleculeKnown(fullM)) {
			Log.e("AJoin", "injecting unknown molecule " + fullM.getName() + ", now " + toString());
			throw new IllegalArgumentException("injecting unknown molecule " + fullM.getName());
		}
		
		injectAndStartReactions(fullM);		
	}
	private Object injectSyncAndReturnValue(M_S fullM) {
		if (!isMoleculeKnown(fullM))  {
			throw new IllegalArgumentException();
		}
		injectAndStartReactions(fullM);
		try {
			fullM.semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		fullM.semaphore = null;
		return fullM.resultValue;
	}
	private static void runOnUIThread(boolean uiThread, final Runnable runnable, ThreadPoolExecutor executor) {
		if (uiThread) {
			if (Looper.myLooper() == Looper.getMainLooper()) {
				runnable.run();
			} else {
				uiHandler.post(runnable);
			}
		} else {
			runInBackground(runnable, executor);
		}
	}
	private static void runInBackground(final Runnable runnable, ThreadPoolExecutor executor) {
		if (runnable != null) {
			executor.execute(runnable);
//			new AsyncTask<Void, Void, Void>() {
//				@Override
//				protected Void doInBackground(Void... params) {
//					runnable.run();
//					return null;
//				}
//			}.execute();
		}
	}
	private void injectAndStartReactions(final M_A fullM) {
		runOnUIThread(decideOnUIThread, new Runnable() {
			
			@Override
			public void run() {
//				Log.d("AJoin", "initial request to inject molecule " + fullM.getName());
				internalInjectAndStartReactions(fullM);
			}
		}, executorForJoins);
		
	}
	private void internalInjectAndStartReactions(M_A fullM) {
//		Log.d("AJoin", "internalInjectAndStartReactions: " + fullM.getName());
		List<M_A> presentMolecules = availableMolecules.get(fullM.getName());
		if (presentMolecules == null) {
			presentMolecules = new ArrayList<AJoin.M_A>();
			availableMolecules.put(fullM.getName(), presentMolecules);
		}
		presentMolecules.add(fullM);
		Log.d("AJoin", "adding molecule " + fullM.getName() + ", now " + toString());
		decideAndRunPossibleReactions();
	}

	private void decideAndRunPossibleReactions() {
		ReactionBody foundReaction = null;
		while( (foundReaction = findAnyReaction()) != null ) {
			Log.d("AJoin", "about to run reaction " + foundReaction.toString());
			foundReaction.runReactionBody();
		}
		
	}

	private ReactionBody findAnyReaction() {
		List<Reaction> reactions = Arrays.asList(definedReactions);
		Collections.shuffle(reactions);
		for (Reaction r : reactions) {
			List<M_A> availableInput = moleculesAvailable(r.nominalInputMolecules);
			if (availableInput != null) {
				ReactionBody newBody = r.reactionBody.makeCopy(r.scheduleOnUIThread);
				newBody.inputMolecules = availableInput;
				return newBody;
			}
		}
		Log.d("AJoin", "found no reactions, now " + toString());
		return null;
	}

	public String toString() {
		String molecules = "";
		for (String n : availableMolecules.keySet()) {
			for (M_A m : availableMolecules.get(n)) {
				if (molecules.length() > 0) {
					molecules += ", ";
				}
				molecules += m.toString();
			}
		}
		return String.format(Locale.US, "{AJoin %d, soup=%s}", joinID, molecules);
	}
	
	private List<M_A> moleculesAvailable(M_A[] nominalInputMolecules) {
		List<M_A> molecules = new ArrayList<AJoin.M_A>();
		List<List<M_A>> affectedMoleculeList = new ArrayList<List<AJoin.M_A>>();
		for (M_A m : nominalInputMolecules) {
			List<M_A> presentMolecules = availableMolecules.get(m.getName());
			if (presentMolecules != null && presentMolecules.size() > 0) {
				Collections.shuffle(presentMolecules);
				molecules.add(presentMolecules.get(0));
				affectedMoleculeList.add(presentMolecules);
			} else {
				return null;
			}
			
		}
		if (molecules.size() != nominalInputMolecules.length) {
			return null;
			
		}
		for (List<M_A> l : affectedMoleculeList) {
			l.remove(0);
		}
		return molecules;
	}

	/**
	 * Convenience functions, to circumvent Java syntax restrictions
	 *
	 */
	
	public static Reaction reaction(M_A[] nominalInputs, ReactionBody body) {
		return new Reaction(nominalInputs, body, false);
	}
	
	public static Reaction reactionUI(M_A[] nominalInputs, ReactionBody body) {
		return new Reaction(nominalInputs, body, true);
	}
	
	
	/**
	 * Define a new set of reactions, using input molecule objects that are already defined but not yet bound to a join definition.
	 *
	 */
	
	public static void define(Reaction... reactions) {
		defineInternal(false, reactions);
	}
	
	public static void defineUI(Reaction... reactions) {
		defineInternal(true, reactions);
	}
			
	private static void defineInternal(boolean uiThread, Reaction... reactions) {
		AJoin join = new AJoin(uiThread);
		join.definedReactions = reactions;
		join.initializeReactionsAndMolecules(); // this will assign the join instance to the given input molecules.
	}

	private void initializeReactionsAndMolecules() {
		knownMoleculeNames = new HashSet<String>();
		availableMolecules = new HashMap<String, List<M_A>>();
		for (Reaction r : definedReactions) {
			for (M_A m : r.nominalInputMolecules) {
				if (m.ownerJoin != null && m.ownerJoin != this) {
					throw new IllegalAccessError("cannot consume molecule " + m.getName() + " defined in another join ID=" + m.ownerJoin.joinID);
				}
				m.ownerJoin = this;
				knownMoleculeNames.add(m.getName());
			}
		}
		Log.d("AJoin", "known molecules " + knownMoleculeNames.toString());
	}
}
