SRC_DIR = src/
BUILD_DIR = build/
MAIN_CLASS = PeerProcess

build:
	cd ${SRC_DIR} && javac -d ../${BUILD_DIR} *.java

run:
	cd ${BUILD_DIR} && java ${MAIN_CLASS} ${PEER} 

clean:
	rm -rf ${BUILD_DIR}
