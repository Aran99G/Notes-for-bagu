# explain语句返回各列含义

![image-20230621220752603](assets/image-20230621220752603.png)

| 列名          | 含义                                                         |
| ------------- | ------------------------------------------------------------ |
| id            | 每个select都有一个对应的id号, 并且是从1开始自增的            |
| select type   | 查询语句执行的查询操作类型                                   |
| table         | 表名                                                         |
| partitions    | 表分区情况                                                   |
| ==**type**==  | 查询所用的访问类型, 效率从高到低: **systemc > const > eq ref > ref** > fulltext > ref or null > **range > index > ALL** |
| possible_keys | 表示在查询中可能使用到某个索引或多个索引; 如果没有选择索引, 显示null |
| key           | 实际查询用到的索引, 如果没有则显示null                       |
| **key_len**   | 当优化器决定使用某个索引执行查询时, 该索引记录的最大长度(主要使用在联合索引) |
| ref           | 使用到索引时, 与索引进行等值匹配的列或常量                   |
| rows          | 全表扫描时表示需要扫描表的行数估计值, 索引扫描时表示扫描索引的行数估计值, 值越小越好 (并不是结果集中的行数) |
| filtered      | 表示符合查询条件的数据百分比. 可以使用rows * (filterd / 100)计算出与explain前一个表进行连接的行数 |
| **extra**     | SQL执行的额外信息                                            |

## id

1. 如果id序号相同, 从上往下执行 (如内连接外连接)
2. 如果id序号不同, 序号大的先执行 (如子查询)
3. 如果两种都存在, 先执行序号大的, 再在同级从上往下执行 (如连接查询和子查询一起)
4. 如果显示null, 最后执行, 表示结果集, 并且不需要使用它来进行查询, 仅仅用作结果集展示 (比如使用union)

## select_type

1. simple: 简单select, 不包括union和子查询
2. primary: 复杂查询中最外层查询, 比如使用union或union all时, id为1的记录select_type通常是primary
3. subquery: 指在select语句中出现的子查询语句, 结果不依赖于外部查询 (不在from语句中) (比如子查询的条件where是常量)
4. dependent subquery: 指在select语句中出现的查询语句, 结果依赖于外部查询 (比如子查询的条件where依赖外部表)
5. derived: 派生表, 在from子句的查询语句, 表示从外部数据源中推导出来的, 而不是从select语句中的其他列中选择出来的
6. union: 分union和union all两种, 若第二个select出现在union之后, 则被标记为union; 如果union被from子句的子查询包含, 那么第一个select会被标记为derived; union会针对相同的结果集进行去重, union all不会进行去重; union会比union all多出一条临时表记录用于存储临时结果集然后进行去重
7. dependent union: 当union作为子查询时, 其中第一个union为dependent subquery, 第二个union为depentent union
8. union result: 表示临时表, 如果两个查询中有相同的列, 则会对这些列进行重复删除, 只保留一个表中的列

## type

 效率从高到低: **systemc > const > eq ref > ref >** fulltext > ref or null > **range > index > ALL**, 一般来说保证**range**级别, 最好能达到ref级别

1. system: const类型的一种特殊场景, 查询的表只有一行记录的情况, 并且该表使用的存储引擎的统计数据是精确的 (InnoDb不是精确的, 会显示ALL; Memory是精确的, 显示system)
2. const: 基于主键或唯一索引查看一行, 当MySQL对查询某部分进行优化, 并转换为一个常量时, 使用这些类型访问转换成常量查询, 效率高
3. eq_ref: 基于主键或唯一索引连接两个表, 对于每个索引键值, 只有一条匹配记录, 被驱动表的类型为 eq_ref 
4. ref: 基于非唯一索引连接两个表或通过二级索引列与常量进行等值匹配, 可能会存在多条匹配记录
   1. 关联查询, 使用非唯一索引进行匹配
   2. 简单查询, 使用二级索引列匹配
5. range: 使用非唯一索引扫描部分索引, 比如使用索引获取某些范围区间的记录
6. index: 扫描整个索引树就能拿到结果, 一般是二级索引, 这种查询一般使用覆盖索引 (需优化, 缩小数据范围)
7. all: 扫描整个表进行匹配, 即扫描聚簇索引树 (需优化, 添加索引优化)
8. NULL: MySQL在优化过程中分解语句就已经可以获取到结果, 执行时甚至不用访问表或索引 (一般使用了聚合函数)

## key_len

联合索引 计算规则:

> 字符串: 
>
> * char(n): n个字节
> * varchar(n): 
>   * 如果是UTF-8: **3n+2**字节, 加的两个字节存储字符串长度
>   * 如果是UTF8MB4: **4n+2**字节
>
> 数值类型:
>
> * tinyint: 1字节
> * smallint: 2字节
> * int: 4字节
> * bigint: 8字节
>
> 时间类型:
>
> * date: 3字节
> * timestamp: 4字节
> * datetime: 8字节
>
> **字段如果可以为NULL, 则需要额外1字节记录是否为NULL**

## ref(这个不太懂)

表示将哪个字段或常量和key列所使用的的字段进行比较

当使用索引列等值查询时, 与索引列进行等值匹配的对象信息

1. 常量
2. 字段
3. 函数

## rows

全表扫描时表示需要扫描表的行数估计值, 索引扫描时表示扫描索引的行数估计值, 值越小越好 (并不是结果集中的行数)

## filterd

表示符合查询条件的数据百分比. 可以使用rows * (filterd / 100)计算出与explain前一个表进行连接的行数

## Extra

1. Using index: 使用非主键索引树就可以查询所需要的数据. 一般是**覆盖索引**, 即查询列都包含在辅助索引树叶子节点中, 不需要回表查询
2. Using where: 不通过索引查询所需要的数据
3. Using index condition: 表示查询列不被索引覆盖, where条件中是一个索引范围查找, 过滤完索引后**回表**找到所有符合条件的数据行
4. Using temporary: 表示需要使用临时表来处理查询 (比如不走索引的去重) , **通过添加索引的方式进行优化Using index** 
5. Using filesort: 当查询中包含order by操作而且无法利用索引完成的排序操作, 数据较少时从内存排序, 如果数据较多需要在磁盘中排序, **需通过添加索引的方式优化成索引排序Using index**
6. Select tables optimized away: 使用某些聚合函数(min, max)来访问某个索引值

