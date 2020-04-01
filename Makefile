PATH := /usr/lib/jvm/java-8-openjdk-amd64/bin:$(PATH)
MINVER := 19
SDK := /usr/local/src/android/adt-bundle-linux-x86_64-20130717/sdk
ANDROID := $(SDK)/platforms/android-$(MINVER)/android.jar
TOOLS := $(wildcard $(SDK)/build-tools/$(MINVER)*/)
PACKAGE := $(notdir $(PWD))
SOURCES = $(wildcard src/com/jcomeau/$(PACKAGE)/*.java)
CLASSES = $(subst .java,.class,$(subst src/,obj/,$(SOURCES)))
APK := bin/$(PACKAGE).apk
export
build: src/com/jcomeau/$(PACKAGE)/R.java $(APK)
clean:
	rm -rf src/com/jcomeau/$(PACKAGE)/R.java
src/com/jcomeau/$(PACKAGE)/R.java:
	$(TOOLS)/aapt package -f -m \
	 -J src \
	 -M AndroidManifest.xml \
	 -S res \
	 -I $(ANDROID)
$(CLASSES): $(SOURCES)
	javac -d obj \
	 -source 1.6 \
	 -target 1.7 \
	 -classpath src \
	 -bootclasspath $(ANDROID) \
	 $+
bin/classes.dex: $(CLASSES)
	strace -f -o/tmp/dx.log $(TOOLS)/dx \
	 --dex \
	 --output=$@ \
	 obj
bin/$(PACKAGE).unaligned.apk: bin/classes.dex
	$(TOOLS)/aapt package -f -m \
	 -F $@ \
	 -M AndroidManifest.xml \
	 -S res \
	 -I $(ANDROID)
	cp $< .
	$(TOOLS)/aapt add bin/$(PACKAGE).unaligned.apk classes.dex
	rm $(<F)
edit: $(SOURCES)
	vi $+
env:
	env
version:
	java -version
list:
	$(TOOLS)/aapt list bin/$(PACKAGE).unaligned.apk
keys:
	@echo Enter password as: $(PACKAGE)
	keytool \
	 -genkeypair \
	 -validity 365 \
	 -keystore $(HOME)/$(PACKAGE)key.keystore \
	 -keyalg RSA \
	 -keysize 2048
$(APK): $(APK:.apk=.unsigned.apk)
	@echo Enter password as: $(PACKAGE)
	jarsigner \
	 -verbose \
	 -sigalg SHA1withRSA \
	 -digestalg SHA1 \
	 -keystore $(HOME)/$(PACKAGE)key.keystore \
	 $@ mykey
%.unsigned.apk: %.unaligned.apk
	$(TOOLS)/zipalign -f 4 $< $@
tools:
	ls $(TOOLS)
install:
	adb install $(APK)
test:
	adb shell am start -n com.jcomeau.$(PACKAGE)/.MainActivity
