package orpheusgame;

/** Allows a class to own an instance of Timer. */
public interface TimerOwner {
	/** Called by the Timer; delta is the amount of time since the last cycle. */
	public void cycle(long delta);
}
