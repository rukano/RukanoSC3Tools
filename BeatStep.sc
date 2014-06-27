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
	var <>midiResponders;
	var <>page;
	var <>selectingPage, <>currentPage;
	var <>deviceID;
	var <>gui;

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
			page[i].pad = Dictionary.new;
			page[i].onFunc = Dictionary.new;
			page[i].offFunc = Dictionary.new;
			16.do{ |j|
				var index = j+1;
				page[i].ccStep[index] = step;
				page[i].cc.put(index, RelativeControl(0, 1, 0));
				page[i].ccFunc.put(index, {});
				page[i].pad.put(index, 0);
				page[i].onFunc.put(index, {});
				page[i].offFunc.put(index, {});
			};
		};
		this.makeResponders;
		this.makeGUI;
	}

	makeGUI {
		gui = BeatStepGui();
	}

	makeResponders {
		var selector = RelativeControl(0, 16, 0).maxMultiplier_(1).roundFactor_(1);

		midiResponders = ();
		midiResponders.cc = Dictionary.new;
		midiResponders.noteOn = Dictionary.new;
		midiResponders.noteOff = Dictionary.new;

		16.do{ |i|
			var index = i+1;
			midiResponders.cc[index] = MIDIFunc.cc({ |value|
				var val = page[currentPage].cc[index].add((value-64) * page[currentPage].ccStep[index]);
				page[currentPage].ccFunc[index].value(val);
				if( gui.notNil ) { gui.setKnob(index, val) };
			}, ccOffset+i, srcID:deviceID).permanent_(true);
			midiResponders.noteOn[index] = MIDIFunc.noteOn({ |value|
				page[currentPage].pad[index] = 1;
				page[currentPage].onFunc[index].value(value);
				if( gui.notNil ) { gui.setPad(index, 0) };
			}, noteOffset+i, srcID:deviceID).permanent_(true);
			midiResponders.noteOff[index] = MIDIFunc.noteOff({ |value|
				page[currentPage].pad[index] = 0;
				page[currentPage].offFunc[index].value(value);
				if( gui.notNil ) { gui.setPad(index, 0) };
			}, noteOffset+i, srcID:deviceID).permanent_(true);
		};

		midiResponders.selector = MIDIFunc.cc({ |value|
			currentPage = selector.add(value-64);
//			"current page %".format(currentPage).postln;
			if( currentPage == 0 ) {
				16.do{ |i| this.blinkLED(i+1, 0.125) };
			} {
				this.blinkLED(currentPage, 0.025, times:3)
			};
			if( gui.notNil ) {
				gui.setPage(currentPage);
				this.recallPage;
			};
		}, 17).permanent_(true);
	}

	recallPage {
		page[currentPage].cc.keysValuesDo{ |index, rv|
			gui.setKnob(index, rv.currentValue);
		};
		page[currentPage].pad.keysValuesDo{ |index, value|
			gui.setPad(index, value);
		};
	}

	at { |slot=1, pageNum=0|
		^page[pageNum].cc[slot]
	}

	kr { |slot=1, lag=0.01, mul=1, add=0, page=0|
		^this.at(slot, page).kr(lag, mul, add);
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


BeatStepGui {
	var <>window, <>knobView, <>padView, <>infoView, <>listView;
	var <>knob, <>pad, <>knobLabel, <>padLabel;

	*new {
		^super.new.init
	}

	init {
		knob = Dictionary.new;
		pad = Dictionary.new;
		knobLabel = Dictionary.new;
		padLabel = Dictionary.new;
		this.makeGUI;
	}

	setKnob { |num, val|
		{ knob[num].value_(val) }.defer;
	}

	setPad { |num, val|
		{ pad[num].value_(val) }.defer;
	}

	setPage { |val|
		{ listView.selection = val }.defer;
	}

	makeGUI {
		window = Window("BeatStep Visualizer", Rect(0, 0, 565, 270)).front;
		window.view.background_(Color.white);
		infoView = View(window, Rect(0, 0, 200, 270)).background_(Color.hsv(0.7, 0.3, 0.7, 0.6));
		knobView = View(window, Rect(200, 0, 400, 135)).background_(Color.hsv(0.6, 0.8, 0.7, 0.3));
		padView = View(window, Rect(200, 135, 400, 135)).background_(Color.hsv(0.6, 0.9, 0.9, 0.1));

		knobView.addFlowLayout((5@5), (5@5));
		padView.addFlowLayout((5@5), (5@5));

		2.do{ |i|
			var size = 40;
			8.do{ |j|
				var index = j+(i*8)+1;
				knobLabel.put(index, StaticText(knobView, (size@15))
					.font_(Font("Monaco", 8))
					.align_(\center)
					.string_("cc %".format(index));
				);
				padLabel.put(index, StaticText(padView, (size@15))
					.font_(Font("Monaco", 8))
					.align_(\center)
					.string_("note %".format(index));
				);
			};
			8.do{ |j|
				var index = j+(i*8)+1;
				knob.put(index, Knob(knobView, (size@size)));
				pad.put(index, Button(padView, (size@size))
					.states_([
						["", Color.black, Color.white],
						["", Color.black, Color.red]
					])
				);
			};
		};

		listView = ListView(infoView, Rect(5, 5, 190, 260))
		.font_(Font("Monaco", 12))
		.items_(
			17.collect{ |i|
				if( i == 0 ) {
					"Main Page"
				} {
					"Page %".format(i)
				}
			};
		);
		listView.selectedStringColor = Color.white;
		listView.stringColor = Color.hsv(0.7, 0.3, 0.7, 0.9);
		listView.hiliteColor = Color.hsv(0.7, 0.3, 0.7, 0.9);
		listView.selectionMode = \single;
		listView.colors = 17.collect{ |i|
			var colors = [Color.hsv(0.7, 0.3, 0.7, 0.2), Color.hsv(0.7, 0.1, 0.9, 0.1)];
			colors[i%2];
		};

		window.onClose_({ BeatStep.instance.gui = nil });


	}
}




































