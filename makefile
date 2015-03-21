JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java

CLASSES = \
        ChatClient.java \
        ChatClientHeartBeat.java \
        ChatClientReceiver.java \
        ChatServer.java \
        ClientWorker.java \
        Message.java \
        ServerWorker.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
		$(RM) *.class