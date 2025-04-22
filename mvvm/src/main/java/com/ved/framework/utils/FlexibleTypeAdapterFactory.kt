package com.ved.framework.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class FlexibleTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != Boolean::class.java && type.rawType != Boolean::class.javaObjectType) {
            return null
        }
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                delegate.write(out, value)
            }
            override fun read(reader: JsonReader): T {
                val jsonElement = JsonParser.parseReader(reader)
                return when {
                    jsonElement.isJsonNull -> delegate.fromJsonTree(jsonElement)
                    jsonElement.isJsonPrimitive -> {
                        val primitive = jsonElement.asJsonPrimitive
                        when {
                            primitive.isBoolean -> delegate.fromJsonTree(primitive)
                            primitive.isString -> {
                                val stringValue = primitive.asString
                                val booleanValue = when (stringValue.lowercase()) {
                                    "true", "1", "yes" -> true
                                    "false", "0", "no" -> false
                                    else -> throw JsonSyntaxException("Cannot parse '$stringValue' as Boolean")
                                }
                                delegate.fromJsonTree(JsonPrimitive(booleanValue))
                            }
                            primitive.isNumber -> {
                                val numberValue = primitive.asNumber
                                val booleanValue = numberValue.toInt() != 0
                                delegate.fromJsonTree(JsonPrimitive(booleanValue))
                            }
                            else -> delegate.fromJsonTree(jsonElement)
                        }
                    }
                    else -> delegate.fromJsonTree(jsonElement)
                }
            }
        }
    }
}