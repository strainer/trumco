Trumco is a Java command which can generate high quality spectrograms. 

It was developed as a personal programming exercise, and to research waveform
analysis with very little knowledge of how spectrograms are traditionally
computed or understood. As a result of its experimental nature Trumco provides
uncommon configuration options, and can achieve visually striking levels of
plot detail. 

In addition to the normal spectra displayed in green and blue channels, Trumco
renders 'solvency' in red, which was first implemented as a test channel and
is included as a curiosity and visual depth enhancement over the standard
spectrogram. 
Technically, red values are high when the sinusoid which is indicated by the 
greenblue, value reduces the waveform complexity by the same
measure of its own complexity when subtracted from the waveform:
```
red= greenblue //
	*( integral(abs(derivative(waveform)))
		-integral(abs(derivative(waveform-sinusoid))) )
	/( integral(abs(derivative(sinusoid))) );
```	

Trumco is different in permitting transform windows of arbitrary length
from 1 to about 100,000 samples, and output image sizes from 1 by 1 to over
10,000 by 10,000 pixels. However, it does employ a lot of working memory,
especially to produce big images with big transform windows. 

The 2 main factors of working memory demand are image height and transform
window size. A 1000 pixel high plot, with 10,000 sample window requires approx
10 * 1000 * 10,000 bytes = ~100 Megabytes of system RAM to run. Output plot
length has relatively little effect on required RAM but is a factor of plot
generation time. 

Trumcos performance is not shabby, but very large plots with long windows may
take some time to complete, perhaps minutes. On such occasions, to relieve the
suspense, an estimate of plot generation time is reported. 
 - - - - - - - - - - - - - - - - - - - - - - -

Usage
=====

Trumco runs from a command window or script where normal considerations to the
locations of the Java interpreter, trumco.jar and the audio source file apply.

The simplest command to run trumco would be:
java trumco.class sourcetrack.wav 

It is possible for switches to conflict, for example -aplts, -lptim and -tprpl
all set time resolution in different ways.  

Switches
========

_Plot_Appearance_

-pltht n
* set plot height in pixels 

-pltln n
* set plot length in pixels 

-ovlap n
* set plot overlap in pixels, for multiple output plots.
* (to improve readability of plot sequences). 

-noaxt
* dont draw time axis 

-noaxf
* dont draw frequency axis 

-fiwvl
* label frequency in wavelengths instead of Hertz 

-logfs
* plot frequency axis as logarithmic progression instead of linear.
* ( tends to overplot lower frequencies and sparsely plot highs ) 

-gamma n
* applies gamma adjustment to plot rendering
* (can improve appearance but distorts measurements)
* If set to 1, green and blue pixel values of 255 will indicate a potential sinusoidal component with amplitude equal or exceeding -mxamp. Pixel values under 255 will indicate an amplitude of mxamp*value/255 

_Time-scale_setters_

-aplts n
* 'autoplots' sets measure leap (time resolution) automatically to produce the specified number of plots for given audio file or a specified time range within file. 

-tprpl n
* 'time per plot', auto sets measureleap to fit time per plot (in millisecs) 

-lpsmp n
* 'leap samples' Number of samples progressed for every measure.
* (This sets the time-scale of the horizontal axis) 

-lptim n
* same as -lpsmp, expressed in millisecs. (millisecs per pixel) 

-rleap n
* round 'measure leap' down to whole sample length, (fwiw). 

_Plot_Range_

-dursp n
* duration to plot in millisecs 

-stspc n
* start plot at n millisecs 

-fnspc n
* finish plot at n millisecs 

_Measurement_Parameters_

-mxamp n
* maximum amplitude to expect in audio,
* affects brightness of spectrogram. 

-pxamp n
* precalculate maximum amplitude and adjust by factor n.
* plot range is pre-passed to estimate maximum amplitude before plotting, and optionally adjusted by factor n.
* e.g. "-pxamp 0.5" sets mxamp to 0.5 * maximum amplitude estimated by pre-pass. 

-minwv n
* maximum hertz to render, expressed in wavelength as samples 

-maxhz n
* same as minwv, expressed in hertz 

-maxwv n
* minimum hertz to render, expressed in wavelength as samples 

-minhz n
* same as maxwv, expressed in hertz 

-mrsmp n
* set size of measurement window in samples.
* analogous to a transform window length (number of samples 'heard' in one pixel) 

-mrtim n
* same as mrsmp only expressed in milliseconds. 

_Channel_Selection_

-chnnl n
* for stereo tracks, sets channel to render ( 0 or 1 ) 

-chnnl 2
* for stereo tracks, downmixes channels. 

-chnnl 3
* for stereo tracks, renders the difference between channels 

_Other_switches_

-rawfl [samplerate] [channels] [bytes per sample] [endian]
* to read as raw pcm, this switch and details must follow the filename in order:- any samplerate stated in hertz, 1|2 channels, 1|2 bytes per channel, big endian word order is default, add 'e' for little endian.

* e.g. java -jar trumco.jar filename.aif -rawfl 44100 2 2 e -stspc 2

-namex ext
* adds an extension ext to output file names, to avoid overwrites.

* e.g. srcfileext.00.bmp, srcfileext.01.bmp, etc..

Examples
========
....
