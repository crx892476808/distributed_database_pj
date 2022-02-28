# distributed_database_pj
2021年DDB期末项目


# 运行方法

```shell
cd [文件根目录]/src/transaction
make all
# 以下每条命令按顺序且分别在新的新的终端窗口执行
make runregistry
make runtm
make runrmflights
make runrmcars
make runrmrooms
make runrmcustomers
make runrmreservations
make runwc
# 以下命令运行client测试代码
make runclient
```

# 主要完成内容
1 补充实现了用户进行数据库访问的逻辑。 
2 实现了分布式数据库事务的两阶段提交算法。
3 针对分布式数据库中Coordinator或Participants发生故障的情况，实现了异常处理和故障恢复模块。
