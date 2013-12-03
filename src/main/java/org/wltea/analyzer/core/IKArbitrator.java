/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 */
package org.wltea.analyzer.core;

import java.util.Stack;
import java.util.TreeSet;

/**
 * IK分词歧义裁决器
 */
class IKArbitrator {

	IKArbitrator(){
		
	}
	
	/**
	 * 分词歧义处理
	 * @param context
	 * @param useSmart
	 */
	void process(AnalyzeContext context , boolean useSmart){
		QuickSortSet orgLexemes = context.getOrgLexemes();
		Lexeme orgLexeme = orgLexemes.pollFirst();
		
		LexemePath crossPath = new LexemePath();
		while(orgLexeme != null){
			if(!crossPath.addCrossLexeme(orgLexeme)){
				//找到与crossPath不相交的下一个crossPath	
				if(crossPath.size() == 1 || !useSmart){
					//crossPath没有歧义 或者 不做歧义处理
					//直接输出当前crossPath
					context.addLexemePath(crossPath);
				}else{
					//对当前的crossPath进行歧义处理
					QuickSortSet.Cell headCell = crossPath.getHead();
					LexemePath judgeResult = this.judge(headCell, useSmart);
					//输出歧义处理结果judgeResult
					context.addLexemePath(judgeResult);
				}
				
				//把orgLexeme加入新的crossPath中
				crossPath = new LexemePath();
				crossPath.addCrossLexeme(orgLexeme);
			}
			orgLexeme = orgLexemes.pollFirst();
		}
		
		
		//处理最后的path
		if(crossPath.size() == 1 || !useSmart){
			//crossPath没有歧义 或者 不做歧义处理
			//直接输出当前crossPath
			context.addLexemePath(crossPath);
		}else{
			//对当前的crossPath进行歧义处理
			QuickSortSet.Cell headCell = crossPath.getHead();
			LexemePath judgeResult = this.judge(headCell, useSmart);
			//输出歧义处理结果judgeResult
			context.addLexemePath(judgeResult);
		}
	}
	
	/**
	 * 歧义识别
	 *
     * @param lexemeCell 歧义路径链表头
     * @param useSmart 歧义路径优先选用两个词
     * @return
	 */
	private LexemePath judge(QuickSortSet.Cell lexemeCell , boolean useSmart){
		//候选路径集合
		TreeSet<LexemePath> pathOptions = new TreeSet<LexemePath>();
		//候选结果路径
		LexemePath option = new LexemePath();
		
		//对crossPath进行一次遍历,同时返回本次遍历中有冲突的Lexeme栈
		Stack<QuickSortSet.Cell> lexemeStack = this.forwardPath(lexemeCell , option);
		
		//当前词元链并非最理想的，加入候选路径集合
		pathOptions.add(option.copy());
		
		//存在歧义词，处理
		QuickSortSet.Cell c = null;
		while(!lexemeStack.isEmpty()){
			c = lexemeStack.pop();
			//回滚词元链
			this.backPath(c.getLexeme() , option);
			//从歧义词位置开始，递归，生成可选方案
			this.forwardPath(c , option);
			pathOptions.add(option.copy());
		}

        LexemePath candidateLexemePath = pathOptions.first();
        if (useSmart && candidateLexemePath.size() == 1 && isLetterOrCnWord(candidateLexemePath)) {
            int orgiSize = candidateLexemePath.size();
            int smartSize = 0;
            if (orgiSize == 1) {
                smartSize = 2;
            }

            for (LexemePath pathOption : pathOptions) {
                if (pathOption.size() == smartSize && isFullSplit(pathOption)) {
                    return pathOption;
                }
            }
        }

        //返回集合中的最优方案
		return candidateLexemePath;

	}

    private boolean isLetterOrCnWord(LexemePath candidateLexemePath) {
        int lexemeType = candidateLexemePath.peekFirst().getLexemeType();
        return lexemeType == Lexeme.TYPE_LETTER || lexemeType == Lexeme.TYPE_CNWORD;
    }

    /**
     * 判断分词路径是否是完整的两个词组成
     * 规则:
     * 1、刚好可以分成两个词
     * 2、两个词的长度都大于1
     * 3、两个词是是不同的类型
     *
     * @param pathOption
     * @return
     */
    private boolean isFullSplit(LexemePath pathOption) {
        Lexeme first = pathOption.peekFirst();
        Lexeme last = pathOption.peekLast();

        if (first.getEndPosition() == last.getBegin()
                && pathOption.getPathLength() == (first.getLength() + last.getLength())
                && (first.getLength() > 1 || last.getLength() > 1)
                && first.getLexemeType() != last.getLexemeType()
                && !hasSingleEnglishLetter(first,last)) {
            return true;
        }
        return false;
    }

    private boolean hasSingleEnglishLetter(Lexeme... lexemes) {
        for (Lexeme lexeme : lexemes) {
            if (lexeme.getLength() == 1 && lexeme.getLexemeType() == Lexeme.TYPE_ENGLISH) {
                return true;
            }
        }

        return false;
    }

    /**
	 * 向前遍历，添加词元，构造一个无歧义词元组合
	 * @param option
     * @param lexemeCell
	 * @return
	 */
	private Stack<QuickSortSet.Cell> forwardPath(QuickSortSet.Cell lexemeCell , LexemePath option){
		//发生冲突的Lexeme栈
		Stack<QuickSortSet.Cell> conflictStack = new Stack<QuickSortSet.Cell>();
		QuickSortSet.Cell c = lexemeCell;
		//迭代遍历Lexeme链表
		while(c != null && c.getLexeme() != null){
			if(!option.addNotCrossLexeme(c.getLexeme())){
				//词元交叉，添加失败则加入lexemeStack栈
				conflictStack.push(c);
			}
			c = c.getNext();
		}
		return conflictStack;
	}
	
	/**
	 * 回滚词元链，直到它能够接受指定的词元
	 * @param l
	 * @param option
	 */
	private void backPath(Lexeme l  , LexemePath option){
		while(option.checkCross(l)){
			option.removeTail();
		}
		
	}
	
}
