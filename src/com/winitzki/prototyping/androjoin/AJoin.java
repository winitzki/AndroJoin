package com.winitzki.prototyping.androjoin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;


public class AJoin {
	private Reaction[] definedReactions;
	private int joinID = 0;
	private static int globalJoinCount = 0;
	private Map<String,List<M_A>> availableMolecules;
	private Set<String> knownMoleculeNames;
	private boolean decideOnUIThread;
	
	private static Handler uiHandler = new Handler(Looper.getMainLooper());
	public AJoin(boolean uiThread) {
		joinID = ++globalJoinCount;
		decideOnUIThread = uiThread;
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
			try {
				M_A newCopy = this.getClass().newInstance();
				newCopy.assign(this.moleculeName, this.ownerJoin);
				newCopy.value = value;
				return newCopy;
			} catch (InstantiationException e) {
				
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				
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
		public String toString() {
			String joinIdString = "";
			if (ownerJoin != null) joinIdString = "[j=" + ownerJoin.joinID + "]";
			return moleculeName + "(" + value.toString() + ")" + joinIdString;
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

	
	public abstract static class ReactionBody {
		private List<M_A> inputMolecules;
		private boolean scheduleOnUIThread;
		
		private M_A findMoleculeById(M_A m) {
			for (M_A inM : inputMolecules) {
				if (inM.getName().equals(m.getName())) return m;
			}
			return null;
		}
		private ReactionBody makeCopy(boolean scheduleOnUIThread) {
			ReactionBody newCopy = null;
			try {
				newCopy = this.getClass().newInstance();
				newCopy.scheduleOnUIThread = scheduleOnUIThread;
			} catch (InstantiationException e) {
				
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				
				e.printStackTrace();
			}
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
			final ReactionBody instance = this;
			final Class<?> instanceClass = this.getClass();
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
					if (paramNumber != paramClasses.length) {
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
			});
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
		if (!isMoleculeKnown(fullM)) return;
		injectAndStartReactions(fullM);		
	}
	private Object injectSyncAndReturnValue(M_S fullM) {
		if (!isMoleculeKnown(fullM)) return null;
		injectAndStartReactions(fullM);
		try {
			fullM.semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		fullM.semaphore = null;
		return fullM.resultValue;
	}
	private static void runOnUIThread(boolean uiThread, final Runnable runnable) {
		if (uiThread) {
			if (Looper.myLooper() == Looper.getMainLooper()) {
				runnable.run();
			} else {
				uiHandler.post(runnable);
			}
		} else {
			runInBackground(runnable);
		}
	}
	private static void runInBackground(final Runnable runnable) {
		if (runnable != null){
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					runnable.run();
					return null;
				}
			};
		}
	}
	private void injectAndStartReactions(final M_A fullM) {
		runOnUIThread(decideOnUIThread, new Runnable() {
			
			@Override
			public void run() {
				internalInjectAndStartReactions(fullM);
			}
		});
		
	}
	private void internalInjectAndStartReactions(M_A fullM) {
		List<M_A> presentMolecules = availableMolecules.get(fullM.getName());
		if (presentMolecules == null) {
			presentMolecules = new ArrayList<AJoin.M_A>();
			availableMolecules.put(fullM.getName(), presentMolecules);
		}
		presentMolecules.add(fullM);
		
		decideAndRunPossibleReactions();
	}

	private void decideAndRunPossibleReactions() {
		ReactionBody foundReaction = null;
		while( (foundReaction = findAnyReaction()) != null ) {
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
		return null;
	}

	/*NSMutableArray *affectedMoleculeList = [NSMutableArray arrayWithCapacity:moleculeNames.count];
    for (NSString *moleculeClass in moleculeNames) {
        NSMutableArray *presentMolecules = [self.availableMoleculeNames objectForKey:moleculeClass];
        if ([presentMolecules count] > 0) {
            [presentMolecules shuffle];  // important to remove a randomly chosen object! so, first we shuffle,
            id chosenMolecule = [presentMolecules lastObject]; // then we select the last object.
            
            [molecules addObject:chosenMolecule];
            [affectedMoleculeList addObject:presentMolecules]; // affectedMoleculeList is the list of arrays from which we have selected last elements. Each of these arrays needs to be trimmed (the last element removed), but only if we finally succeed in finding all required molecules. Otherwise, nothing should be removed from any lists.
        } else {
            // did not find this molecule, but reaction requires it - nothing to do now.
            return nil;
        }
    }
    if (moleculeNames.count != molecules.count) return nil;
    // if we are here, we have found all input molecules required for the reaction!
    // now we need to remove them from the molecule arrays; note that affectedMoleculeInstances is a pointer to an array inside the dictionary self.availableMoleculeNames.
    for (NSMutableArray *affectedMoleculeInstances in affectedMoleculeList) {
        [affectedMoleculeInstances removeLastObject]; // now that the array was shuffled, we know that we need to remove the last object.
    }
    return [NSArray arrayWithArray:molecules];*/
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
				m.ownerJoin = this;
				knownMoleculeNames.add(m.getName());
			}
		}
		
	}
}
