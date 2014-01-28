/*
Little class to render terminal voices into a temp folder (or a given path) that can be loaded as buffer.
Subclass SpeechBuffer calls the render and loads the buffer automatically.
Use render2 if you have the SpeechClI tool ("https://github.com/rukano/SpeechCLI") for extra parameters.

WARNING: Files have a 22050Hz sample rate!

Usage:

// the render method works synchronous:
b = SpeechRender("something to say").render(\Alex).asBuffer(s);
b.play

// the render2 methos seems not to be...
b = SpeechRender("slowl").render2("Alex", 10, 30, 100).asBuffer(s);
b.play // ERROR!

// the go like this...
a = SpeechRender("slowly saying it").render2("Alex", 10, 30, 100);
b = a.asBuffer(s)
b.play


2010 + 2014
-rukano-
*/

SpeechRender {

	classvar <>voices, <>defaultVoice;
	classvar <>tempDir, <>tempPrefix;
	classvar <>speechCLIexec;

	var <>cmd, <>filePath, <>string;

	*initClass {
		voices = ();
		voices.all = [
			// 0..4
			'Agnes', 'Kathy', 'Princess', 'Vicki', 'Victoria',
			// 5..9
			'Bruce', 'Fred', 'Junior', 'Ralph', 'Alex',
			// 10..15
			'Albert', 'Bad News', 'Bahh', 'Bells', 'Boing', 'Bubbles',
			// 16..20
			'Cellos', 'Deranged', 'Good News', 'Hysterical', 'Pipe Organ',
			// 21..23
			'Trinoids', 'Whisper', 'Zarvox'
		];
		voices.male = ['Bruce', 'Fred', 'Junior', 'Ralph', 'Alex'];
		voices.female = ['Agnes', 'Kathy', 'Princess', 'Vicki', 'Victoria'];
		voices.others = [
			'Albert', 'Bad News', 'Bahh', 'Bells', 'Boing', 'Bubbles',
			'Cellos', 'Deranged', 'Good News', 'Hysterical', 'Pipe Organ',
			'Trinoids', 'Whisper', 'Zarvox'
		];

		defaultVoice = voices.male[0];
		tempPrefix = "temp_speech_";
		tempDir = thisProcess.platform.recordingsDir +/+ "SpeechRenderings";
		File.exists(tempDir).not.if { ("mkdir -p " + tempDir.escapeChar($ )).systemCmd	};

	}

	*new { |text|
		^super.new.init(text);
	}

	*cleanUpDir {
		("rm" + (tempDir.escapeChar($ )) +/+ tempPrefix ++ "*").systemCmd;
	}

	init { |text|
		string = text;
	}

	render2 { |voice, rate, pitch, mod, path|
		if( speechCLIexec.isNil ) { "Set up the executable path to SpeechCLI tool".warn; ^this };
		cmd = speechCLIexec;
		if( voice.isNil.not ) {
			(voice.class == Integer).if { voice = voices.all[voice] };
			cmd = cmd + "-v" + voice;
		};
		if( rate.isNil.not ) { cmd = cmd + "-r" + rate };
		if( pitch.isNil.not ) { cmd = cmd + "-p" + pitch };
		if( mod.isNil.not ) { cmd = cmd + "-m" + mod };
		if( path.isNil ) {
			filePath = tempDir +/+ tempPrefix ++ Date.localtime.stamp ++ ".aiff"
		}{
			filePath = path;
		};
		cmd = cmd + "-o" + (filePath.escapeChar($ )) + string.quote;
		cmd.postln.systemCmd;
		^this
	}

	render { |voice, opt, path|
		// start the command
		cmd = "say";

		// add the voice - sybols and strings pass through
		(voice.isNil).if { voice = defaultVoice };
		(voice.class == Integer).if { voice = voices.all[voice] };

		cmd = cmd + "-v" + voice.asString;

		// add more options
		opt.isNil.if { opt = "" };
		cmd = cmd + opt;

		// add output file path
		path.isNil.if {
			filePath = tempDir +/+ tempPrefix ++ Date.localtime.stamp ++ ".aiff"
		} {
			filePath = path
		};

		// cmd is ready!!!
		cmd = cmd + "-o" + (filePath.escapeChar($ )) + string;

		cmd.systemCmd;
		^this
	}

	asBuffer { |server|
		server.isNil.if { server = Server.default };
		^Buffer.read(server, filePath)
	}
}