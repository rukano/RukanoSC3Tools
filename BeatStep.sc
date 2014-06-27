/*
* Class for using the Arturia BeatStep with relative control knobs
* The Knob numbers are 1..16
* The Pad note numbers are 41..56
* The first row of pads is set to toggle
* The second row of pads is instant
* The big knob is CC 17
* Depends on RelativeControl.sc
* rukano 2014
*/

BeatStep {
	classvar <>midiout, <>instance;
	var <>ccOffset, <>noteOffset;
	var <>cc, <>ccStep, <>ccFunc, <>onFunc, <>offFunc, <>midiResponders;
	var <>page;
	var <>selectingPage, <>currentPage;
	var <>deviceID;

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
		instance.page.do{ |page| page.cc.do{ |rc| rc.free } };
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
		midiout.latency = 0.001;
	}

	init { |step, kindex, pindex, id=308732547|
		deviceID = id;
		ccOffset = kindex;
		noteOffset = pindex;

		selectingPage = false;
		currentPage = 0;

		page = Dictionary.new;

		17.do{ |i|
			page[i] = ();
			page[i].cc = Dictionary.new;
			page[i].ccStep = Dictionary.new;
			page[i].ccFunc = Dictionary.new;
			page[i].onFunc = Dictionary.new;
			page[i].offFunc = Dictionary.new;
			16.do{ |j|
				var index = j+1;
				page[i].ccStep[index] = step;
				page[i].cc.put(index, RelativeControl(0, 1, 0));
				page[i].ccFunc.put(index, {});
				page[i].onFunc.put(index, {});
				page[i].offFunc.put(index, {});
			};
		};
		this.makeResponders;
	}

	makeResponders {
		var selector = RelativeControl(0, 17, 0).maxMultiplier_(1).roundFactor_(1);

		midiResponders = ();
		midiResponders.cc = Dictionary.new;
		midiResponders.noteOn = Dictionary.new;
		midiResponders.noteOff = Dictionary.new;

		16.do{ |i|
			var index = i+1;
			midiResponders.cc[index] = MIDIFunc.cc({ |value|
				var val = page[currentPage].cc[index].add((value-64) * page[currentPage].ccStep[index]);
				page[currentPage].ccFunc[index].value(val);
			}, ccOffset+i, srcID:deviceID).permanent_(true);
			midiResponders.noteOn[index] = MIDIFunc.noteOn({ |value|
				page[currentPage].onFunc[index].value(value)
			}, noteOffset+i, srcID:deviceID).permanent_(true);
			midiResponders.noteOff[index] = MIDIFunc.noteOff({ |value|
				page[currentPage].offFunc[index].value(value)
			}, noteOffset+i, srcID:deviceID).permanent_(true);
		};

		midiResponders.selector = MIDIFunc.cc({ |value|
			currentPage = selector.add(value-64);
			"current page %".format(currentPage).postln;
			if( currentPage == 0 ) {
				16.do{ |i| this.blinkLED(i+1, 0.125) };
			} {
				this.blinkLED(currentPage, 0.025, times:3)
			};
		}, 17).permanent_(true);
	}

	at { |slot=1, pageNum=0|
		^page[pageNum].cc[slot]
	}

	sendMIDI { |num, value|
		if( value > 0 ) {
			midiout.noteOn(0, num, value);
		} {
			midiout.noteOff(0, num, 0);
		};
	}

	setLED { |num, value|
		this.sendMIDI(noteOffset + (num-1), value*127);
	}

	blinkLED { |num, duration=0.02, times=1|
		fork{
			times.do{
				this.setLED(num, 1);
				duration.wait;
				this.setLED(num, 0);
				duration.wait;
			};
		}
	}


}



/*


*/