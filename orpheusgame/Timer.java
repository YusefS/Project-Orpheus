package orpheusgame;

/** Generates time-based events for it's owner, which must implement TimerOwner. */
public class Timer implements Runnable {
	/** This is the parent, which will be notified of timer events. */
	private TimerOwner parent;
	/** Frames per second, and the millisecond delay necessary to achieve this. */
	private int fps, fps_delay;
	/** Will be false when the timer is running, and true otherwise.*/
	private boolean bQuit;
	/** This class's own timer object. */
	private Thread timer;
	
	/** Constructs a timer with a default of 25 frames per second. */
	public Timer(TimerOwner parent){
		this(25, parent);
	}

	/** Constructs the timer, specifying frames per second and it's owner. 
	 * Frames per second must be a minimum of 1.*/
	public Timer(int fps, TimerOwner parent){
		this.parent = parent;
		// The minimum frames per second is 1.
		this.fps = Math.max(fps, 1);
		fps_delay = 1000 / fps;
	}
	
	
	/** Starts the Timer's cycling; the run() method will be called. Note that it
	 *  is important to start() the Timer only after the TimerOwner has finished it's
	 *  own initialization and packaging, otherwise null exceptions may occur. This is a
	 *  consequence of Thread usage. */
	public void start(){
		timer = new Thread(this);
		// Try starting the thread
		try { 
			bQuit = false;
			timer.start();}
		catch (IllegalThreadStateException e) {
			// Can't start a thread that's already going!
			e.printStackTrace();
		}
	}

	/** Stops the timer from cycling and generating events. */
	public void stop(){
		bQuit = true;
	}
	
	/** Called internally by Thread's start() method. No need to call this yourself. */
	public void run(){
		long oldTime = System.currentTimeMillis();
		long sleepTime, renderTime; 
		long delta = fps_delay; // Normally, each cycle will be a certain number of
								// milliseconds long. However, if there is any jitter in
								// the system's clock, this will take that into account.
		
		// The game loop
		while (!bQuit) {
			delta = System.currentTimeMillis() - oldTime; /* So how much time actually
			passed since the last cycle? It might be more or less than fps_delay, see. This
			will be 0 if we are running the very first cycle. */
			
			oldTime = System.currentTimeMillis();
			
			parent.cycle(delta); // Do a single frame, and tell the parent how much time
								 // has elapsed.
			
			// Okay, we've just used up some time by letting the parent do it's thing, how much?
			renderTime = System.currentTimeMillis() - oldTime;
			sleepTime = fps_delay - (renderTime); // How much time we have left to sleep.
			
			// Now we have to wait until it's time to cycle again...we must sleep at
			// least one millisecond to give some control back to the OS.
			if (sleepTime < 0) {sleepTime = 1;}
			try {Thread.sleep(sleepTime);}
			catch (InterruptedException ie){
				ie.printStackTrace();
				System.out.println("The Thread object in Timer has been interrupted from SLEEP!");
			}
			// Back to the top of the loop
		}
	}
	
	
}
