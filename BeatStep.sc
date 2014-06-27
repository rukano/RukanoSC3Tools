/*
 * Class for using the Arturia BeatStep with relative control knobs
 * Depends on RelativeControl.sc
 * rukano 2014
 */

BeatStep {
	classvar <>midiout, <>instance;
	var <>ccOffset, <>noteOffset;
	var <>cc, <>ccStep, <>ccFunc, <>onFunc, <>offFunc, <>midiResponders;

	*new { |step=0.001, kindex=1, pindex=41|
		if(MIDIClient.initialized) {
			BeatStep.connectMIDI;
		} {
			MIDIClient.init;
			BeatStep.connectMIDI;
		};
		if(instance.isNil) {
			instance = super.new.init(step, kindex, pindex)
		};
		^instance
	}

	*clear {
		instance.cc.do{ |rc| rc.free };
		instance.midiResponders.do{ |section|
			section.do{ |responder|
				responder.free;
			};
		};
	}

	*reconnectMIDI {
		MIDIClient.init;
		BeatStep.connectMIDI;
	}

	*connectMIDI {
		MIDIIn.connectAll;
		midiout = MIDIOut.newByName("Arturia BeatStep", "Arturia BeatStep");
	}

	init { |step, kindex, pindex|
		ccOffset = kindex;
		noteOffset = pindex;
		cc = Dictionary.new;
		ccStep = Dictionary.new;
		ccFunc = Dictionary.new;
		onFunc = Dictionary.new;
		offFunc = Dictionary.new;
		16.do{ |i|
			var index = i+1;
			ccStep[index] = step;
			cc.put(index, RelativeControl(0, 1, 0));
			ccFunc.put(index, {});
			onFunc.put(index, {});
			offFunc.put(index, {});
		};

		ccStep[17] = step;
		cc.put(17, RelativeControl(0, 1, 0));
		ccFunc.put(17, {});

		this.makeResponders;
	}

	makeResponders {
		midiResponders = ();
		midiResponders.cc = Dictionary.new;
		midiResponders.noteOn = Dictionary.new;
		midiResponders.noteOff = Dictionary.new;

		16.do{ |i|
			var index = i+1;
			midiResponders.cc[index] = MIDIFunc.cc({ |value|
				cc[index].add((value-64)*ccStep[index]);
				ccFunc[index].value(cc[index].currentValue)
			}, ccOffset+i).permanent_(true);
			midiResponders.noteOn[index] = MIDIFunc.noteOn({ |value| onFunc[index].value(value) }, noteOffset+i).permanent_(true);
			midiResponders.noteOff[index] = MIDIFunc.noteOff({ |value| offFunc[index].value(value) }, noteOffset+i).permanent_(true);
		};
	}

}



/*


*/