# 20210625

影子页面是原子性的实现方法？

锁机制是并发控制的实现方法？



接下来，在part2 首先介绍了 RM的实现，随后是并发控制的实现(TM)



**问题是到现在都没搞清楚到底是基于Source Code 做还是 Optional Source  Code做**

感觉学长的项目是基于Source Code 做的，因为他们的RM资源对象类的命名和Optional Source Code 不一样。
但是PPT中明显就是说RM已经基本实现，包括资源对象和RMimpl。

决策：先用OptionSourceCode来做吧

结果：OptionSourceCode的RMManagerImpl没有main函数，第一个make run就失败了

转移到 SourceCode来做，但是Source Code并不符合PPT中说的RM基本实现，可选的做法是把optionSourceCode的部分RM实现扔过来



但即使这样也没法运行，目前的问题是conf/ddb.conf这个东西找不到



一开始make runregistry的作用



> rmiregistry 命令在当前主机的指定端口上创建并启动远程对象注册表。如果省略 port，则注册表将在端口 1099 上启动。 rmiregistry 命令不产生任何输出并且通常在后台运行。例如：
>
> rmirregistry &
>
> 远程对象注册表是一种引导命名服务，同一主机上的 RMI 服务器使用它来将远程对象绑定到名称。本地和远程主机上的客户端然后可以查找远程对象并进行远程方法调用。
>
> 注册表通常用于定位应用程序需要在其上调用方法的第一个远程对象。该对象反过来将为查找其他对象提供特定于应用程序的支持。
>
> java.rmi.registry.LocateRegistry 类的方法用于获取在本地主机或本地主机和端口上运行的注册表。
>
> java.rmi.Naming 类的基于 URL 的方法对注册表进行操作，可用于在任何主机和本地主机上查找远程对象：将简单（字符串）名称绑定到远程对象，重新绑定远程对象的新名称（覆盖旧绑定），解除远程对象的绑定，并列出注册表中绑定的 URL。



大概能看懂整个流程了，client去找workflowController，然后workflowController需要去调用resourceManager的相应方法



java -D 的作用

```shell
-D<名称>=<值>
                  设置系统属性
```



感觉即使是RM，具体的资源类仍然没有对应上磁盘上的文件，也就是说还是没有实现



现在的问题

（1）ResourceManager类实现不完整，没有对应到文件上

（2）ResourceManager类的影子页面机制实现了吗？

（3）打通一个从WorkFlow Controller 到 ResourceManager的道路



# 20210626

## RM变更

将main函数放到RMimpl中 （option code中本来是放在不同的impl中，比如RMRoomsImpl这样）

## RM变更

自主实现了各个实体类

## 打通从workFlow Controller 到 ResourceManager的道路

为什么持久化的data还有xid这个事务ID存在作为标识符？那不就是每张表只能对应每个事务了吗？（insert）



似乎看懂了一点：在RM的commit 中包含了把要提交的内容写回到data/里的内容



## table names 使用规定在RM类中



## 提交

按ppt里的说法，提交应该是workcontroller去调用TM中的commit 方法，再由TM去确认各个RM的情况之后调用RM的commit

![image-20210626171130152](D:\To_w\master\master_part2\distributed_database\distributed_database_pj\media\image-20210626171130152.png)

![image-20210626171156365](D:\To_w\master\master_part2\distributed_database\distributed_database_pj\media\image-20210626171156365.png)

# 20210627

RM出现两次

RMCustomers's xids is Empty ? true
RMCustomers bound to TM

是正常的，因为在wc中初始化时有一个reconnect的操作。



TODO：验证Fights完成了插入后能不能正确query。

已经验证

# 20210628

TODO?: 影子页面？

关于行锁：一锁锁一行或多行记录，相对于表锁而言。表锁是一锁锁整张表

https://blog.csdn.net/weixin_37686415/article/details/114711276



**table代表已经持久化的数据, xtable代表事务进行中的数据**

**transactionlogs的使用，ResourceManagerImpl对象的实例化**

ResourceManagerImpl(String ) 调用 recover() , recover()调用LoadTransactionLogs，并且会在这个函数内重新获得tables和xtables的控制。TransactionLogs应该只是记录到目前为止ResourceManagerImpl所参与的事务的ID，并不影响再次运行（已经验证）



# 20210629

TODO ： 影子页面？

RMImpl 的 commit 存在删除xid相关数据然后保存真正data的过程

xid相关数据： data/[xid]/tablename

注意，这里存的都是整张表，而不只是这个事务涉及的数据

```java
//protected RMTable getTable(int xid, String tablename),getTable 被 insert调用，获取该Transaction对应的临时表
//注意，当getTable输入的xid==-1时，则返回当前持久化的data
//只带一个参数的getTable就是返回持久化的data
xidtables = (Hashtable) tables.get(new Integer(xid));
if (xidtables == null)
{
    xidtables = new Hashtable();
    tables.put(new Integer(xid), xidtables);
}

//...

RMTable table = (RMTable) xidtables.get(tablename);
if (table != null)
    return table;
table = loadTable(new File("data/" + (xid == -1 ? "" : "" + xid + "/") + tablename));


```

这样就可以看懂了，**RMImpl的操作都是在transaction对应的临时表上进行**

但是 RMImpl让人感觉是一次把所有的临时表全部commit 了？ 不对，因为每个RM都对应一个RMImpl对象，所以这里要删的data/[xid]/[tableName]应该局限于这个对象管理的临时表

但是最后有一个data/[xid]的删除就很迷惑



**现在影子页面的机制其实已经完成了，但是在RMimpl的commit最后删除data/[xid]可能是错误的做法**



## 关于最终实现

TM中的所有接口都是我们自己实现的，还新定义了一部分接口

WC中的所有接口也都是我们自己实现的

RM应该也有按需修改的部分

Makefile 也做了修改，以便形成所要求的文档结构

实体类也是我们自己实现的

## TODO

1 再完成一个实体类，以便测试我们对RMImpl 中commit的做法理解是否正确

注意：直接query某个实体类（比如Flight 和 Room，返回的是剩余的座位和房间数）

测试完成，可以工作，写一个完整的日志，push

2 两阶段提交，初始。

RM里直接调用的是RMTable中已经封装好的RMTable::lock方法，并且在RMImpl的每个操作（如Insert ，query 和 delete 中都进行调用）



感觉这里的加锁操作都已经实现好了，那还需要实现什么2PL？

可能2PL.ppt里还有一些内容，可以看这部分PPT后再考虑2PL的实现。





## RM变更

将RMImpl的commit函数中的

```java
new File("data/" + xid).delete(); // delete this xid, why?
```

移到TM的commit中 



# 20210701

## 读2PC.ppt后考虑两阶段提交的实现

分布式提交要求Commit 是 atomic的，以便保证如下特性：

> must ensure that despite failures, if all failures repaired, then transactions commits or aborts at all sites.

常见的Atomic Commit Protocol  ： Two-Phase Commit

**Terminology**

RM/Participant 对应我们代码里的RM； Coordinator对应TM

**总过程**

![image-20210701210607879](D:\To_w\master\master_part2\distributed_database\distributed_database_pj\media\image-20210701210607879.png)

![image-20210701210659749](D:\To_w\master\master_part2\distributed_database\distributed_database_pj\media\image-20210701210659749.png)

**States of the Transaction**

![image-20210701210924776](D:\To_w\master\master_part2\distributed_database\distributed_database_pj\media\image-20210701210924776.png)

Coordinator需要为每个Transaction维护一个protocol Database

![image-20210701211408404](D:\To_w\master\master_part2\distributed_database\distributed_database_pj\media\image-20210701211408404.png)

![image-20210701211440144](D:\To_w\master\master_part2\distributed_database\distributed_database_pj\media\image-20210701211440144.png)

当Participant进入P状态的时候，（1）它必须已经获得了所有资源；（2）它最终要么commit ，要么abort （都是在coordinator的指挥下进行）

Coordinator进入C状态当且仅当所有的participants已经是在P状态，并且确保<u>最终</u>所有的participants都会commit

**Normal Actions (Coordinator)**

> 1 –**make entry into protocol** database for transaction marking its status as **initiated** when coordinator **first learns about transaction**
>
> 2 –**Add participant to the cohort list** in protocol database when coordinator learns about the cohorts (指的应该是参与者)
>
> 3 –**Change status of transaction to preparing** before sending prepare message. (it is assumed that coordinator will know about all the participants before this step)
>
> 4 –**On receipt of PREPARE** message from cohort, **mark cohort as PREPARED**. If **all cohorts PREPARED**, then **change status to COMMITTED and send COMMIT message**.
>
> ​	must force a commit log record to disk before sending commit message
>
> 5 on receipt of ACK message from cohort, mark cohort as ACKED. When **all cohorts have acked**, then **delete entry of transaction from protocol database**
>
> ​	Must write a completed log record to disk before deletion from protocol database. No need to force the write though. 

**Normal Actions( Participant)**

(ppt11)

> 1 **On receipt of PREPARE message, write PREPARED log record before sending PREPARED message**
>
> ​	needs to be forced to disk since coordinator may now commit 
>
> 2 On receipt of COMMIT message, **write COMMIT log record before sending ACK to coordinator**
>
> ​	–cohort must ensure log forced to disk before sending ack -- but no great urgency for doing so.

**Timeout Actions**

![image-20210703161056196](D:\To_w\master\master_part2\distributed_database\distributed_database_pj\media\image-20210703161056196.png)

Coordinator Timeout Actions

> **waiting for votes of participants:** ABORT transaction, send aborts to all

注意这里在等ACK的时候不回滚

> –**waiting for ack from some participant:** forward the transaction to recovery process that periodically will send COMMIT to participant. When participant will recover, and all participants send an ACK, coordinator writes a completion log record and deletes entry from protocol database.

Participant TImeout Actions

有点迷惑的是为什么要waiting for prepare？

>  –**waiting for prepare:** abort the transaction, send abort message to coordinator. 

> –**Waiting for decision:** The transaction is blocked.

## TODO

继续读完PPT，然后按照normal Actions 的方法实现基本的两阶段提交，如果可以就接着实现部分异常处理。





# 20210704

## 两阶段提交实现

RM里的commit没有返回值，无法确认是否返回ACK，这就不符合两阶段提交的要求了

## RM 变更 

让RMimpl的commit 返回布尔值，表示是否正确完成commit



## RM的调用

注意，TM在optioncode的原始定义里，enlist就包含(ResourceManager rm)的参数，所以这里的做法应该没错

## normal action的实现

目前没有加上log的记录，主要是不知道以什么形式写log？

注意TM里面也定义和实现好了dieNow，说明TM的错误也是要被考虑的，但是

## TM端的日志

文件：

./data/transLog/xid

内容：

当前xid对应的trans的状态



## RM端的日志

目前的实现（在optionSourceCode中已经完成的）是在commit的时候将临时的数据文件删除

会不会相应的日志记录在了transaction.log里面？

暂时不考虑日志的实现，先把participant端的状态管理好吧



## RM 变更

加入transIDToStatus这个对象，管理该rm对每个Transaction的状态



## 考虑需要进行错误处理的场景

1 当并非所有的RM都返回Prepared的时候，Abort All

在TM端考虑处理方式

注意关于die的接口全部定义在WorkflowController中，这里的die应该是发生在prepare之前？所以实现dieBeforePrepare

如果已经有RM 挂掉了，挂掉的RM要怎么回滚？还是只对alive的RM做回滚，而挂掉的RM在重启之后才回滚？

重启是要RM自己去重启吗？（也就是直接在控制台重新MakeRun）主要是目前也没有看到client端有进行重启RM的操作。

RM自行进行重启后会调用recover() ,重新拿到xids，随后reconnect，重新enlist，所以要做的应该是在enlist的时候让相应的RM成功aborted？

目前：删掉了临时文件夹data/xid中的RM对应临时数据。

TODO：

1 看在当前的情况下能否正确删除掉data/1 √

2 看看是否需要为每个RM单独记录transaction.log √



**transaction.log在重启中的作用**

```java
public ResourceManagerImpl(String rmiName) -> recover() -> loadTransactionLogs() -> 获取t_xids，  将xtables恢复
```

Transaction.log记录一个hashset: xids，里面记录所有参与的transaction的ID（每次在query, add 或者delete的时候都会xids.add(xid)， 在commit 或者 abort 的时候会xids.remove(xid))。 但在目前的实现中似乎是每个RM都共享同一个transaction.log，感觉并不靠谱。

**recover**

```java

tm = (TransactionManager) Naming.lookup(rmiPort + TransactionManager.RMIName);
对每个xids中包含的xid: 
tm.enlist(xid, this);
说明应该要在enlist的过程中进行恢复
```



## RM 变更

abort中删除xid的移到TM中



## RM变更

loadTransactionLog 全部按照 RM的名字命名



# 20210705 

## Abort的实现

TODO： 1 看看当前对insert方法的abort有没有很好的支持

2 新增其它实体



# 20210711

**注意，临时数据是RM维护的，而transLog是我们后面自主加的**

## Abort的实现(2)

TODO： 1 看看当前对insert方法的abort有没有很好的支持

已经验证对于before prepare的异常，进行了正确的abort √

已经验证对于before commit 的异常，进行了正确的recommit √

2 新增其它实体

3 waiting for ack from some participant（在after prepare 或者before commit 的时候rm挂掉）

之前实现的是在vote的时候rm 挂掉的处理方式，现在应该要实现commit命令从tm发出的时候rm挂掉的处理

目前RM对于临时表的所有内容都有控制权，这里似乎也要改造才行，否则就让不同RM之间职责不明确了。

## 目前进行recover时，每个RM会去找所有的临时表，这显然不合理



## RM变更

在recover中限制当前RM只会关注自身所管理的Table。



## Abort的实现（3）

TODO ： 

（1） 测试：在两个RM都挂掉的情况下，当前的处理能否正确让两个RM分别恢复自身的提交。（或分别abort）
