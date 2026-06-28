package com.homework.common.utils;

public final class LoginUserHolder {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    //给当前线程单独准备一个变量，用来存 Long类型的 userId
    //为什么用 ThreadLocal?
    //ThreadLocal 是线程安全的，每个线程都有自己的副本，不会相互影响，适合在多线程环境下使用，避免了线程安全问题。

    private LoginUserHolder() {
    }
    //private 构造方法：表示 不允许别人 new 这个类


    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }
    //用于保存 当前用户ID 到 当前线程里

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }
    //获取当前用户ID

    public static void removeUserId(){
        USER_ID_HOLDER.remove();
    }
}
