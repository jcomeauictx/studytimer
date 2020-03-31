MINVER := 19
SDK := /usr/local/src/android/adt-bundle-linux-x86_64-20130717/sdk
build: src/com/jcomeau/studytimer/R.java classes
clean:
	rm -rf src/com/jcomeau/studytimer/R.java
src/com/jcomeau/studytimer/R.java:
	cd $(SDK) && \
	 ./build-tools/$(MINVER)*/aapt package -f -m \
	  -J $(PWD)/src \
	  -M $(PWD)/AndroidManifest.xml \
	  -S $(PWD)/res \
	  -I $(SDK)/platforms/android-19/android.jar
classes: src/com/jcomeau/studytimer/*.java
	javac -d obj \
	 -classpath src \
	 -bootclasspath $(SDK)/platforms/android-19/android.jar \
	 $+
