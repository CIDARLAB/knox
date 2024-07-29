(function (root, factory) {
		"use strict";

		if (typeof exports === 'object') {
			module.exports = factory();
		} else if (typeof define === 'function' && define.amd) {
			define(factory);
		} else {
			root.Reservoir = factory();
		}
	}(this, function () {
		"use strict";

		// We use the same constant specified in [Vitt85]
		var switchToAlgorithmZConstant = 22;

		// `debug` was used to test for correctness of the more complicated
		// algorithms, X and Z, by comparing their results to baseline R
		var debug = "none";

		function _Reservoir(reservoirSize, randomNumberGen) {
			var rng = randomNumberGen || Math.random;

			// `reservoirSize` must be a number between 1 and 2^32
			var reservoirSize =
				Math.max(1, (Math.floor(reservoirSize) >> 0) || 1);

			// `totalItemCount` contains the total number of items 
			// processed by `pushSome` against the reservoir
			var totalItemCount = 0;

			// `numToSkip` is the total number of elements to skip over
			// before accepting one into the reservoir.
			var numToSkip = -1;

			// `currentAlgorithm` starts with algorithmX and switches to
			// algorithmZ after `switchThreshold` items is reached
			var currentAlgorithm = algorithmX;

			// `switchThreshold` is the `totalItemCount` at which to switch
			// over from algorithm X to Z
			var switchThreshold = 
				switchToAlgorithmZConstant * reservoirSize;

			if(debug === "R") {
				currentAlgorithm = algorithmR;
			} else if(debug === "X") {
				switchThreshold = Infinity;
			} else if(debug === "Z") {
				currentAlgorithm = algorithmZ;
			}

			// `algorithmXCount` is the number of items processed by algorithmX
			//  ie. the `totalItemCount` minus `reservoirSize`
			var algorithmXCount = 0;

			// `W` is used in algorithmZ
			var W = Math.exp(-Math.log(rng()) / reservoirSize);

			// `evictNext` is used only by algorithmR
			var evictNext = null;

			// `targetArray` is the array to be returned by Reservoir()
			var targetArray = [];

			targetArray.pushSome = function() {
				this.length = Math.min(this.length, reservoirSize);

				for(var i = 0; i < arguments.length; i++) {
					addSample.call(this, arguments[i]);
				}

				return this.length;
			};

			// `addSample` adds a single item at a time by using `numToSkip`
			// to determine whether to include it in the reservoir
			var addSample = function(sample) {
				// Prefill the reservoir if it isn't full
				if(totalItemCount < reservoirSize) {
					this.push(sample);
				} else {
					if(numToSkip < 0) {
						numToSkip = currentAlgorithm();
					}
					if(numToSkip === 0) {
						replaceRandomSample(sample, this);
					}
					numToSkip--;
				}
				totalItemCount++;
				return this;
			};

			// `replaceRandomSample` selects a single value from `reservoir`
			// for eviction and replaces it with `sample`
			function replaceRandomSample(sample, reservoir) {
				// Typically, the new sample replaces the "evicted" sample
				// but below we remove the evicted sample and push the
				// new value to ensure that reservoir is sorted in the
				// same order as the input data (ie. iterator or array).
				var randomIndex;
				if(evictNext !== null) {
					randomIndex = evictNext;
					evictNext = null;
				} else {
					randomIndex = Math.floor(rng() * reservoirSize);
				}
				reservoir.splice(randomIndex, 1);
				reservoir.push(sample);
			}

			// From [Vitt85], "Algorithm R"
			// Selects random elements from an unknown-length input.
			// Has a time-complexity of:
			//   O(N)
			// Number of random numbers required:
			//   N - n
			// Where:
			//   n = the size of the reservoir
			//   N = the size of the input
			function algorithmR() {
				var localItemCount = totalItemCount + 1,
				    randomValue = Math.floor(rng() * localItemCount),
				    toSkip = 0;

				while (randomValue >= reservoirSize) {
					toSkip++;
					localItemCount++;
					randomValue = Math.floor(rng() * localItemCount);
				}
				evictNext = randomValue;
				return toSkip;
			}

			// From [Vitt85], "Algorithm X"
			// Selects random elements from an unknown-length input.
			// Has a time-complexity of:
			//   O(N)
			// Number of random numbers required:
			//   2 * n * ln( N / n )
			// Where:
			//   n = the size of the reservoir
			//   N = the size of the input
			function algorithmX() {
				var localItemCount = totalItemCount,
				    randomValue = rng(),
				    toSkip = 0,
				    quotient;

				if (totalItemCount <= switchThreshold) {
					localItemCount++;
					algorithmXCount++;
					quotient = algorithmXCount / localItemCount;

					while (quotient > randomValue) {
						toSkip++;
						localItemCount++;
						algorithmXCount++;
						quotient = (quotient * algorithmXCount) / localItemCount;
					}
					return toSkip;
				} else {
					currentAlgorithm = algorithmZ;
					return currentAlgorithm();
				}
			}

			// From [Vitt85], "Algorithm Z"
			// Selects random elements from an unknown-length input.
			// Has a time-complexity of:
			//   O(n(1 + log (N / n)))
			// Number of random numbers required:
			//   2 * n * ln( N / n )
			// Where:
			//   n = the size of the reservoir
			//   N = the size of the input
			function algorithmZ() {
				var term = totalItemCount - reservoirSize + 1,
				    denom,
				    numer,
				    numer_lim;

				while(true) {
					var randomValue = rng();
					var x = totalItemCount * (W - 1);
					var toSkip = Math.floor(x);

					var subterm = ((totalItemCount + 1) / term);
					subterm *= subterm;
					var termSkip = term + toSkip;
					var lhs = Math.exp(Math.log(((randomValue * subterm) * termSkip)/ (totalItemCount + x)) / reservoirSize); 
					var rhs = (((totalItemCount + x) / termSkip) * term) / totalItemCount;

					if(lhs <= rhs) {
						W = rhs / lhs;
						break;
					}

					var y = (((randomValue * (totalItemCount + 1)) / term) * (totalItemCount + toSkip + 1)) / (totalItemCount + x);

					if(reservoirSize < toSkip) {
						denom = totalItemCount;
						numer_lim = term + toSkip;
					} else {
						denom = totalItemCount - reservoirSize + toSkip;
						numer_lim = totalItemCount + 1;
					}

					for(numer = totalItemCount + toSkip; numer >= numer_lim; numer--) {
						y = (y * numer) / denom;
						denom--;
					}

					W = Math.exp(-Math.log(rng()) / reservoirSize);

					if(Math.exp(Math.log(y) / reservoirSize) <= (totalItemCount + x) / totalItemCount) {
						break;
					}
				}
				return toSkip;
			}
			return targetArray;
		}

		return _Reservoir;

		// REFERENCES
		// [Vitt85] Vitter, Jeffery S. "Random Sampling with a Reservoir." ACM
		//          Transactions on Mathematical Software, Vol. 11, No. 1, March
		//          1985, pp. 37-57. Retrieved from
		//          http://www.cs.umd.edu/~samir/498/vitter.pdf
}));