/*
 * Class for handing relative control inputs
 * Made specially for working with the Arturia BeatStep
 * to multiply the increment when the signal is faster
 * rukano 2014
 */

RelativeControl {
	var <>minValue, <>maxValue, <>roundFactor;
	var <>maxMultiplier, <>minDiff, <>maxDiff;
	var <>controlBus, <>currentValue;
	var <>lastTime, <>multiplier;

	*new { |min=0, max=1, default=0|
		^super.new.init(min, max, default)
	}

	init { |min, max, default|
		minValue = min;
		maxValue = max;
		maxMultiplier = 16;
		minDiff = 0.003;
		maxDiff = 0.01;
		roundFactor = 0;

		multiplier = 1;
		currentValue = default;
		lastTime = Main.elapsedTime;
	}

	add { |step=0.1|
		var currentTime = Main.elapsedTime;
		var diff = currentTime - lastTime;
		lastTime = currentTime;
		multiplier = diff.linlin(minDiff,maxDiff,maxMultiplier,1);
		currentValue = (currentValue + (step * multiplier)).clip(minValue, maxValue).round(roundFactor);
		if( controlBus.notNil ) { controlBus.set(currentValue) };
//		[currentValue, multiplier, diff].postln;
		^currentValue;
	}

	set { |value|
		currentValue = value;
		if( controlBus.notNil ) { controlBus.set(currentValue) };
	}

	bus {
		controlBus = controlBus ?? { Bus.control(Server.default, 1) };
		controlBus.set(currentValue);
		^controlBus
	}

	kr { |lag=0.1, mul=1, add=0|
		if( controlBus.isNil ) { this.bus };
		^MulAdd(controlBus.kr, mul, add).lag(lag)
	}

	free {
		controlBus.free;
	}

}