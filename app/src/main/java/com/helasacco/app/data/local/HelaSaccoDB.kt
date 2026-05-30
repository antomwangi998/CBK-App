package com.helasacco.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.helasacco.app.data.local.dao.*
import com.helasacco.app.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        BranchEntity::class,
        MemberEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        LoanEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(HelaSaccoConverters::class)
abstract class HelaSaccoDB : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun branchDao(): BranchDao
    abstract fun memberDao(): MemberDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditLogDao(): AuditLogDao
}

class HelaSaccoConverters {
    @TypeConverter
    fun fromList(list: List<String>?): String? = list?.joinToString(",")

    @TypeConverter
    fun toList(value: String?): List<String>? = value?.split(",")?.filter { it.isNotBlank() }
}
