package podbrushkin.javafxtable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class MyObject {
    private final JsonObject obj;
    private String toString;

    public MyObject(JsonObject obj) {
        this.obj = obj;
    }

    public JsonElement getColumn(String key) {
        return obj.get(key);
    }

    public JsonElement getColumn(int index) {
        var iter = obj.entrySet().iterator();
        for (int i = 0; i < index; i++) {
            iter.next();
        }
        var val = iter.next().getValue();
        return val;
    }

    public String toString() {
        if (toString == null) { 
            toString = obj.toString();
        }
        return toString;
    }
}