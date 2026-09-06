package com.herwin.netez.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromAuthType(value: AuthType): String {
        return value.name
    }

    @TypeConverter
    fun toAuthType(value: String): AuthType {
        return AuthType.valueOf(value)
    }

    @TypeConverter
    fun fromStabilityMode(value: StabilityMode): String {
        return value.name
    }

    @TypeConverter
    fun toStabilityMode(value: String): StabilityMode {
        return StabilityMode.valueOf(value)
    }

    @TypeConverter
    fun fromMonitoringType(value: MonitoringType): String {
        return value.name
    }

    @TypeConverter
    fun toMonitoringType(value: String): MonitoringType {
        return MonitoringType.valueOf(value)
    }
}
