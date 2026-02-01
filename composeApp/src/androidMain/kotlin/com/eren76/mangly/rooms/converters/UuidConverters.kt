package com.eren76.mangly.rooms.converters

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.util.UUID

class UuidConverters {
    @TypeConverter
    fun fromBytes(bytes: ByteArray?): UUID? {
        if (bytes == null || bytes.size != 16) return null
        val bb = ByteBuffer.wrap(bytes)
        val high = bb.long
        val low = bb.long
        return UUID(high, low)
    }

    @TypeConverter
    fun uuidToBytes(uuid: UUID?): ByteArray? {
        if (uuid == null) return null
        val bb = ByteBuffer.allocate(16)
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}
