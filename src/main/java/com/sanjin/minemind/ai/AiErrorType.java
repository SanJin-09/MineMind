package com.sanjin.minemind.ai;

public enum AiErrorType {
    NETWORK("网络连接失败，请检查网络或代理设置"),
    AUTH("API Key 无效，请重新设置 /ai key <provider> <key>"),
    MODEL("当前模型不存在，请使用 /ai model 重新选择"),
    QUOTA("服务商返回限流或额度不足，请稍后再试"),
    REQUEST("请求参数无效或上下文过长，请清空上下文后重试"),
    SERVICE("服务商暂时不可用，请稍后再试"),
    TIMEOUT("请求超时，稍后可重试"),
    EMPTY_RESPONSE("回复内容为空"),
    MISSING_KEY("当前没有配置 API Key"),
    LOCAL("本地配置或运行状态异常");

    private final String message;

    AiErrorType(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
