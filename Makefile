.PHONY: all build clean assembly compile test sbt

PREFIX_PATH = $$(pwd)/target
CONF_PATH   = $$(pwd)/project
IVY_PATH    = $(CONF_PATH)/ivy2
MAVEN_PATH  = $(CONF_PATH)/github/maven
BOOT_PATH   = $(CONF_PATH)/boot
GEM_PATH    = $(PREFIX_PATH)/gems
LIB_PATH    = $(PREFIX_PATH)/lib
ENV         = TMPDIR=$(PREFIX_PATH) LD_LIBRARY_PATH=$(LIB_PATH) DYLD_LIBRARY_PATH=$(LIB_PATH)
SBT         = env $(ENV) ./sbt

all: build

build: target assembly

clean:
	rm -rf $(PREFIX_PATH)
	rm -rf .sbt
	rm -rf $(MAVEN_PATH)
	cd project && rm -rf project target boot ivy2
	cd core && rm -rf project target
	cd example && rm -rf project target

target:
	mkdir -p $(PREFIX_PATH) $(GEM_PATH) $(LIB_PATH) $(IVY_PATH) $(BOOT_PATH) $(MAVEN_PATH)

assembly: target
	$(SBT) +assembly

compile:
	$(SBT) +compile

test:
	$(SBT) +test

sbt: target
	$(SBT)
