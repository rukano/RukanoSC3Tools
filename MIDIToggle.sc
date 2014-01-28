MIDIToggle {
	// creates a notOn/noteOff MIDIdef pair
	*new{ |key, funcOn, funcOff, note, chan, permanent=true|
		var names = this.getNames(key);
		var funcs = [funcOn, funcOff];
		[\noteOn, \noteOff].do{ |type, i|
			MIDIdef(names[i], funcs[i], note, chan, type).permanent_(permanent)
		};
		^nil
	}

	*free { |key|
		this.getNames(key).do{ |key| MIDIdef(key).free }
	}

	*getNames { |key|
		^[(key++"_on").asSymbol, (key++"_off").asSymbol];
	}
}


/*

MIDIToggle(\asdf, {}, {}, 34)

MIDIdef.all

*/
