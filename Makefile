PROJECT_NAME = atypon-exercise
MAIN_CLASS = com.atypon.exercise.App
TARGET_DIR = target
SRC_DIR = src

build:
	@mvn compile

clean:
	@mvn clean

run:
	@java -cp $(TARGET_DIR)/classes $(MAIN_CLASS)