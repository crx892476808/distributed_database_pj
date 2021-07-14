RMIREGPORT =3345 
PROJECTROOT = ..
PATH = /usr/bin
all : server client
DSTPATH = $(PROJECTROOT)/../bin
CLASSPATH = $(DSTPATH)/transaction
CONFPATH = $(PROJECTROOT)/../conf
vpath %.class $(DSTPATH)

server : $(CLASSPATH)/ResourceManager.class \
         $(CLASSPATH)/ResourceManagerImpl.class \
         $(CLASSPATH)/ResourceManagerImpl_Stub.class \
         $(CLASSPATH)/TransactionManager.class \
         $(CLASSPATH)/TransactionManagerImpl.class \
         $(CLASSPATH)/TransactionManagerImpl_Stub.class \
         $(CLASSPATH)/WorkflowController.class \
         $(CLASSPATH)/WorkflowControllerImpl.class \
         $(CLASSPATH)/WorkflowControllerImpl_Stub.class \
         $(CLASSPATH)/InvalidTransactionException.class \
         $(CLASSPATH)/TransactionAbortedException.class

client: $(CLASSPATH)/Client.class

runregistry :
	$(PATH)/rmiregistry -J-classpath -J$(DSTPATH) $(RMIREGPORT)

runtm : server
	$(PATH)/java -classpath $(DSTPATH) -DrmiPort=$(RMIREGPORT) -Djava.security.policy=./security-policy transaction.TransactionManagerImpl

runrmflights : server
	$(PATH)/java -classpath $(DSTPATH) -DrmiPort=$(RMIREGPORT) -DrmiName=RMFlights -Djava.security.policy=./security-policy transaction.ResourceManagerImpl

runrmrooms : server
	$(PATH)/java -classpath $(DSTPATH) -DrmiPort=$(RMIREGPORT) -DrmiName=RMRooms -Djava.security.policy=./security-policy transaction.ResourceManagerImpl

runrmcars : server
	$(PATH)/java -classpath $(DSTPATH) -DrmiPort=$(RMIREGPORT) -DrmiName=RMCars -Djava.security.policy=./security-policy transaction.ResourceManagerImpl

runrmcustomers : server
	$(PATH)/java -classpath $(DSTPATH) -DrmiPort=$(RMIREGPORT) -DrmiName=RMCustomers -Djava.security.policy=./security-policy transaction.ResourceManagerImpl

runwc : server
	$(PATH)/java -classpath $(DSTPATH) -DrmiPort=$(RMIREGPORT) -Djava.security.policy=./security-policy transaction.WorkflowControllerImpl

runclient : client
	$(PATH)/java -classpath $(DSTPATH) -DrmiPort=$(RMIREGPORT) transaction.Client

clean :
	$(RM) *.class
	$(RM) -r $(CLASSPATH)

$(CLASSPATH)/%.class : %.java
	$(shell if [ ! -e $(DSTPATH) ];then mkdir -p $(DSTPATH); fi)
	$(PATH)/javac -classpath $(PROJECTROOT) -d $(DSTPATH) $<

$(CLASSPATH)/%_Stub.class : %.java
	$(PATH)/rmic -classpath $(DSTPATH) -d $(DSTPATH) transaction.$*

.PHONY : runregistry runtm runrmflights runrmrooms runrmcars runrmcustomers runwc runclient clean
