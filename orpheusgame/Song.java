package orpheusgame;

import java.io.File;
import java.io.IOException;
import javax.sound.midi.*;

/** Encapsulates MIDI data from a musical piece, as well as MIDI playback controls. By implementing Receiver, 
 *  all messages sent by the sequencer will also be sent to this receiver. This allows the program to 
 *  do nifty effects based on what note is currently playing, for example. */
public class Song implements Receiver {

	/** The midi file from where the song is loaded. */
	private File source;
	/** The sequence of midi events from the source file. A sequence consists of many tracks. */
	private Sequence sequence;
	/** The tracks stored in the sequence. */
	private Track[] tracks;
	
	/** Is true when the song has finished playing. */
	private boolean isOver;
	
	/** An array of every note, holding the time when that note should be turned off. */
	private long[] notes = new long[128];
	/** All the channels available from the synthesizer. We generally only use the first one (i.e. channels[0])*/
	private MidiChannel[] channels;
	/** The bank of all instruments available. */
	private Instrument[] soundbank;
	
	/** The system's current MIDI sequencer and synthesizer objects. */
	private Sequencer sequencer;
	private Synthesizer synthesizer;
	
	public Song(){
		// Nothing to do here yet.
		// I could have put loadDevices() here, but then I wouldn't be able to return true/false.
	}

	/** Sets up MIDI devices for playback and opens sequencer. Returns true upon success, false otherwise. 
	 *  This must be called before playback can occur. */
	public boolean loadDevices(){
		try {sequencer = MidiSystem.getSequencer();} 
		catch (MidiUnavailableException e) {
			e.printStackTrace();
			return false;
		}
		
		if (sequencer == null) {
			System.out.println("Unable to load MIDI Sequencer.");
			return false;
		}
		
		// Add Listener so sequencer/synthesizer closes at the end of a song's playback.
		sequencer.addMetaEventListener(new MetaEventListener() {
			public void meta(MetaMessage event) {
				if (event.getType() == 47) {
					sequencer.close();
					// Don't close the synthesizer if you still wanna make noises
					//if (synthesizer != null) {synthesizer.close();}
					// Add code here if we want to know when the song ends!
					// ....
					// ....
					// ....
					isOver = true;
				}
			}
		});
		
		// Open Sequencer
		try { sequencer.open();}
		catch (MidiUnavailableException e) {
			e.printStackTrace();
			return false;	
		}
		
		// Now some complicated stuff, is our sequencer a synthesizer? On many systems, this will be true.
		// If this is not the case, then we have to account for it.
		if (! (sequencer instanceof Synthesizer)) {
			// Link the sequencer to the system's default synthesizer
			try {
				synthesizer = MidiSystem.getSynthesizer();
				synthesizer.open();
				Receiver synthReceiver = synthesizer.getReceiver();
				Transmitter seqTransmitter = sequencer.getTransmitter();
				seqTransmitter.setReceiver(synthReceiver);
			}
			catch (MidiUnavailableException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		// Here we allow Song to intercept sequencer messages:
		try {
			Transmitter seqIntercept = sequencer.getTransmitter();
			seqIntercept.setReceiver(this);
		}
		catch (MidiUnavailableException e) {
			System.out.println("Problem linking Receiver");
			e.printStackTrace();
			return false;
		}
		
		// Grab the raw channels from the synthesizer so we can directly turn notes on and off if we
		// want to.
		// Note that if the sequencer is an instance of the synthesizer, we don't need to do this???
		try {
			synthesizer = MidiSystem.getSynthesizer();
			synthesizer.open();
			channels = synthesizer.getChannels();
			soundbank = synthesizer.getDefaultSoundbank().getInstruments();
			synthesizer.loadInstrument(soundbank[1]);
			//channels[0].programChange(24);
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
			return false;
		}
			
		// Everything went better than expected!
		return true;
	}
	
	public boolean loadData(File source){
		this.source = source;
		
		try {
			sequence = MidiSystem.getSequence(source);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;			
		}
		
		tracks = sequence.getTracks();
		// If everything went okay, return true;
		return true;
	}
	
	/** Returns the array of tracks in the loaded sequence. Will be null if no sequence loaded, or upon error. */
	public Track[] getTracks(){
		return tracks;
	}
	
	/** Returns the length of the song, in milliseconds. */
	public long getLength(){
		return (sequence.getMicrosecondLength() / 1000);
	}
	
	/** Returns milliseconds per tick for this sequence. */
	public float getTempo(){
		return (getLength() * 1.0f / sequence.getTickLength());
	}
	
	/** Returns the source midi file of the song. */
	public File getSource(){
		return source;
	}

	public void close() {
		// Nothing to do here.
	}

	/** Ever message that goes through Song will also be sent through here, by virtue of the fact that we've
	 * linked it up that way in the loadDevices() method. */
	public void send(MidiMessage msg, long timeStamp) {
		// The status byte of a midi message; 
		// msg.getStatus();
		// MidiMessage data comes in signed bytes! We gotta convert it to integer so Java can use it...
		int iData = (int) (msg.getMessage()[1] & 0xFF);
	}
	
	/** Plays the loaded sequence. MIDI events will trickle through the send() method. */
	public boolean play() {
		// If the sequencer has been closed, we need to reopen it. Alternatively, we could just not ever close
		// it.
		try { sequencer.open();}
		catch (MidiUnavailableException e) {
			e.printStackTrace();
			return false;	
		}
		
		try {sequencer.setSequence(sequence);}
		catch (InvalidMidiDataException e) {
			e.printStackTrace();
			return false;
		}
		
		sequencer.start();
		isOver = false;
		return true;
	}
	
	/** Halts playback of the currently playing song. Does this reset song to beginning???*/
	public void stop(){
		if (sequencer == null) {return;}
		if (!sequencer.isOpen()){return;}
		sequencer.stop();
	}
	
	/** Sends a NoteOn message directly to the MIDI channel. This has the effect of immediately playing the sound. */
	public void noteOn(int noteNumber, int velocity, long toff){
		channels[0].noteOn(noteNumber, velocity);
		notes[noteNumber] = toff; //The time when the note needs to end
	}
	
	/** Sends a NoteOff message directly to the MIDI channel. If the note was playing, it should now be off. */
	public void noteOff(int noteNumber, int velocity){
		channels[0].noteOff(noteNumber, velocity);
		notes[noteNumber] = 0;
	}
	
	/** This updates all currently playing notes and turns them off properly. 
	 * @param 'time' refers to the CURRENT TIME*/
	public void updateNotes(long time){
		for (int i = 0; i < notes.length; i++){
			if ((notes[i] <= time) && (notes[i] != 0)) {noteOff(i, 127);}
		}
	}

	// TODO Include support for multiple channels, because a single MIDI channel generally can only have one MIDI instrument loaded. That would be boring!!!
	
	/** Induces a program change in the chosen channel. */
	public void setChannelInstrument(int channel, int instrument){
		if (channel >= channels.length || channel < 0) {return;}
		if (instrument >= 128 || instrument < 0) {return;}
		channels[channel].programChange(instrument);
	}
	
	/** Returns true if the current song has finished playing. */
	public boolean isOver(){
		return isOver;
	}
}
