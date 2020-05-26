# Study Timer

This is a work-in-progress mainly for my own study of law at <https://nwculaw.edu>. Hopefully it will be useful to others as well, once I get it working.

# Goals

* nag every .1 hour to see if I'm still studying
* save study time per course per week
* play audio files associated with study courses and keep track of time

# Usage

* Use the `make allaudio` recipe to copy MP3 files for your courses to
  your device. The Makefile will almost certainly need to be edited with
  the correct locations for source and destination.
* The audio files need to be in a directory structure matching
  Year/CourseName/`*`.mp3.
* To prevent audible glitches for when the audio content is being played
  while moving with the phone, pulse the power button to turn off the screen.
  This way, screen rotation will not result in killing and restarting the
  Activity.
* You can alternate between "Study" and "Listen" indefinitely, with both
  activities adding to the accumulated time for the selected course.
