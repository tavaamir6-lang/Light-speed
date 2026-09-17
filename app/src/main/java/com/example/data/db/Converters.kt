package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.ProtocolType

class Converters {
    @TypeConverter
    fun fromProtocolType(type: ProtocolType): String = type.name

    @TypeConverter
    fun toProtocolType(value: String): ProtocolType {
        return try {
            ProtocolType.valueOf(value)
        } catch (e: Exception) {
            ProtocolType.VMESS
        }
    }
}
