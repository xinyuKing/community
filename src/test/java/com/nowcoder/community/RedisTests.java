package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = StudyApplication.class)
public class RedisTests {
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings(){
        String redisKey="test:count";
        redisTemplate.opsForValue().set(redisKey,1);

        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHashes(){
        String redisKey="test:user";
        redisTemplate.opsForHash().put(redisKey,"id",1);
        redisTemplate.opsForHash().put(redisKey,"username","tom");

        System.out.println(redisTemplate.opsForHash().get(redisKey,"username"));
        System.out.println(redisTemplate.opsForHash().get(redisKey,"id"));
    }

    @Test
    public void testLists(){
        String redisKey="test:ids";
        redisTemplate.opsForList().leftPush(redisKey,101);
        redisTemplate.opsForList().leftPush(redisKey,102);
        redisTemplate.opsForList().leftPush(redisKey,103);
        redisTemplate.opsForList().leftPush(redisKey,104);

        System.out.println(redisTemplate.opsForList().size(redisKey));
        System.out.println(redisTemplate.opsForList().index(redisKey,0));
        System.out.println(redisTemplate.opsForList().range(redisKey,0,3));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    }

    @Test
    public void testSets(){
        String redisKey="test:teachers";

        redisTemplate.opsForSet().add(redisKey,"aaa","bbb","ccc","ddd","eee");

        System.out.println(redisTemplate.opsForSet().size(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().members(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
    }

    @Test
    public void testSortedSets(){
        String redisKey="test:Students";

        redisTemplate.opsForZSet().add(redisKey,"tom01",20);
        redisTemplate.opsForZSet().add(redisKey,"tom02",30);
        redisTemplate.opsForZSet().add(redisKey,"tom03",40);
        redisTemplate.opsForZSet().add(redisKey,"tom04",50);
        redisTemplate.opsForZSet().add(redisKey,"tom05",60);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        System.out.println(redisTemplate.opsForZSet().score(redisKey,"tom02"));
        System.out.println(redisTemplate.opsForZSet().rank(redisKey,"tom02"));
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey,"tom02"));
        System.out.println(redisTemplate.opsForZSet().range(redisKey,0,2));
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey,0,2));
    }

    @Test
    public void testKeys(){
        redisTemplate.delete("test:user");

        System.out.println(redisTemplate.hasKey("test:user"));

        redisTemplate.expire("test:Students",10, TimeUnit.SECONDS);
    }

    /*多次访问同一个key*/
    @Test
    public void testBoundOperations(){
        String redisKey="test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    /*编程式事务*/
    @Test
    public void testTransactional(){
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey="test:tx";
                //启用事务
                operations.multi();

                operations.opsForSet().add(redisKey,"tom01");
                operations.opsForSet().add(redisKey,"tom02");
                operations.opsForSet().add(redisKey,"tom03");

                System.out.println(operations.opsForSet().members(redisKey));

                //提交事务
                return operations.exec();
            }
        });
        System.out.println(obj);
    }

    /*统计20万个重复数据的独立总数*/
    @Test
    public void testHyperLogLog(){
        String redisKey="test:hll:02";
        for (int i = 0; i < 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey,i);
        }

        for (int i = 0; i < 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey,(int)(Math.random()*100000+1));
        }

        Long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);
    }

    /*将三组数据合并，再统计合并后的重复数据的独立总数*/
    @Test
    public void testHyperLogLogUnion(){
        String redisKey03="test:hll:03";
        for (int i = 0; i < 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey03,i);
        }

        String redisKey04="test:hll:04";
        for (int i = 5000; i < 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey04,i);
        }

        String redisKey05="test:hll:05";
        for (int i = 10000; i < 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey05,i);
        }

        String unionKey="test:hll:union";

        redisTemplate.opsForHyperLogLog().union(unionKey,redisKey03,redisKey04,redisKey05);

        Long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);
    }

    /*统计一组数据的布尔值*/
    @Test
    public void testBitMap(){
        String redisKey="test:bm:01";

        //记录
        redisTemplate.opsForValue().setBit(redisKey,1,true);
        redisTemplate.opsForValue().setBit(redisKey,4,true);
        redisTemplate.opsForValue().setBit(redisKey,7,true);

        //查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,3));

        //统计
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);
    }

    /*统计三组数据的布尔值，并对这三组数据做OR运算*/
    @Test
    public void testBitMapOperate(){
        String redisKey02="test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey02,0,true);
        redisTemplate.opsForValue().setBit(redisKey02,1,true);
        redisTemplate.opsForValue().setBit(redisKey02,2,true);

        String redisKey03="test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey03,3,true);
        redisTemplate.opsForValue().setBit(redisKey03,4,true);
        redisTemplate.opsForValue().setBit(redisKey03,2,true);

        String redisKey04="test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey04,4,true);
        redisTemplate.opsForValue().setBit(redisKey04,5,true);
        redisTemplate.opsForValue().setBit(redisKey04,6,true);

        String redisKey="test:bm:or";
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(), redisKey02.getBytes(), redisKey03.getBytes(), redisKey04.getBytes());
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });

        System.out.println(obj);

        for (int i = 0; i < 7; i++) {
            System.out.println(redisTemplate.opsForValue().getBit(redisKey,i));
        }
    }
}
