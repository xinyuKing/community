package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger= LoggerFactory.getLogger(SensitiveFilter.class);
    //替换敏感词的常量
    private static final String REPLACEMENT="***";

    //根节点
    private TreeNode rootNode=new TreeNode();

    @PostConstruct
    public void init(){
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive_words.txt");
                BufferedReader reader=new BufferedReader(new InputStreamReader(is));
        ){
            String keyword;
            while ((keyword=reader.readLine())!=null){
                //添加到前缀树
                this.addKeyword(keyword);
            }

        }catch (IOException e){
            logger.error("加载敏感词文件失败"+e.getMessage());
        }

    }

    //将一个敏感词添加到前缀树中
    private void addKeyword(String keyword) {
        TreeNode tempNode=rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c=keyword.charAt(i);
            if (tempNode.getSubNode(c)==null) {
                //新建一个节点
                tempNode.addSubNode(c,new TreeNode());
            }
            //指针指向子节点
            tempNode=tempNode.getSubNode(c);
            if(i==keyword.length()-1){
                //最后一个节点设为结束节点
                tempNode.setKeywordEnd(true);
            }
        }
    }

    //过滤敏感词
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }

        //指针1
        TreeNode tempNode=rootNode;
        //指针2
        int begin=0;
        //指针3
        int position=0;
        //结果
        StringBuilder sb=new StringBuilder();

        while (position<text.length()){
            char c = text.charAt(position);

            //跳过符号
            if(isSymbol(c)){
                //若指针一处于根节点，将此符号计入结果,让指针2 向下走
                if(tempNode==rootNode){
                    sb.append(c);
                    begin++;
                }
                position++;
                continue;
            }

            //检查下一个节点
            tempNode=tempNode.getSubNode(c);

            if(tempNode==null){
                sb.append(text.charAt(begin));
                ++begin;
                position=begin;
                //指针重新指向根节点
                tempNode=rootNode;
            }else if (tempNode.isKeywordEnd()){
                //发现敏感词,替换
                sb.append(REPLACEMENT);
                begin=++position;
            }else{
                //继续检查下一个字符
                position++;
            }
        }

        //将最后一批字符计入结果
        sb.append(text.substring(begin));

        return sb.toString();
    }

    //判断是否为符号
    private Boolean isSymbol(Character c){
        //0x2E80~0x9FFF是东亚文字
        return !CharUtils.isAsciiAlphanumeric(c)&&(c<0x2E80||c>0x9FFF);
    }

    //前缀树
    private class TreeNode{
        //关键词结束标识
        private Boolean isKeywordEnd=false;

        //子节点（key是子节点字符，value是子节点）
        private Map<Character,TreeNode> subNode=new HashMap<>();

        public Boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(Boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        //添加字节点
        public void addSubNode(Character c,TreeNode node){
            subNode.put(c,node);
        }

        public TreeNode getSubNode(Character c){
            return subNode.get(c);
        }
    }
}
