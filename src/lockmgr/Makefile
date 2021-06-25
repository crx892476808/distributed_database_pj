PROJECTROOT = ..
DSTPATH = $(PROJECTROOT)/../bin
CLASSPATH = $(DSTPATH)/lockmgr


lockmgr : $(CLASSPATH)/DataObj.class \
		  $(CLASSPATH)/DeadlockException.class \
	      $(CLASSPATH)/LockManager.class \
	      $(CLASSPATH)/RedundantLockRequestException.class \
	      $(CLASSPATH)/TimeObj.class \
	      $(CLASSPATH)/TPHashTable.class \
	      $(CLASSPATH)/TrxnObj.class \
	      $(CLASSPATH)/WaitObj.class \
	      $(CLASSPATH)/XObj.class

lmtest : LockManagerTest.class lockmgr

runlmtest : lmtest
	java -classpath $(PROJECTROOT) lockmgr.LockManagerTest

clean :
	$(RM) *.class
	$(RM) -r $(CLASSPATH)

$(CLASSPATH)/%.class : %.java
	javac -classpath $(PROJECTROOT) -d $(DSTPATH)  $<

.PHONY : runlmtest clean

