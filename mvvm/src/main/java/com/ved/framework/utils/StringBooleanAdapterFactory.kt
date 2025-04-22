package com.ved.framework.utils

import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class StringBooleanAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        // 只处理String类型
        if (type.rawType != String::class.java) {
            return null
        }

        val delegate = gson.getDelegateAdapter(this, type)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                if (value == null){
                    out.nullValue()
                    return
                }
                delegate.write(out, value)
            }

            override fun read(reader: JsonReader): T {
                return when (reader.peek()) {
                    JsonToken.NULL ->{
                        //消费null值
                        reader.nextNull()
                        //返回空字符串
                        delegate.fromJsonTree(JsonPrimitive("")) as T
                    }
                    JsonToken.BOOLEAN -> {
                        // 当遇到布尔值时，转换为字符串
                        val booleanValue = reader.nextBoolean()
                        delegate.fromJsonTree(JsonPrimitive(booleanValue.toString())) as T
                    }
                    JsonToken.NUMBER -> {
                        // 也可以处理数字类型（如1/0）
                        val numberValue = reader.nextDouble()
                        delegate.fromJsonTree(JsonPrimitive((numberValue != 0.0).toString())) as T
                    }
                    else -> {
                        // 其他情况使用默认解析
                        delegate.read(reader)
                    }
                }
            }
        }
    }
}