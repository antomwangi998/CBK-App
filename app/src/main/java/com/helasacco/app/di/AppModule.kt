package com.helasacco.app.di

import android.content.Context
import androidx.room.Room
import com.helasacco.app.data.local.HelaSaccoDB
import com.helasacco.app.data.local.dao.*
import com.helasacco.app.data.repository.*
import com.helasacco.app.ui.admin.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HelaSaccoDB =
        Room.databaseBuilder(context, HelaSaccoDB::class.java, "hela_sacco_v3.db")
            .fallbackToDestructiveMigrationOnDowngrade().build()

    @Provides fun provideUserDao(db: HelaSaccoDB): UserDao = db.userDao()
    @Provides fun provideBranchDao(db: HelaSaccoDB): BranchDao = db.branchDao()
    @Provides fun provideMemberDao(db: HelaSaccoDB): MemberDao = db.memberDao()
    @Provides fun provideAccountDao(db: HelaSaccoDB): AccountDao = db.accountDao()
    @Provides fun provideTransactionDao(db: HelaSaccoDB): TransactionDao = db.transactionDao()
    @Provides fun provideLoanDao(db: HelaSaccoDB): LoanDao = db.loanDao()
    @Provides fun provideNotificationDao(db: HelaSaccoDB): NotificationDao = db.notificationDao()
    @Provides fun provideAuditLogDao(db: HelaSaccoDB): AuditLogDao = db.auditLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton
    fun provideAuthRepository(userDao: UserDao, sessionManager: SessionManager): AuthRepository =
        AuthRepositoryImpl(userDao, sessionManager)

    @Provides @Singleton
    fun provideMemberRepository(memberDao: MemberDao): MemberRepository = MemberRepositoryImpl(memberDao)

    @Provides @Singleton
    fun provideAccountRepository(accountDao: AccountDao): AccountRepository = AccountRepositoryImpl(accountDao)

    @Provides @Singleton
    fun provideTransactionRepository(transactionDao: TransactionDao): TransactionRepository = TransactionRepositoryImpl(transactionDao)

    @Provides @Singleton
    fun provideLoanRepository(loanDao: LoanDao): LoanRepository = LoanRepositoryImpl(loanDao)

    @Provides @Singleton
    fun provideNotificationRepository(notificationDao: NotificationDao): NotificationRepository = NotificationRepositoryImpl(notificationDao)
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager = SessionManager(context)
}
