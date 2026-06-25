package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.group.data.entity.*

@Database(
    entities = [
        Expense::class,
        DebtDue::class,
        GroupEntity::class,
        MemberEntity::class,
        GroupExpenseEntity::class,
        ExpenseParticipantEntity::class,
        ExpensePayerEntity::class,
        SettlementEntity::class,
        GroupCategoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun debtDueDao(): DebtDueDao
    abstract fun groupDao(): com.example.group.data.dao.GroupDao

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
                db.execSQL("ALTER TABLE expenses ADD COLUMN imageUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN foodDetails TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN imageBytes BLOB DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table: groups
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `groups` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`currency` TEXT NOT NULL, " +
                            "`color` INTEGER NOT NULL, " +
                            "`description` TEXT, " +
                            "`createdDate` INTEGER NOT NULL, " +
                            "`isArchived` INTEGER NOT NULL DEFAULT 0" +
                            ")"
                )

                // Table: members
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `members` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`groupId` INTEGER NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`avatarUri` TEXT, " +
                            "`defaultWeight` REAL NOT NULL DEFAULT 1.0, " +
                            "`notes` TEXT, " +
                            "`isEnabled` INTEGER NOT NULL DEFAULT 1, " +
                            "`createdDate` INTEGER NOT NULL" +
                            ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_members_groupId` ON `members` (`groupId`)")

                // Table: group_expenses
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `group_expenses` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`groupId` INTEGER NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`amount` REAL NOT NULL, " +
                            "`currency` TEXT NOT NULL, " +
                            "`category` TEXT NOT NULL, " +
                            "`date` INTEGER NOT NULL, " +
                            "`notes` TEXT, " +
                            "`receiptBytes` BLOB, " +
                            "`createdDate` INTEGER NOT NULL, " +
                            "`updatedDate` INTEGER NOT NULL" +
                            ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_group_expenses_groupId` ON `group_expenses` (`groupId`)")

                // Table: expense_participants
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `expense_participants` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`expenseId` INTEGER NOT NULL, " +
                            "`memberId` INTEGER NOT NULL, " +
                            "`shareAmount` REAL NOT NULL, " +
                            "`splitMethod` TEXT NOT NULL, " +
                            "`rawWeight` REAL" +
                            ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_participants_expenseId` ON `expense_participants` (`expenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_participants_memberId` ON `expense_participants` (`memberId`)")

                // Table: expense_payers
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `expense_payers` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`expenseId` INTEGER NOT NULL, " +
                            "`memberId` INTEGER NOT NULL, " +
                            "`paidAmount` REAL NOT NULL" +
                            ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_payers_expenseId` ON `expense_payers` (`expenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_payers_memberId` ON `expense_payers` (`memberId`)")

                // Table: settlements
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `settlements` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`groupId` INTEGER NOT NULL, " +
                            "`payerId` INTEGER NOT NULL, " +
                            "`payeeId` INTEGER NOT NULL, " +
                            "`amount` REAL NOT NULL, " +
                            "`date` INTEGER NOT NULL, " +
                            "`notes` TEXT" +
                            ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlements_groupId` ON `settlements` (`groupId`)")

                // Table: group_categories
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `group_categories` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`groupId` INTEGER, " +
                            "`name` TEXT NOT NULL, " +
                            "`iconName` TEXT" +
                            ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_group_categories_groupId` ON `group_categories` (`groupId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
