PATH := /usr/lib/jvm/java-8-openjdk-amd64/bin:$(PATH)
MINVER := 19
SDK := /usr/local/src/android/adt-bundle-linux-x86_64-20130717/sdk
ANDROID := $(SDK)/platforms/android-$(MINVER)/android.jar
TOOLS := $(wildcard $(SDK)/build-tools/$(MINVER)*/)
PACKAGE := $(notdir $(PWD))
SOURCES = $(wildcard src/com/jcomeau/$(PACKAGE)/*.java)
export
build: src/com/jcomeau/$(PACKAGE)/R.java classes dex package
clean:
	rm -rf src/com/jcomeau/$(PACKAGE)/R.java
src/com/jcomeau/$(PACKAGE)/R.java:
	$(TOOLS)/aapt package -f -m \
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
package:
	$(TOOLS)/aapt package -f -m \
	 -F $(PWD)/bin/$(PACKAGE).unaligned.apk \
	 -M $(PWD)/AndroidManifest.xml \
	 -S $(PWD)/res \
	 -I $(ANDROID)
	cp $(PWD)/bin/classes.dex .
	$(TOOLS)/aapt add $(PWD)/bin/$(PACKAGE).unaligned.apk classes.dex
	rm classes.dex
edit: $(SOURCES)
	vi $+
env:
	env
version:
	java -version
list:
	$(TOOLS)/aapt list $(PWD)/bin/$(PACKAGE).unaligned.apk
