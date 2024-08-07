SHELL := /bin/bash
# attempt to build using new process, has to be working by 2024-10-01
# if building for immediate installation on phone, use `make NEW_PROCESS=`
NEW_PROCESS ?= 1
ADB_TIMEOUT ?= 3  # seconds to wait if no device connected
REALLY ?= echo  # don't really run `make clean` or `make distclean`
ADB_COPY ?= 5  # seconds to wait for audio file copy if no device connected
APPNAME := $(notdir $(PWD))
PACKAGE := com.gnixl.$(APPNAME)
APPPATH := $(subst .,/,$(PACKAGE))
MINVER := 19
SDK := /usr/local/src/android/adt-bundle-linux-x86_64-20130717/sdk
PLATFORM := $(SDK)/platforms/android-$(MINVER)
ANDROID := $(PLATFORM)/android.jar
TOOLS := $(wildcard $(SDK)/build-tools/$(MINVER)*/)
# use Debian tools when available
DEBTOOLS := /usr/bin
# https://developer.android.com/tools/bundletool
# for building .aab files, Android App Bundles
BUNDLETOOL_JAR := $(HOME)/Downloads/bundletool-all-1.17.1.jar
AAPT ?= $(shell which $(DEBTOOLS)/aapt \
 $(TOOLS)/aapt \
 false 2>/dev/null | head -n 1)
AAPT2 ?= $(shell which $(DEBTOOLS)/aapt2 \
 $(TOOLS)/aapt2 \
 false 2>/dev/null | head -n 1)
KEYTOOL ?= $(shell which $(DEBTOOLS)/keytool \
 $(TOOLS)/keytool \
 false 2>/dev/null | head -n 1)
DX ?= $(shell which $(DEBTOOLS)/dx $(TOOLS)/dx false 2>/dev/null | head -n 1)
ZIPALIGN ?= $(shell which $(DEBTOOLS)/zipalign \
 $(TOOLS)/zipalign \
 false 2>/dev/null | head -n 1)
JAVA ?= $(shell which $(DEBTOOLS)/java \
 $(TOOLS)/java java false 2>/dev/null | head -n 1)
JAVAC ?= $(shell which $(DEBTOOLS)/javac \
 $(TOOLS)/javac javac false 2>/dev/null | head -n 1)
JARSIGNER ?= $(shell which $(DEBTOOLS)/jarsigner \
 $(TOOLS)/jarsigner jarsigner false 2>/dev/null | head -n 1)
# the following line MUST come after the JAVA definition above
BUNDLETOOL := $(JAVA) -jar $(BUNDLETOOL_JAR)
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
# new AAB build process
# https://developer.android.com/build/building-cmdline
AAB := bin/$(APPNAME).aab
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
KEYSTORE ?= $(HOME)/$(APPNAME)key.keystore
NEW_KEYSTORE ?= $(HOME)/$(APPNAME).keystore.p12
UPLOAD_KEYSTORE ?= $(HOME)/google_upload.keystore
ifneq ($(SHOWENV),)
export
else
# may need to export certain things for Makefile to work properly
endif
# new build process creates uninstallable (at least on my phone) APK
# using conditionals for use of two different `all` recipes doesn't work
# results are unpredictable
all: clean build reinstall copysingle $(AAB)
%.debug:
	@echo DEBUG:$*
build: $(DIRS) $(R) $(APK)
clean: .gitignore
	$(REALLY) rm -rf dummy $$(sed -n '/^#clean/,/^#distclean/{//!p;}' $<)
	@if [ "$(REALLY)" ]; then \
	 echo NOTE: '`$(MAKE) REALLY= $(MAKECMDGOALS)`' \
	  '(without the backticks) to bypass goofproofing' >&2; \
	 false; \
	fi
distclean: .gitignore clean
	$(REALLY) rm -rf dummy $$(sed -n '/^#distclean/,/^#end/{//!p;}' $<)
	@if [ "$(REALLY)" ]; then \
	 echo NOTE: '`$(MAKE) REALLY= $(MAKECMDGOALS)`' \
	  '(without the backticks) to bypass goofproofing' >&2; \
	 false; \
	fi
$(R): $(RESOURCES)
	$(AAPT) package $(DEBUG) -f -m \
	 --version-name $(VERSION) \
	 -J src \
	 -M $(MANIFEST) \
	 -S res \
	 -I $(ANDROID)
# must use older javac, otherwise error:
# com.android.dx.cf.iface.ParseException: bad class file magic (cafebabe)
# or version (0034.0000)
$(CLASSES): $(SOURCES)
	$(JAVAC) -d obj \
	 -source 1.7 \
	 -target 1.7 \
	 -classpath src \
	 -bootclasspath $(ANDROID) \
	 $+
bin/classes.dex: $(CLASSES)
	/bin/bash -x $(DX) \
	 --dex \
	 --output=$@ \
	 obj
bin/$(APPNAME).unsigned.apk: bin/classes.dex res_compiled $(MANIFEST)
ifeq ($(NEW_PROCESS),)
	@echo DEBUG:building $@ for immediate deployment
	$(AAPT) package -f -m $(DEBUG) \
	 --version-name $(VERSION) \
	 -F $@ \
	 -M $(MANIFEST) \
	 -S res \
	 -I $(ANDROID)
	cp $< .  # copy dex here temporarily
	$(AAPT) add $@ classes.dex
	rm $(<F)  # remove the copy
else
	@echo DEBUG:building $@ for Android App Bundle
	$(AAPT2) link --proto-format -o $@ \
	 -I $(ANDROID) \
	 --manifest $(MANIFEST) \
	 -R $(word 2, $+)/*.flat --auto-add-overlay
endif
$(AAB): $(AAB:=.unsigned)
	$(JARSIGNER) \
	 -verbose \
	 -sigalg SHA256withRSA \
	 -digestalg SHA256 \
	 -keystore $(KEYSTORE) \
	 -storepass $(APPNAME) \
	 -keypass $(APPNAME) \
	 $< $(APPNAME)  # final $(APPNAME) is for alias
	mv $< $@
$(AAB:=.unsigned): base.zip
	@echo DEBUG:building $@
	$(BUNDLETOOL) build-bundle --modules=$< --output=$@
	$(BUNDLETOOL) validate --bundle $@
	cp -i $@ $(USBKEY)
base.zip: base .FORCE
	rm -f $@
	@echo DEBUG:building $@
	cd $< && zip -r ../$@ .
	rm -rf $<
base: .FORCE
	@echo DEBUG:building $@
	# the following don't work as prerequisites and I don't understand
	# gmake well enough to fix it, so let's just `make` them.
	$(MAKE) clean
	$(MAKE) $(DIRS) $(R) bin/$(APPNAME).unsigned.apk bin/classes.dex
	# end of ugly hack
	rm -rf $@
	mkdir -p $@/manifest $@/dex
	unzip -d $@ bin/$(APPNAME).unsigned.apk
	mv $@/AndroidManifest.xml $@/manifest
	cp bin/classes.dex $@/dex/
	for directory in $$(find res/ -type d); do \
	 (cd $@ && mkdir -p $$directory); \
	done
	for file in $$(find $@ -maxdepth 0 -type f); do \
	 path=$$(find res/ -type f -name $$file); \
	 if [ "$$path" ]; then \
	  cp $$file $@/$(dir $$path)/; \
	 fi; \
	done
res_compiled: res
	@echo DEBUG:building $@
	mkdir -p $@
	$(AAPT2) compile $(shell find $</ -type f) -o $@/
edit: $(EDITABLE)
	vi $+
env:
ifneq ($(SHOWENV),)
	env | grep -v '^LS_COLORS'
else
	$(MAKE) SHOWENV=1 $@
endif
version:
	$(JAVA) -version
	$(JAVAC) -version
list:
	$(ZIPALIGN) -cv 4 $(APK)
keys: $(KEYSTORE)
$(KEYSTORE):
	@echo Enter password as: $(APPNAME)
	$(KEYTOOL) \
	 -genkeypair \
	 -validity 10000 \
	 -keystore $@ \
	 -alias $(APPNAME) \
	 -keyalg RSA \
	 -keysize 2048
$(AAB): $(wildcard res_compiled/*.flat)
$(APK:.apk=.signed.apk): $(APK:.apk=.unsigned.apk)
	@echo DEBUG:building signed apk
	$(JARSIGNER) \
	 -verbose \
	 -sigalg SHA256withRSA \
	 -digestalg SHA256 \
	 -keystore $(KEYSTORE) \
	 -storepass $(APPNAME) \
	 -keypass $(APPNAME) \
	 $< $(APPNAME)  # final $(APPNAME) is for alias
	mv $< $@
$(APK): $(APK:.apk=.signed.apk)
	rm -f $@
	$(ZIPALIGN) -f 4 $< $@
	cp -i $@ $(USBKEY)
tools:
	ls $(TOOLS)
install:
	-timeout $(ADB_TIMEOUT) $(ADB) install $(APK)
uninstall:
	-timeout $(ADB_TIMEOUT) $(ADB) uninstall $(PACKAGE)
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
exportkey: $(HOME)/appstore_upload_certificate.pem
$(HOME)/appstore_upload_certificate.pem:
	$(KEYTOOL) -export -rfc \
	 -keystore $(KEYSTORE) \
	 -alias $(APPNAME) \
	 -file $@
%.view: %.pem
	@echo viewing $< >&2
	-openssl x509 -in $< -noout -text
%.fp: %.pem
	@echo viewing fingerprint of $< >&2
	-openssl x509 -in $< -noout -fingerprint
certs: $(HOME)/appstore_upload_certificate.view \
 $(HOME)/studytimer_privkey_new.view $(HOME)/upload_cert.view
fps: $(HOME)/appstore_upload_certificate.fp \
 $(HOME)/studytimer_privkey_new.fp $(HOME)/upload_cert.fp
copysingle:
	@echo Copying "$(AUDIO)/$(FIRSTAUDIO)/$(SINGLEAUDIO)" to device
	-timeout $(ADB_COPY) $(ADB) shell mkdir -m 777 -p "$(STORAGE)" && \
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
# https://play.google.com/console/developers/8507177076018030452/
#  app/4971993077044930911/keymanagement
# Upload this to the above page under ^ Upload private key, step 4
# of ## Let Google Play manage your app signing key
newkeys: $(NEW_KEYSTORE)
$(NEW_KEYSTORE): $(KEYSTORE)
	@echo Enter both store password and key password as Google password
	$(KEYTOOL) \
	 -importkeystore \
	 -srckeystore $< \
	 -srcstorepass $(APPNAME) \
	 -srckeypass $(APPNAME) \
	 -srcalias $(APPNAME) \
	 -destalias $(APPNAME) \
	 -destkeystore $@ \
	 -deststoretype PKCS12
cert: $(HOME)/$(APPNAME).cert.pem
$(HOME)/$(APPNAME).cert.pem: $(NEW_KEYSTORE)
	@echo Enter both store password and key password as Google password
	openssl pkcs12 \
	 -in $< \
	 -nodes \
	 -nocerts \
	 -out $@
new_appsign_key: $(HOME)/studytimer_privkey_new.pem
$(HOME)/studytimer_privkey_new.pem:
	@echo 'keystore and key passwords same as before'
	$(JAVA) -jar $(USBKEY)/pepk.jar \
	 --keystore=$(KEYSTORE) \
	 --alias=$(APPNAME) \
	 --output=$@ \
	 --rsa-aes-encryption \
	 --encryption-key-path=$(USBKEY)/encryption_public_key.pem
$(UPLOAD_KEYSTORE):
	@echo 'Enter password(s) as google pass'
	$(KEYTOOL) \
	 -genkeypair \
	 -validity 10000 \
	 -keystore $@ \
	 -alias upload \
	 -keyalg RSA \
	 -keysize 2048
upload_cert: $(HOME)/upload_cert.pem
$(HOME)/upload_cert.pem: $(UPLOAD_KEYSTORE)
	@echo 'Enter password(s) as google pass'
	$(KEYTOOL) \
	 -export \
	 -rfc \
	 -keystore $(UPLOAD_KEYSTORE) \
	 -alias upload \
	 -file $@
