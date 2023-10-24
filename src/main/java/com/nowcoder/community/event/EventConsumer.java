package com.nowcoder.community.event;

import com.alibaba.fastjson2.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;



@Component
public class EventConsumer implements CommunityConstant {
    private static final Logger logger= LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private DiscussPostService discussPostService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.key.access}")
    private String access;

    @Value("${qiniu.key.secret}")
    private String secret;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;


    /*处理系统异步发送评论，点赞，关注的通知*/
    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_FOLLOW,TOPIC_LIKE})
    public void HandleCommentMessage(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null) {
            logger.error("消息格式错误！");
            return;
        }

        //发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String,Object> map=new HashMap<>();
        map.put("userId",event.getUserId());
        map.put("entityType",event.getEntityType());
        map.put("entityId",event.getEntityId());

        if(!event.getData().isEmpty()){
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                map.put(entry.getKey(),entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(map));

        messageService.addMessage(message);
    }

    /*处理发帖时把帖子异步发送到elasticsearch中去*/
    @KafkaListener(topics = TOPIC_PUBLISH)
    public void HandlePublish(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null) {
            logger.error("消息格式错误！");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());

        elasticsearchService.saveDiscussPost(post);
    }

    /*处理删帖时在elasticsearch中异步删除该帖子*/
    @KafkaListener(topics = TOPIC_DELETE)
    public void HandleDelete(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null) {
            logger.error("消息格式错误！");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    /*消费分享事件*/
    @KafkaListener(topics = TOPIC_SHARE)
    public void HandleShare(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null) {
            logger.error("消息格式错误！");
            return;
        }
        String htmlUrl=(String)event.getData().get("htmlUrl");
        String fileName=(String)event.getData().get("fileName");
        String suffix=(String)event.getData().get("suffix");

        String cmd=wkImageCommand+" --quality 75 "+htmlUrl+" "+wkImageStorage+"/"+fileName+suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功："+cmd);
        } catch (IOException e) {
            logger.error("生成长图失败："+e.getMessage());
        }

        // 启动定时器，监视该图片，一旦生成了，则上传到七牛云
        UploadTask task=new UploadTask(fileName,suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);
    }

    class UploadTask implements Runnable{
        //文件名称
        private String fileName;
        //文件后缀
        private String suffix;
        //启动任务的返回值（它可以停止定时器）
        private Future future;
        //开始时间
        private long startTime;
        //上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime=System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            //生成图片失败:大于30秒
            if(System.currentTimeMillis()-startTime>30*1000){
                logger.error("执行时间过长，终止任务："+fileName);
                future.cancel(true);
                return;
            }
            //上传失败
            if(uploadTimes>=10){
                logger.error("上传次数过多，终止任务："+fileName);
                future.cancel(true);
                return;
            }

            String path=wkImageStorage+"/"+fileName+suffix;
            File file = new File(path);
            if(file.exists()){
                logger.info(String.format("开始第%d次上传[%s].",++uploadTimes,fileName));
                //设置相应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                // 生成上传凭证
                Auth auth = Auth.create(access, secret);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                //指定上传的机房
                UploadManager manager = new UploadManager(new Configuration(Zone.zone0()));
                try {
                    //开始上传图片
                    Response response = manager.put(path, fileName, uploadToken, null, "image/" + suffix, false);
                    //处理响应结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if(json==null||json.get("code")==null||!json.get("code").toString().equals("0")){
                        logger.info(String.format("第%d次上传失败[%s]",uploadTimes,fileName));
                    }else{
                        logger.info(String.format("第%d次上传成功[%s]",uploadTimes,fileName));
                        future.cancel(true);
                    }
                }catch (QiniuException e){
                    logger.info(String.format("第%d次上传失败[%s]",uploadTimes,fileName));
                }
            }else{
                logger.info("等待图片生成["+fileName+"]");
            }
        }
    }
}
