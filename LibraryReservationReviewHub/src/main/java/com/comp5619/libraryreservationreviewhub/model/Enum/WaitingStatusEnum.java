package com.comp5619.libraryreservationreviewhub.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 等待列表状态枚举
 */
@Getter
public enum WaitingStatusEnum {

    WAITING("等待中", "waiting"),
    NOTIFIED("已通知", "notified"),
    EXPIRED("已过期", "expired"),
    CANCELLED("已取消", "cancelled");

    private final String text;
    private final String value;

    WaitingStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static WaitingStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (WaitingStatusEnum waitingStatusEnum : WaitingStatusEnum.values()) {
            if (waitingStatusEnum.value.equals(value)) {
                return waitingStatusEnum;
            }
        }
        return null;
    }
}