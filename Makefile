.PHONY: setup build adbinstall release lint detekt test clean

# One pinned, patched extractor is used by local builds and CI.
NEWPIPEEXTRACTOR_REPO := https://github.com/TeamNewPipe/NewPipeExtractor
NEWPIPEEXTRACTOR_TAG := v0.26.3
NEWPIPEEXTRACTOR_REF := caae86c943857cc6e1a762e3488d6a14e9cf7800
NEWPIPEEXTRACTOR_DIR := build/newpipeextractor
NEWPIPEEXTRACTOR_STAMP := $(NEWPIPEEXTRACTOR_DIR)/.a1swaara-patched
PATCHES := patches/newpipeextractor.patch patches/newpipeextractor-android10.patch

setup: $(NEWPIPEEXTRACTOR_STAMP)

build: setup
	NEWPIPE_EXTRACTOR_DIR=$(NEWPIPEEXTRACTOR_DIR) ./gradlew :app:assembleHttp3Debug

adbinstall: setup
	NEWPIPE_EXTRACTOR_DIR=$(NEWPIPEEXTRACTOR_DIR) ./gradlew :app:installHttp3Debug

release: setup
	NEWPIPE_EXTRACTOR_DIR=$(NEWPIPEEXTRACTOR_DIR) ./gradlew --no-daemon :app:assembleHttp3Release :app:assembleHttp2OnlyRelease

lint: setup
	NEWPIPE_EXTRACTOR_DIR=$(NEWPIPEEXTRACTOR_DIR) ./gradlew lint

detekt: setup
	NEWPIPE_EXTRACTOR_DIR=$(NEWPIPEEXTRACTOR_DIR) ./gradlew detekt

test: setup
	NEWPIPE_EXTRACTOR_DIR=$(NEWPIPEEXTRACTOR_DIR) ./gradlew :app:testHttp3DebugUnitTest :app:testHttp2OnlyDebugUnitTest

$(NEWPIPEEXTRACTOR_STAMP): $(PATCHES) Makefile
	@set -eu; \
	  mkdir -p build; \
	  rm -rf $(NEWPIPEEXTRACTOR_DIR).tmp; \
	  trap 'rm -rf $(NEWPIPEEXTRACTOR_DIR).tmp' EXIT; \
	  git clone --depth 1 --branch $(NEWPIPEEXTRACTOR_TAG) $(NEWPIPEEXTRACTOR_REPO) $(NEWPIPEEXTRACTOR_DIR).tmp; \
	  test "$$(git -C $(NEWPIPEEXTRACTOR_DIR).tmp rev-parse HEAD)" = "$(NEWPIPEEXTRACTOR_REF)"; \
	  for patch in $(PATCHES); do git -C $(NEWPIPEEXTRACTOR_DIR).tmp apply "../../$$patch"; done; \
	  touch $(NEWPIPEEXTRACTOR_DIR).tmp/.a1swaara-patched; \
	  rm -rf $(NEWPIPEEXTRACTOR_DIR); \
	  mv $(NEWPIPEEXTRACTOR_DIR).tmp $(NEWPIPEEXTRACTOR_DIR)

clean:
	rm -rf build
