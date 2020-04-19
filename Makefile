APPNAME := $(notdir $(PWD))
PACKAGE := com.jcomeau.$(APPNAME)
APPPATH := src/$(subst .,/,$(PACKAGE))
MINVER := 19
SDK := /usr/local/src/android/adt-bundle-linux-x86_64-20130717/sdk
ANDROID := $(SDK)/platforms/android-$(MINVER)/android.jar
TOOLS := $(wildcard $(SDK)/build-tools/$(MINVER)*/)
PATH := /usr/lib/jvm/java-8-openjdk-amd64/bin:$(TOOLS):$(PATH)
R := $(APPPATH)/R.java
SOURCES = $(wildcard $(APPPATH)/*.java)
CLASSES = $(subst .java,.class,$(subst src/,obj/,$(SOURCES)))
RESOURCES := $(wildcard res/*/*)
RAW := $(wildcard res/*/*.mp3)
MANIFEST := AndroidManifest.xml
OTHER := README.md
EDITABLE := $(filter-out $(RAW), $(filter-out $(R), \
	     $(SOURCES)) $(RESOURCES) $(MANIFEST)) $(OTHER)
APK := bin/$(APPNAME).apk
DIRS := obj bin res/drawable libs
TIMESTAMP ?= $(shell date +%Y%m%d%H%M%S)
DEBUG ?= --debug-mode
export
all: rebuild reinstall
rebuild: clean build
build: $(DIRS) $(R) $(APK)
clean:
	rm -rf $(R) $(DIRS)
$(APPPATH)/R.java: $(RESOURCES)
	$(TOOLS)/aapt package $(DEBUG) -f -m \
	 -J src \
	 -M $(MANIFEST) \
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
bin/$(APPNAME).unaligned.apk: bin/classes.dex $(MANIFEST)
	$(TOOLS)/aapt package -f -m $(DEBUG) \
	 -F $@ \
	 -M $(MANIFEST) \
	 -S res \
	 -I $(ANDROID)
	cp $< .
	$(TOOLS)/aapt add bin/$(APPNAME).unaligned.apk classes.dex
	rm $(<F)
edit: $(EDITABLE)
	vi $+
env:
	env
version:
	java -version
list:
	$(TOOLS)/aapt list bin/$(APPNAME).unaligned.apk
keys:
	@echo Enter password as: $(APPNAME)
	keytool \
	 -genkeypair \
	 -validity 365 \
	 -keystore $(HOME)/$(APPNAME)key.keystore \
	 -keyalg RSA \
	 -keysize 2048
$(APK): $(APK:.apk=.unsigned.apk)
	@echo Enter password as: $(APPNAME)
	jarsigner \
	 -verbose \
	 -sigalg SHA1withRSA \
	 -digestalg SHA1 \
	 -keystore $(HOME)/$(APPNAME)key.keystore \
	 $< mykey
	mv $< $@
%.unsigned.apk: %.unaligned.apk
	$(TOOLS)/zipalign -f 4 $< $@
tools:
	ls $(TOOLS)
install:
	adb install $(APK)
uninstall:
	adb uninstall $(PACKAGE)
reinstall: uninstall install
test:
	adb shell am start -n $(PACKAGE)/.MainActivity
$(APPPATH) $(DIRS):
	mkdir -p $@
mp3find:
	adb shell 'find / -name "*.mp3" 2>/dev/null'
studytimer: .FORCE
	[ -d $@ ] && mv -f $@ /tmp/$@.$(TIMESTAMP) || true
	apktool d bin/studytimer.apk
	[ -d /tmp/$@.$(TIMESTAMP) ] && diff -r $@ /tmp/$@.$(TIMESTAMP) || true
.FORCE:
shell:
	bash -i
