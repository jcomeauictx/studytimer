MINVER := 19
SDK := /usr/local/src/android/adt-bundle-linux-x86_64-20130717/sdk
ANDROID := $(SDK)/platforms/android-$(MINVER)/android.jar
SOURCES = $(wildcard src/com/jcomeau/studytimer/*.java)
export
build: src/com/jcomeau/studytimer/R.java classes
clean:
	rm -rf src/com/jcomeau/studytimer/R.java
src/com/jcomeau/studytimer/R.java:
	cd $(SDK) && \
	 ./build-tools/$(MINVER)*/aapt package -f -m \
	  -J $(PWD)/src \
	  -M $(PWD)/AndroidManifest.xml \
	  -S $(PWD)/res \
	  -I $(ANDROID)
classes: $(SOURCES)
	javac -d obj \
	 -source 1.6 \
	 -target 1.8 \
	 -classpath src \
	 -bootclasspath $(ANDROID) \
	 $+
edit: $(SOURCES)
	vi $+
env:
	env
