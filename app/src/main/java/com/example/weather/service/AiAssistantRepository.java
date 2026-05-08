package com.example.weather.service;

import android.text.TextUtils;

import com.example.weather.BuildConfig;
import com.example.weather.model.AiChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiAssistantRepository {

    public interface ChatCallback {
        void onSuccess(String responseText);

        void onFailure(String message);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    public void chat(List<AiChatMessage> messages, String systemPrompt, ChatCallback callback) {
        if (TextUtils.isEmpty(BuildConfig.ALIYUN_BAILIAN_API_KEY)) {
            callback.onFailure("请在 local.properties 中配置 ALIYUN_BAILIAN_API_KEY");
            return;
        }

        if (!TextUtils.isEmpty(BuildConfig.ALIYUN_BAILIAN_APP_ID)) {
            requestAgentApp(messages, systemPrompt, new ChatCallback() {
                @Override
                public void onSuccess(String responseText) {
                    callback.onSuccess(responseText);
                }

                @Override
                public void onFailure(String message) {
                    requestModelFallback(messages, systemPrompt, callback, message);
                }
            });
            return;
        }

        requestModelFallback(messages, systemPrompt, callback, null);
    }

    private void requestAgentApp(List<AiChatMessage> messages, String systemPrompt, ChatCallback callback) {
        JsonObject payload = new JsonObject();
        JsonArray input = new JsonArray();

        input.add(buildAppMessage("system", systemPrompt));

        for (AiChatMessage message : messages) {
            if (message.isLoading()) {
                continue;
            }
            input.add(buildAppMessage(message.getRole(), message.getContent()));
        }

        payload.add("input", input);
        payload.addProperty("stream", false);

        Request request = new Request.Builder()
                .url(buildEndpoint())
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + BuildConfig.ALIYUN_BAILIAN_API_KEY)
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                callback.onFailure("AI 应用请求失败：" + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    callback.onFailure(buildErrorMessage(response.code(), body, true));
                    return;
                }
                String text = extractAssistantText(body);
                if (TextUtils.isEmpty(text)) {
                    callback.onFailure("AI 应用暂时没有返回可用内容");
                    return;
                }
                callback.onSuccess(text.trim());
            }
        });
    }

    private void requestModelFallback(List<AiChatMessage> messages, String systemPrompt,
                                      ChatCallback callback, String appFailureMessage) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", TextUtils.isEmpty(BuildConfig.ALIYUN_BAILIAN_MODEL)
                ? "qwen-plus"
                : BuildConfig.ALIYUN_BAILIAN_MODEL);
        JsonArray messageArray = new JsonArray();
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messageArray.add(systemMessage);
        for (AiChatMessage message : messages) {
            if (message.isLoading()) {
                continue;
            }
            JsonObject item = new JsonObject();
            item.addProperty("role", message.getRole());
            item.addProperty("content", message.getContent());
            messageArray.add(item);
        }
        payload.add("messages", messageArray);

        Request request = new Request.Builder()
                .url(buildModelEndpoint())
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + BuildConfig.ALIYUN_BAILIAN_API_KEY)
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                String prefix = TextUtils.isEmpty(appFailureMessage) ? "" : appFailureMessage + "；";
                callback.onFailure(prefix + "AI 对话模型请求失败：" + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    String prefix = TextUtils.isEmpty(appFailureMessage) ? "" : appFailureMessage + "；";
                    callback.onFailure(prefix + buildErrorMessage(response.code(), body, false));
                    return;
                }
                String text = extractModelText(body);
                if (TextUtils.isEmpty(text)) {
                    String prefix = TextUtils.isEmpty(appFailureMessage) ? "" : appFailureMessage + "；";
                    callback.onFailure(prefix + "AI 对话模型暂时没有返回可用内容");
                    return;
                }
                callback.onSuccess(text.trim());
            }
        });
    }

    private String buildEndpoint() {
        String baseUrl = normalizeBaseUrl(BuildConfig.ALIYUN_BAILIAN_BASE_URL);
        if (baseUrl.contains("/api/v2/apps/agent/") && baseUrl.endsWith("/responses")) {
            return baseUrl;
        }
        return baseUrl + "/api/v2/apps/agent/" + BuildConfig.ALIYUN_BAILIAN_APP_ID + "/compatible-mode/v1/responses";
    }

    private String buildModelEndpoint() {
        return normalizeBaseUrl(BuildConfig.ALIYUN_BAILIAN_BASE_URL) + "/compatible-mode/v1/chat/completions";
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        String normalized = TextUtils.isEmpty(rawBaseUrl)
                ? "https://dashscope.aliyuncs.com"
                : rawBaseUrl.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String extractAssistantText(String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray output = root.has("output") && root.get("output").isJsonArray()
                ? root.getAsJsonArray("output")
                : null;
        if (output == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (JsonElement outputItem : output) {
            if (!outputItem.isJsonObject()) {
                continue;
            }
            JsonObject messageObject = outputItem.getAsJsonObject();
            String role = getString(messageObject, "role");
            if (!"assistant".equals(role)) {
                continue;
            }
            JsonArray contentArray = messageObject.has("content") && messageObject.get("content").isJsonArray()
                    ? messageObject.getAsJsonArray("content")
                    : null;
            if (contentArray == null) {
                continue;
            }
            for (JsonElement contentItem : contentArray) {
                if (!contentItem.isJsonObject()) {
                    continue;
                }
                JsonObject contentObject = contentItem.getAsJsonObject();
                if ("output_text".equals(getString(contentObject, "type"))) {
                    String text = getString(contentObject, "text");
                    if (!TextUtils.isEmpty(text)) {
                        if (builder.length() > 0) {
                            builder.append('\n');
                        }
                        builder.append(text);
                    }
                }
            }
        }
        return builder.toString();
    }

    private String extractModelText(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = root.has("choices") && root.get("choices").isJsonArray()
                    ? root.getAsJsonArray("choices")
                    : null;
            if (choices == null || choices.size() == 0 || !choices.get(0).isJsonObject()) {
                return "";
            }
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.has("message") && choice.get("message").isJsonObject()
                    ? choice.getAsJsonObject("message")
                    : null;
            return getString(message, "content");
        } catch (Exception ignored) {
            return "";
        }
    }

    private JsonObject buildAppMessage(String role, String content) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "message");
        item.addProperty("role", role);
        JsonArray contentArray = new JsonArray();
        JsonObject textObject = new JsonObject();
        textObject.addProperty("type", "input_text");
        textObject.addProperty("text", content);
        contentArray.add(textObject);
        item.add("content", contentArray);
        return item;
    }

    private String buildErrorMessage(int code, String body, boolean appMode) {
        String detail = "";
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (root.has("error") && root.get("error").isJsonObject()) {
                JsonObject error = root.getAsJsonObject("error");
                detail = getString(error, "message");
                if (TextUtils.isEmpty(detail)) {
                    detail = getString(error, "detail");
                }
                if (TextUtils.isEmpty(detail)) {
                    detail = getString(error, "title");
                }
            } else {
                detail = getString(root, "message");
            }
        } catch (Exception ignored) {
            detail = "";
        }

        if (code == 401 || code == 403) {
            if (appMode && body != null && body.contains("App.AccessDenied")) {
                return "AI 应用无访问权限，请检查百炼应用发布状态或 API Key 授权";
            }
            return TextUtils.isEmpty(detail)
                    ? "AI 助手鉴权失败，请检查阿里云 API Key、App ID 或服务权限"
                    : "AI 助手鉴权失败：" + detail;
        }
        if (code == 404) {
            return "AI 助手接口地址不可用，请检查 ALIYUN_BAILIAN_BASE_URL 配置";
        }
        if (!TextUtils.isEmpty(detail)) {
            return "AI 助手请求失败：" + detail;
        }
        return "AI 助手请求失败：HTTP " + code;
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
