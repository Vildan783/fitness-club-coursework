package com.fitstudio.mobile

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DbException(message: String) : Exception(message)

@Entity(tableName = "Trainers")
data class TrainerEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "TrainerId") val trainerId: Int = 0,
    @ColumnInfo(name = "FullName") val fullName: String,
    @ColumnInfo(name = "Specialization") val specialization: String,
    @ColumnInfo(name = "Phone") val phone: String,
    @ColumnInfo(name = "HireDate") val hireDate: String,
    @ColumnInfo(name = "ExperienceYears") val experienceYears: Int
)

@Entity(
    tableName = "Halls",
    indices = [Index(value = ["Name"], unique = true)]
)
data class HallEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "HallId") val hallId: Int = 0,
    @ColumnInfo(name = "Name") val name: String,
    @ColumnInfo(name = "Capacity") val capacity: Int,
    @ColumnInfo(name = "FloorNumber") val floorNumber: Int
)

@Entity(tableName = "Clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "ClientId") val clientId: Int = 0,
    @ColumnInfo(name = "FullName") val fullName: String,
    @ColumnInfo(name = "Phone") val phone: String,
    @ColumnInfo(name = "Email") val email: String?,
    @ColumnInfo(name = "BirthDate") val birthDate: String,
    @ColumnInfo(name = "RegisteredAt") val registeredAt: String
)

@Entity(
    tableName = "MembershipPlans",
    indices = [Index(value = ["Name"], unique = true)]
)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "PlanId") val planId: Int = 0,
    @ColumnInfo(name = "Name") val name: String,
    @ColumnInfo(name = "DurationDays") val durationDays: Int,
    @ColumnInfo(name = "Price") val price: Double,
    @ColumnInfo(name = "VisitLimit") val visitLimit: Int?
)

@Entity(
    tableName = "Memberships",
    foreignKeys = [
        ForeignKey(entity = ClientEntity::class, parentColumns = ["ClientId"], childColumns = ["ClientId"]),
        ForeignKey(entity = PlanEntity::class, parentColumns = ["PlanId"], childColumns = ["PlanId"])
    ],
    indices = [Index("ClientId"), Index("PlanId")]
)
data class MembershipEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "MembershipId") val membershipId: Int = 0,
    @ColumnInfo(name = "ClientId") val clientId: Int,
    @ColumnInfo(name = "PlanId") val planId: Int,
    @ColumnInfo(name = "StartDate") val startDate: String,
    @ColumnInfo(name = "EndDate") val endDate: String,
    @ColumnInfo(name = "VisitsLeft") val visitsLeft: Int?,
    @ColumnInfo(name = "Status") val status: String
)

@Entity(
    tableName = "Classes",
    foreignKeys = [
        ForeignKey(entity = TrainerEntity::class, parentColumns = ["TrainerId"], childColumns = ["TrainerId"]),
        ForeignKey(entity = HallEntity::class, parentColumns = ["HallId"], childColumns = ["HallId"])
    ],
    indices = [Index("TrainerId"), Index("HallId")]
)
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "ClassId") val classId: Int = 0,
    @ColumnInfo(name = "Title") val title: String,
    @ColumnInfo(name = "TrainerId") val trainerId: Int,
    @ColumnInfo(name = "HallId") val hallId: Int,
    @ColumnInfo(name = "StartsAt") val startsAt: String,
    @ColumnInfo(name = "DurationMinutes") val durationMinutes: Int,
    @ColumnInfo(name = "MaxParticipants") val maxParticipants: Int
)

@Entity(
    tableName = "Enrollments",
    foreignKeys = [
        ForeignKey(entity = ClassEntity::class, parentColumns = ["ClassId"], childColumns = ["ClassId"]),
        ForeignKey(entity = ClientEntity::class, parentColumns = ["ClientId"], childColumns = ["ClientId"])
    ],
    indices = [Index(value = ["ClassId", "ClientId"], unique = true), Index("ClientId")]
)
data class EnrollmentEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "EnrollmentId") val enrollmentId: Int = 0,
    @ColumnInfo(name = "ClassId") val classId: Int,
    @ColumnInfo(name = "ClientId") val clientId: Int,
    @ColumnInfo(name = "EnrolledAt") val enrolledAt: String,
    @ColumnInfo(name = "Status") val status: String
)

@Entity(tableName = "AuditLog")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "AuditId") val auditId: Int = 0,
    @ColumnInfo(name = "TableName") val tableName: String,
    @ColumnInfo(name = "ActionName") val actionName: String,
    @ColumnInfo(name = "EntityId") val entityId: Int,
    @ColumnInfo(name = "Details") val details: String?,
    @ColumnInfo(name = "CreatedAt") val createdAt: String
)

data class ClassRow(
    @ColumnInfo(name = "ClassId") val classId: Int,
    @ColumnInfo(name = "Title") val title: String,
    @ColumnInfo(name = "TrainerId") val trainerId: Int,
    @ColumnInfo(name = "TrainerName") val trainerName: String,
    @ColumnInfo(name = "HallId") val hallId: Int,
    @ColumnInfo(name = "HallName") val hallName: String,
    @ColumnInfo(name = "StartsAt") val startsAt: String,
    @ColumnInfo(name = "DurationMinutes") val durationMinutes: Int,
    @ColumnInfo(name = "MaxParticipants") val maxParticipants: Int,
    @ColumnInfo(name = "taken") val taken: Int
)

data class MembershipRow(
    @ColumnInfo(name = "MembershipId") val membershipId: Int,
    @ColumnInfo(name = "PlanName") val planName: String,
    @ColumnInfo(name = "Price") val price: Double,
    @ColumnInfo(name = "StartDate") val startDate: String,
    @ColumnInfo(name = "EndDate") val endDate: String,
    @ColumnInfo(name = "VisitsLeft") val visitsLeft: Int?,
    @ColumnInfo(name = "Status") val status: String
)

data class EnrollmentRow(
    @ColumnInfo(name = "EnrollmentId") val enrollmentId: Int,
    @ColumnInfo(name = "ClientId") val clientId: Int,
    @ColumnInfo(name = "ClientName") val clientName: String,
    @ColumnInfo(name = "Status") val status: String,
    @ColumnInfo(name = "EnrolledAt") val enrolledAt: String
)

@Dao
abstract class ClubDao {
    @Query(
        """
        SELECT * FROM Clients
        WHERE :q = '' OR FullName LIKE '%' || :q || '%' OR Phone LIKE '%' || :q || '%'
           OR IFNULL(Email, '') LIKE '%' || :q || '%'
        ORDER BY FullName
        """
    )
    abstract fun searchClients(q: String): List<ClientEntity>

    @Query("SELECT * FROM Clients WHERE ClientId = :id")
    abstract fun client(id: Int): ClientEntity?

    @Insert
    abstract fun insertClient(row: ClientEntity): Long

    @Update
    abstract fun updateClient(row: ClientEntity)

    @Query("SELECT * FROM MembershipPlans ORDER BY PlanId")
    abstract fun plans(): List<PlanEntity>

    @Query("SELECT * FROM MembershipPlans WHERE PlanId = :id")
    abstract fun plan(id: Int): PlanEntity?

    @Query(
        """
        SELECT m.MembershipId, p.Name AS PlanName, p.Price, m.StartDate, m.EndDate, m.VisitsLeft, m.Status
        FROM Memberships m JOIN MembershipPlans p ON p.PlanId = m.PlanId
        WHERE m.ClientId = :clientId
        ORDER BY m.StartDate DESC, m.MembershipId DESC
        """
    )
    abstract fun memberships(clientId: Int): List<MembershipRow>

    @Query(
        """
        SELECT * FROM Memberships
        WHERE ClientId = :clientId AND Status = 'Активен'
          AND StartDate <= :today AND EndDate >= :today
        ORDER BY EndDate DESC LIMIT 1
        """
    )
    abstract fun activeMembership(clientId: Int, today: String): MembershipEntity?

    @Query("UPDATE Memberships SET Status = 'Истёк' WHERE ClientId = :clientId AND Status = 'Активен'")
    abstract fun expireActive(clientId: Int)

    @Insert
    abstract fun insertMembership(row: MembershipEntity): Long

    @Query("UPDATE Memberships SET VisitsLeft = VisitsLeft - 1 WHERE MembershipId = :id AND VisitsLeft IS NOT NULL")
    abstract fun spendVisit(id: Int)

    @Query("UPDATE Memberships SET VisitsLeft = VisitsLeft + 1 WHERE MembershipId = :id AND VisitsLeft IS NOT NULL")
    abstract fun returnVisit(id: Int)

    @Query("SELECT * FROM Trainers ORDER BY FullName")
    abstract fun trainers(): List<TrainerEntity>

    @Query("SELECT * FROM Halls ORDER BY HallId")
    abstract fun halls(): List<HallEntity>

    @Query("SELECT Capacity FROM Halls WHERE HallId = :id")
    abstract fun hallCapacity(id: Int): Int?

    @Query(
        """
        SELECT c.ClassId, c.Title, c.TrainerId, t.FullName AS TrainerName, c.HallId, h.Name AS HallName,
               c.StartsAt, c.DurationMinutes, c.MaxParticipants,
               (SELECT COUNT(*) FROM Enrollments e WHERE e.ClassId = c.ClassId AND e.Status != 'Отменён') AS taken
        FROM Classes c
        JOIN Trainers t ON t.TrainerId = c.TrainerId
        JOIN Halls h ON h.HallId = c.HallId
        WHERE :q = '' OR c.Title LIKE '%' || :q || '%' OR t.FullName LIKE '%' || :q || '%'
           OR h.Name LIKE '%' || :q || '%'
        ORDER BY c.StartsAt
        """
    )
    abstract fun searchClasses(q: String): List<ClassRow>

    @Query(
        """
        SELECT c.ClassId, c.Title, c.TrainerId, t.FullName AS TrainerName, c.HallId, h.Name AS HallName,
               c.StartsAt, c.DurationMinutes, c.MaxParticipants,
               (SELECT COUNT(*) FROM Enrollments e WHERE e.ClassId = c.ClassId AND e.Status != 'Отменён') AS taken
        FROM Classes c
        JOIN Trainers t ON t.TrainerId = c.TrainerId
        JOIN Halls h ON h.HallId = c.HallId
        WHERE c.ClassId = :id
        """
    )
    abstract fun classRow(id: Int): ClassRow?

    @Query("SELECT * FROM Classes WHERE ClassId = :id")
    abstract fun classEntity(id: Int): ClassEntity?

    @Insert
    abstract fun insertClass(row: ClassEntity): Long

    @Update
    abstract fun updateClass(row: ClassEntity)

    @Query(
        """
        SELECT COUNT(*) FROM Enrollments
        WHERE ClassId = :classId AND Status != 'Отменён'
        """
    )
    abstract fun taken(classId: Int): Int

    @Query(
        """
        SELECT e.EnrollmentId, e.ClientId, c.FullName AS ClientName, e.Status, e.EnrolledAt
        FROM Enrollments e JOIN Clients c ON c.ClientId = e.ClientId
        WHERE e.ClassId = :classId AND e.Status != 'Отменён'
        ORDER BY c.FullName
        """
    )
    abstract fun enrollments(classId: Int): List<EnrollmentRow>

    @Query("SELECT * FROM Enrollments WHERE EnrollmentId = :id")
    abstract fun enrollment(id: Int): EnrollmentEntity?

    @Query("SELECT * FROM Enrollments WHERE ClassId = :classId AND ClientId = :clientId")
    abstract fun enrollmentOf(classId: Int, clientId: Int): EnrollmentEntity?

    @Insert
    abstract fun insertEnrollment(row: EnrollmentEntity): Long

    @Query("UPDATE Enrollments SET Status = :status, EnrolledAt = :at WHERE EnrollmentId = :id")
    abstract fun setEnrollment(id: Int, status: String, at: String)

    @Transaction
    open fun issueMembership(clientId: Int, planId: Int, startDate: String) {
        if (client(clientId) == null) throw DbException("Клиент не найден.")
        val plan = plan(planId) ?: throw DbException("Тариф не найден.")
        expireActive(clientId)
        val end = LocalDate.parse(startDate).plusDays(plan.durationDays.toLong()).toString()
        insertMembership(
            MembershipEntity(
                clientId = clientId,
                planId = planId,
                startDate = startDate,
                endDate = end,
                visitsLeft = plan.visitLimit,
                status = "Активен"
            )
        )
    }

    @Transaction
    open fun enroll(classId: Int, clientId: Int) {
        if (client(clientId) == null) throw DbException("Клиент не найден.")
        if (classEntity(classId) == null) throw DbException("Занятие не найдено.")
        val today = LocalDate.now().toString()
        val pass = activeMembership(clientId, today) ?: throw DbException("У клиента нет активного абонемента.")
        if (pass.visitsLeft != null && pass.visitsLeft <= 0) throw DbException("Посещения по абонементу закончились.")
        val existing = enrollmentOf(classId, clientId)
        if (existing?.status == "Записан") throw DbException("Клиент уже записан на это занятие.")
        val room = classEntity(classId)!!
        if (taken(classId) >= room.maxParticipants) throw DbException("Свободных мест нет.")
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        if (existing != null) {
            setEnrollment(existing.enrollmentId, "Записан", now)
        } else {
            insertEnrollment(EnrollmentEntity(classId = classId, clientId = clientId, enrolledAt = now, status = "Записан"))
        }
        spendVisit(pass.membershipId)
    }

    @Transaction
    open fun cancelEnrollment(enrollmentId: Int) {
        val row = enrollment(enrollmentId) ?: throw DbException("Запись не найдена.")
        if (row.status == "Отменён") throw DbException("Запись уже отменена.")
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        setEnrollment(enrollmentId, "Отменён", now)
        val today = LocalDate.now().toString()
        val pass = activeMembership(row.clientId, today)
        if (pass?.visitsLeft != null) returnVisit(pass.membershipId)
    }
}

@Database(
    entities = [
        TrainerEntity::class, HallEntity::class, ClientEntity::class, PlanEntity::class,
        MembershipEntity::class, ClassEntity::class, EnrollmentEntity::class, AuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FitDatabase : RoomDatabase() {
    abstract fun dao(): ClubDao

    companion object {
        @Volatile private var instance: FitDatabase? = null

        fun get(context: Context): FitDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, FitDatabase::class.java, "fitstudio.db")
                    .addCallback(SeedCallback)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        installTriggers(db)
        seed(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        installTriggers(db)
    }
}

private fun installTriggers(db: SupportSQLiteDatabase) {
    for (sql in TRIGGER_SQL) {
        val name = sql.substringAfter("TRIGGER ").substringBefore("\n").trim()
        db.execSQL("DROP TRIGGER IF EXISTS $name")
        db.execSQL(sql.trim().removeSuffix(";"))
    }
}

private val TRIGGER_SQL = listOf(
    """
CREATE TRIGGER trg_Clients_Audit_Ins
AFTER INSERT ON Clients
BEGIN
    INSERT INTO AuditLog(TableName, ActionName, EntityId, Details, CreatedAt)
    VALUES ('Clients', 'INSERT', NEW.ClientId, NEW.FullName, datetime('now'));
END;
""",
    """
CREATE TRIGGER trg_Clients_Audit_Upd
AFTER UPDATE ON Clients
BEGIN
    INSERT INTO AuditLog(TableName, ActionName, EntityId, Details, CreatedAt)
    VALUES ('Clients', 'UPDATE', NEW.ClientId, NEW.FullName, datetime('now'));
END;
""",
    """
CREATE TRIGGER trg_Clients_Audit_Del
AFTER DELETE ON Clients
BEGIN
    INSERT INTO AuditLog(TableName, ActionName, EntityId, Details, CreatedAt)
    VALUES ('Clients', 'DELETE', OLD.ClientId, OLD.FullName, datetime('now'));
END;
""",
    """
CREATE TRIGGER trg_Classes_Hall_Ins
AFTER INSERT ON Classes
WHEN NEW.MaxParticipants > (SELECT Capacity FROM Halls WHERE HallId = NEW.HallId)
BEGIN
    SELECT RAISE(ABORT, 'Лимит занятия больше вместимости зала.');
END;
""",
    """
CREATE TRIGGER trg_Classes_Hall_Upd
AFTER UPDATE ON Classes
WHEN NEW.MaxParticipants > (SELECT Capacity FROM Halls WHERE HallId = NEW.HallId)
BEGIN
    SELECT RAISE(ABORT, 'Лимит занятия больше вместимости зала.');
END;
""",
    """
CREATE TRIGGER trg_Enrollments_Capacity_Ins
AFTER INSERT ON Enrollments
WHEN (SELECT COUNT(*) FROM Enrollments WHERE ClassId = NEW.ClassId AND Status != 'Отменён')
     > (SELECT MaxParticipants FROM Classes WHERE ClassId = NEW.ClassId)
BEGIN
    SELECT RAISE(ABORT, 'Превышена вместимость занятия.');
END;
""",
    """
CREATE TRIGGER trg_Enrollments_Capacity_Upd
AFTER UPDATE ON Enrollments
WHEN (SELECT COUNT(*) FROM Enrollments WHERE ClassId = NEW.ClassId AND Status != 'Отменён')
     > (SELECT MaxParticipants FROM Classes WHERE ClassId = NEW.ClassId)
BEGIN
    SELECT RAISE(ABORT, 'Превышена вместимость занятия.');
END;
""",
    """
CREATE TRIGGER trg_Memberships_Validate_Ins
AFTER INSERT ON Memberships
WHEN NEW.EndDate < NEW.StartDate OR (NEW.VisitsLeft IS NOT NULL AND NEW.VisitsLeft < 0)
BEGIN
    SELECT RAISE(ABORT, 'Некорректный срок или остаток абонемента.');
END;
""",
    """
CREATE TRIGGER trg_Memberships_Validate_Upd
AFTER UPDATE ON Memberships
WHEN NEW.EndDate < NEW.StartDate OR (NEW.VisitsLeft IS NOT NULL AND NEW.VisitsLeft < 0)
BEGIN
    SELECT RAISE(ABORT, 'Некорректный срок или остаток абонемента.');
END;
"""
)

private fun seed(db: SupportSQLiteDatabase) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val later = today.plusDays(2)
    fun at(day: LocalDate, hour: Int) = "$day %02d:00:00".format(hour)

    db.execSQL("INSERT INTO Trainers(FullName, Specialization, Phone, HireDate, ExperienceYears) VALUES ('Анна Соколова','Йога','+7 903 110-20-01','2021-03-01',7)")
    db.execSQL("INSERT INTO Trainers(FullName, Specialization, Phone, HireDate, ExperienceYears) VALUES ('Игорь Лебедев','Силовой тренинг','+7 903 110-20-02','2019-09-15',10)")
    db.execSQL("INSERT INTO Trainers(FullName, Specialization, Phone, HireDate, ExperienceYears) VALUES ('Марина Ким','Пилатес','+7 903 110-20-03','2022-01-20',5)")
    db.execSQL("INSERT INTO Trainers(FullName, Specialization, Phone, HireDate, ExperienceYears) VALUES ('Павел Орлов','Бокс','+7 903 110-20-04','2018-05-11',12)")
    db.execSQL("INSERT INTO Trainers(FullName, Specialization, Phone, HireDate, ExperienceYears) VALUES ('Елена Ветрова','Растяжка','+7 903 110-20-05','2023-04-02',4)")
    db.execSQL("INSERT INTO Trainers(FullName, Specialization, Phone, HireDate, ExperienceYears) VALUES ('Дмитрий Сафин','Функциональный тренинг','+7 903 110-20-06','2020-11-08',8)")

    db.execSQL("INSERT INTO Halls(Name, Capacity, FloorNumber) VALUES ('Большой зал',20,1)")
    db.execSQL("INSERT INTO Halls(Name, Capacity, FloorNumber) VALUES ('Зал единоборств',12,1)")
    db.execSQL("INSERT INTO Halls(Name, Capacity, FloorNumber) VALUES ('Кардио-зона',15,2)")
    db.execSQL("INSERT INTO Halls(Name, Capacity, FloorNumber) VALUES ('Зал йоги',16,2)")

    db.execSQL("INSERT INTO MembershipPlans(Name, DurationDays, Price, VisitLimit) VALUES ('Разовое занятие',1,700,1)")
    db.execSQL("INSERT INTO MembershipPlans(Name, DurationDays, Price, VisitLimit) VALUES ('Утренний',30,2500,12)")
    db.execSQL("INSERT INTO MembershipPlans(Name, DurationDays, Price, VisitLimit) VALUES ('8 занятий',30,4500,8)")
    db.execSQL("INSERT INTO MembershipPlans(Name, DurationDays, Price, VisitLimit) VALUES ('Месяц безлимит',30,5500,NULL)")
    db.execSQL("INSERT INTO MembershipPlans(Name, DurationDays, Price, VisitLimit) VALUES ('3 месяца безлимит',90,14000,NULL)")

    val clients = listOf(
        listOf("Смирнов Артём", "+7 916 200-00-01", "artem.smirnov@mail.ru", "1998-04-12"),
        listOf("Кузнецова Ольга", "+7 916 200-00-02", "olga.k@mail.ru", "1995-11-03"),
        listOf("Попов Никита", "+7 916 200-00-03", "nikita.popov@gmail.com", "2001-02-19"),
        listOf("Васильева Ирина", "+7 916 200-00-04", "irina.v@yandex.ru", "1992-07-28"),
        listOf("Морозов Кирилл", "+7 916 200-00-05", null, "1989-09-14"),
        listOf("Новикова Алина", "+7 916 200-00-06", "alina.n@mail.ru", "2000-12-01"),
        listOf("Фёдоров Павел", "+7 916 200-00-07", "pavel.f@mail.ru", "1997-06-23"),
        listOf("Соловьёва Мария", "+7 916 200-00-08", "maria.s@gmail.com", "1994-01-30"),
        listOf("Волков Денис", "+7 916 200-00-09", null, "1987-08-08"),
        listOf("Павлова Екатерина", "+7 916 200-00-10", "ekaterina.p@mail.ru", "1999-05-17"),
        listOf("Белов Андрей", "+7 916 200-00-11", "andrey.belov@mail.ru", "1993-03-09"),
        listOf("Орлова Дарья", "+7 916 200-00-12", "darya.orlova@mail.ru", "2002-10-21")
    )
    val stmt = db.compileStatement("INSERT INTO Clients(FullName, Phone, Email, BirthDate, RegisteredAt) VALUES (?,?,?,?,?)")
    for (row in clients) {
        stmt.clearBindings()
        stmt.bindString(1, row[0]!!)
        stmt.bindString(2, row[1]!!)
        if (row[2] == null) stmt.bindNull(3) else stmt.bindString(3, row[2]!!)
        stmt.bindString(4, row[3]!!)
        stmt.bindString(5, today.toString())
        stmt.executeInsert()
    }

    fun klass(title: String, trainer: Int, hall: Int, starts: String, minutes: Int, max: Int) {
        db.execSQL(
            "INSERT INTO Classes(Title, TrainerId, HallId, StartsAt, DurationMinutes, MaxParticipants) VALUES (?,?,?,?,?,?)",
            arrayOf<Any>(title, trainer, hall, starts, minutes, max)
        )
    }
    klass("Утренняя йога", 1, 4, at(today, 9), 60, 12)
    klass("Силовая база", 2, 1, at(today, 18), 55, 16)
    klass("Пилатес", 3, 4, at(tomorrow, 11), 50, 12)
    klass("Бокс для начинающих", 4, 2, at(tomorrow, 19), 60, 10)
    klass("Растяжка", 5, 4, at(today, 20), 45, 14)
    klass("Функциональный круг", 6, 1, at(tomorrow, 7), 45, 15)
    klass("Кардио-интервал", 6, 3, at(later, 18), 40, 12)
    klass("Вечерняя йога", 1, 4, at(later, 19), 60, 12)

    val end30 = today.plusDays(30).toString()
    fun pass(client: Int, plan: Int, visits: Int?) {
        db.execSQL(
            "INSERT INTO Memberships(ClientId, PlanId, StartDate, EndDate, VisitsLeft, Status) VALUES (?,?,?,?,?,?)",
            arrayOf<Any?>(client, plan, today.toString(), end30, visits, "Активен")
        )
    }
    for (id in 1..8) pass(id, 3, 8)
    pass(9, 4, null)
    pass(10, 4, null)
    pass(11, 2, 12)

    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    fun enroll(classId: Int, clientId: Int) {
        db.execSQL(
            "INSERT INTO Enrollments(ClassId, ClientId, EnrolledAt, Status) VALUES (?,?,?,?)",
            arrayOf<Any>(classId, clientId, now, "Записан")
        )
    }
    for (id in 1..8) enroll(1, id)
    listOf(1, 2, 9, 10).forEach { enroll(2, it) }
    db.execSQL("UPDATE Memberships SET VisitsLeft = 6 WHERE ClientId IN (1,2) AND Status = 'Активен'")
    db.execSQL("UPDATE Memberships SET VisitsLeft = 7 WHERE ClientId BETWEEN 3 AND 8 AND Status = 'Активен'")
}

class ClubStore private constructor(private val dao: ClubDao) {
    fun clients(query: String): List<Client> = dao.searchClients(query.trim()).map { it.toClient() }

    fun client(id: Int): Client = dao.client(id)?.toClient() ?: throw DbException("Клиент не найден.")

    fun saveClient(id: Int, name: String, phone: String, email: String?, birth: String): Client {
        if (name.trim().length < 3) throw DbException("Укажите ФИО клиента.")
        if (phone.trim().length < 6) throw DbException("Укажите телефон.")
        if (birth > LocalDate.now().toString()) throw DbException("Дата рождения не может быть в будущем.")
        val mail = email?.trim()?.ifBlank { null }
        if (id == 0) {
            val newId = dao.insertClient(
                ClientEntity(
                    fullName = name.trim(),
                    phone = phone.trim(),
                    email = mail,
                    birthDate = birth,
                    registeredAt = LocalDate.now().toString()
                )
            ).toInt()
            return client(newId)
        }
        val current = dao.client(id) ?: throw DbException("Клиент не найден.")
        dao.updateClient(current.copy(fullName = name.trim(), phone = phone.trim(), email = mail, birthDate = birth))
        return client(id)
    }

    fun plans(): List<Plan> = dao.plans().map { Plan(it.planId, it.name, it.price, it.visitLimit) }

    fun memberships(clientId: Int): List<Membership> = dao.memberships(clientId).map {
        Membership(it.membershipId, it.planName, it.price, it.startDate, it.endDate, it.visitsLeft, it.status)
    }

    fun issueMembership(clientId: Int, planId: Int, startDate: String) = guard { dao.issueMembership(clientId, planId, startDate) }

    fun classes(query: String): List<GymClass> = dao.searchClasses(query.trim()).map { it.toGym() }

    fun gymClass(id: Int): GymClass = dao.classRow(id)?.toGym() ?: throw DbException("Занятие не найдено.")

    fun trainers(): List<Named> = dao.trainers().map { Named(it.trainerId, "${it.fullName} · ${it.specialization}") }

    fun halls(): List<Named> = dao.halls().map { Named(it.hallId, "${it.name} · мест ${it.capacity}") }

    fun clientOptions(): List<Named> = dao.searchClients("").map { Named(it.clientId, it.fullName) }

    fun saveClass(id: Int, title: String, trainerId: Int, hallId: Int, startsAt: String, duration: Int, max: Int): GymClass {
        if (title.trim().length < 3) throw DbException("Укажите название занятия.")
        if (duration <= 0 || max <= 0) throw DbException("Длительность и лимит мест должны быть больше нуля.")
        val capacity = dao.hallCapacity(hallId) ?: throw DbException("Зал не найден.")
        if (max > capacity) throw DbException("Лимит занятия больше вместимости зала.")
        if (id == 0) {
            val newId = guard {
                dao.insertClass(
                    ClassEntity(
                        title = title.trim(),
                        trainerId = trainerId,
                        hallId = hallId,
                        startsAt = startsAt,
                        durationMinutes = duration,
                        maxParticipants = max
                    )
                ).toInt()
            }
            return gymClass(newId)
        }
        val current = dao.classEntity(id) ?: throw DbException("Занятие не найдено.")
        if (dao.taken(id) > max) throw DbException("На занятие уже записано больше людей, чем новый лимит.")
        guard {
            dao.updateClass(
                current.copy(
                    title = title.trim(),
                    trainerId = trainerId,
                    hallId = hallId,
                    startsAt = startsAt,
                    durationMinutes = duration,
                    maxParticipants = max
                )
            )
        }
        return gymClass(id)
    }

    fun enrollments(classId: Int): List<Enrollment> = dao.enrollments(classId).map {
        Enrollment(it.enrollmentId, it.clientId, it.clientName, it.status, it.enrolledAt)
    }

    fun enroll(classId: Int, clientId: Int) = guard { dao.enroll(classId, clientId) }

    fun cancelEnrollment(id: Int) = guard { dao.cancelEnrollment(id) }

    private fun <T> guard(block: () -> T): T {
        try {
            return block()
        } catch (ex: DbException) {
            throw ex
        } catch (ex: Exception) {
            val text = generateSequence<Throwable>(ex) { it.cause }
                .mapNotNull { it.message }
                .firstOrNull { it.contains("Лимит") || it.contains("Превышена") || it.contains("Некорректный") }
            throw DbException(text?.substringBefore(" (code") ?: (ex.message ?: "Ошибка базы"))
        }
    }

    companion object {
        fun get(context: Context) = ClubStore(FitDatabase.get(context).dao())
    }
}

fun Context.store(): ClubStore = ClubStore.get(this)

private fun ClientEntity.toClient() = Client(clientId, fullName, phone, email, birthDate, registeredAt)

private fun ClassRow.toGym() = GymClass(
    classId, title, trainerId, trainerName, hallId, hallName, startsAt, durationMinutes, maxParticipants, taken
)
