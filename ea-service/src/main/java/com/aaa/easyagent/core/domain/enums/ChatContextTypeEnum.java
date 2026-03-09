package com.aaa.easyagent.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author liuzhen.tian
 * @version 1.0 ChatContextTypeEnum.java  2026/3/13 22:30
 */
@Getter
@AllArgsConstructor
public enum ChatContextTypeEnum {
    thinking,
    data,
    tool,
    finalAnswer;
}
