package com.aaa.springai.agent.parser;

/**
 *
 * OutputParserException继承自Exception时，提示你必须在parse方法中添加throws OutputParserException声明
 * OutputParserException继承自RuntimeException时，编译器不会要求你显式处理这个异常
 *
 * @author liuzhen.tian
 * @version 1.0 OutputParserException.java  2025/4/19 21:44
 */
public class OutputParserException extends RuntimeException{
    public OutputParserException(String s) {
        super(s);
    }

    public OutputParserException() {
    }
}
