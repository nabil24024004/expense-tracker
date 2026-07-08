package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Expense::class, DebtDue::class, Account::class, PlannedTransaction::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun debtDueDao(): DebtDueDao
    abstract fun accountDao(): AccountDao
    abstract fun plannedTransactionDao(): PlannedTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `debts_dues` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`personName` TEXT NOT NULL, " +
                            "`amount` REAL NOT NULL, " +
                            "`description` TEXT NOT NULL, " +
                            "`date` INTEGER NOT NULL, " +
                            "`dueDate` INTEGER, " +
                            "`type` TEXT NOT NULL, " +
                            "`isCleared` INTEGER NOT NULL DEFAULT 0, " +
                            "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                            "`sheetRow` INTEGER" +
                            ")"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'EXPENSE'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create accounts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `balance` REAL NOT NULL, 
                        `colorHex` TEXT NOT NULL, 
                        `icon` TEXT NOT NULL, 
                        `currency` TEXT NOT NULL, 
                        `includeInBalance` INTEGER NOT NULL DEFAULT 1, 
                        `displayOrder` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create planned_transactions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `planned_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `amount` REAL NOT NULL, 
                        `category` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `accountId` INTEGER NOT NULL, 
                        `startDate` INTEGER NOT NULL, 
                        `intervalType` TEXT NOT NULL, 
                        `intervalN` INTEGER NOT NULL, 
                        `oneTime` INTEGER NOT NULL, 
                        `nextDueDate` INTEGER NOT NULL, 
                        `isActive` INTEGER NOT NULL DEFAULT 1, 
                        `description` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                // Alter expenses table to add new columns
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `accountId` INTEGER")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `toAccountId` INTEGER")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")

                // Create default account "Cash"
                db.execSQL("""
                    INSERT INTO `accounts` (`name`, `balance`, `colorHex`, `icon`, `currency`, `includeInBalance`, `displayOrder`) 
                    VALUES ('Cash', 0.0, '#EA3B35', 'wallet', '৳', 1, 0)
                """.trimIndent())

                // Set all existing expenses to reference the default "Cash" account (id = 1)
                db.execSQL("UPDATE `expenses` SET `accountId` = 1")
            }
        }

        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insert Cash with a fixed id=1 so the ViewModel seed check is always consistent
                db.execSQL("""
                    INSERT INTO `accounts` (`id`, `name`, `balance`, `colorHex`, `icon`, `currency`, `includeInBalance`, `displayOrder`) 
                    VALUES (1, 'Cash', 0.0, '#EA3B35', 'wallet', '৳', 1, 0)
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(CALLBACK)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
