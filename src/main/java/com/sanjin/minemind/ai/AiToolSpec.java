package com.sanjin.minemind.ai;

import com.google.gson.JsonObject;

public record AiToolSpec(String name, String label, String description, JsonObject parameters, boolean readOnly) {
    public AiToolSpec {
        name = name == null ? "" : name.trim();
        label = label == null ? name : label.trim();
        description = description == null ? "" : description.trim();
        parameters = parameters == null ? new JsonObject() : parameters.deepCopy();
    }
}
