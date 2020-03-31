PATH := /usr/lib/jvm/java-8-openjdk-amd64/bin:$(PATH)
MINVER := 19
SDK := /usr/local/src/android/adt-bundle-linux-x86_64-20130717/sdk
ANDROID := $(SDK)/platforms/android-$(MINVER)/android.jar
TOOLS := $(wildcard $(SDK)/build-tools/$(MINVER)*/)
SOURCES = $(wildcard src/com/jcomeau/studytimer/*.java)
export
build: src/com/jcomeau/studytimer/R.java classes dex
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
	 -target 1.7 \
	 -classpath src \
	 -bootclasspath $(ANDROID) \
	 $+
dex:
	strace -f -o/tmp/dx.log $(TOOLS)/dx \
	 --dex \
	 --output=$(PWD)/bin/classes.dex \
	 $(PWD)/obj
edit: $(SOURCES)
	vi $+
env:
	env
version:
	java -version
