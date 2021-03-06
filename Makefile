APPNAME := $(notdir $(PWD))
PACKAGE := com.jcomeau.$(APPNAME)
APPPATH := $(subst .,/,$(PACKAGE))
MINVER := 19
SDK := /usr/local/src/android/adt-bundle-linux-x86_64-20130717/sdk
ANDROID := $(SDK)/platforms/android-$(MINVER)/android.jar
TOOLS := $(wildcard $(SDK)/build-tools/$(MINVER)*/)
# MUST use java 7 with this version of Android tools!
#PATH := /usr/lib/jvm/java-8-openjdk-amd64/bin:$(TOOLS):$(PATH)
PATH := /usr/local/src/jdk1.7.0_80/bin:$(TOOLS):$(PATH)
R := src/$(APPPATH)/R.java
SOURCES = $(wildcard src/$(APPPATH)/*.java)
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
DEBUG ?= #--debug-mode
VERSION := git-$(shell git describe --always 2>/dev/null)
# USBKEY is mount point of nwculaw.edu USB key, or to where it was copied
USBKEY ?= $(HOME)/Downloads
# SDCARD is your phone's external data directory (even if really internal)
SDCARD ?= /sdcard
SCHOOL ?= nwculaw.edu
YEAR ?= 1
DEVICE ?=  # make DEVICE="-t1" to specify device (adb devices -l to list)
ADB := adb $(DEVICE)
AUDIO := $(wildcard $(USBKEY)/$(YEAR)*MP3)
FIRSTAUDIO ?= $(notdir $(shell cd "$(AUDIO)" && find . -type d | sed -n 2p))
SINGLEAUDIO := $(notdir $(shell cd "$(AUDIO)"/"$(FIRSTAUDIO)" && ls 02*))
STORAGE := $(SDCARD)/Android/data/$(PACKAGE)/files/$(SCHOOL)/$(YEAR)
export
all: rebuild reinstall copysingle
rebuild: clean build
build: $(DIRS) $(R) $(APK)
clean:
	rm -rf $(R) $(DIRS)
src/$(APPPATH)/R.java: $(RESOURCES)
	$(TOOLS)/aapt package $(DEBUG) -f -m \
	 --version-name $(VERSION) \
	 -J src \
	 -M $(MANIFEST) \
	 -S res \
	 -I $(ANDROID)
# must use older javac, otherwise error:
# com.android.dx.cf.iface.ParseException: bad class file magic (cafebabe)
# or version (0034.0000)
$(CLASSES): $(SOURCES)
	javac -d obj \
	 -source 1.7 \
	 -target 1.7 \
	 -classpath src \
	 -bootclasspath $(ANDROID) \
	 $+
bin/classes.dex: $(CLASSES)
	/bin/bash -x $(TOOLS)/dx \
	 --dex \
	 --output=$@ \
	 obj
bin/$(APPNAME).unsigned.apk: bin/classes.dex $(MANIFEST)
	$(TOOLS)/aapt package -f -m $(DEBUG) \
	 --version-name $(VERSION) \
	 -F $@ \
	 -M $(MANIFEST) \
	 -S res \
	 -I $(ANDROID)
	cp $< .  # copy dex here temporarily
	$(TOOLS)/aapt add $@ classes.dex
	rm $(<F)  # remove the copy
edit: $(EDITABLE)
	vi $+
env:
	env
version:
	java -version
list:
	$(TOOLS)/zipalign -cv 4 $(APK)
keys:
	@echo Enter password as: $(APPNAME)
	keytool \
	 -genkeypair \
	 -validity 10000 \
	 -keystore $(HOME)/$(APPNAME)key.keystore \
	 -alias $(APPNAME) \
	 -keyalg RSA \
	 -keysize 2048
$(APK:.apk=.signed.apk): $(APK:.apk=.unsigned.apk)
	@echo Enter password as: $(APPNAME)
	jarsigner \
	 -verbose \
	 -sigalg SHA1withRSA \
	 -digestalg SHA1 \
	 -keystore $(HOME)/$(APPNAME)key.keystore \
	 $< $(APPNAME)
	mv $< $@
$(APK): $(APK:.apk=.signed.apk)
	rm -f $@
	$(TOOLS)/zipalign -f 4 $< $@
	cp -i $@ ~/Downloads/
tools:
	ls $(TOOLS)
install:
	$(ADB) install $(APK)
uninstall:
	$(ADB) uninstall $(PACKAGE)
reinstall: uninstall install
test:
	$(ADB) shell am start -n $(PACKAGE)/.MainActivity
src/$(APPPATH) $(DIRS) $(HOME)/etc/ssl:
	mkdir -p $@
mp3find:
	$(ADB) shell 'find / -name "*.mp3" 2>/dev/null'
studytimer: .FORCE
	[ -d $@ ] && mv -f $@ /tmp/$@.$(TIMESTAMP) || true
	apktool d bin/studytimer.apk
	[ -d /tmp/$@.$(TIMESTAMP) ] && diff -r $@ /tmp/$@.$(TIMESTAMP) || true
.FORCE:
shell:
	bash -i
exportkey: $(HOME)/etc/ssl
	keytool -export -rfc \
	 -keystore $(HOME)/$(APPNAME)key.keystore \
	 -alias $(APPNAME) \
	 -file $(HOME)/etc/ssl/appstore_upload_certificate.pem
copysingle:
	@echo Copying "$(AUDIO)/$(FIRSTAUDIO)/$(SINGLEAUDIO)" to device
	$(ADB) shell mkdir -m 777 -p "$(STORAGE)"
	$(ADB) push "$(AUDIO)"/"$(FIRSTAUDIO)"/"$(SINGLEAUDIO)" \
		"$(STORAGE)"/"$(FIRSTAUDIO)"/"$(SINGLEAUDIO)"
copyaudio:
	@echo Copying mp3 files from "$(AUDIO)/$(FIRSTAUDIO)" to device
	$(ADB) shell mkdir -m 777 -p "$(STORAGE)"
	$(ADB) push "$(AUDIO)"/"$(FIRSTAUDIO)" "$(STORAGE)"/"$(FIRSTAUDIO)"
allaudio:
	@echo Copying mp3 files from USB key to device
	for directory in "$(AUDIO)"/*; do \
	 echo Copying audio from "$$directory" to phone; \
	 $(ADB) shell mkdir -m 777 -p "$(STORAGE)"; \
	 $(ADB) push "$$directory" "$(STORAGE)"/"$$(basename "$$directory")"; \
	done
/tmp/$(APPNAME).log: .FORCE
	timeout 2m $(ADB) logcat | grep $(APPNAME) > $@ &
log: /tmp/$(APPNAME).log
logcat:
	$(ADB) $@ | sed -n '/BufferQueueProducer/n; /$(APPNAME)/p'
classes:
	ls "$(AUDIO)"
