/*
    База данных фитнес-клуба Fit Studio.
    SQL Server Express.
    Запуск в SSMS или: sqlcmd -S localhost\SQLEXPRESS -E -f 65001 -i FitStudio.sql
*/
USE master;
GO

IF DB_ID(N'FitStudio') IS NOT NULL
BEGIN
    ALTER DATABASE FitStudio SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE FitStudio;
END
GO

CREATE DATABASE FitStudio;
GO

USE FitStudio;
GO

/* ===================== Таблицы ===================== */

CREATE TABLE Trainers (
    TrainerId        INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    FullName         NVARCHAR(120) NOT NULL,
    Specialization   NVARCHAR(80)  NOT NULL,
    Phone            NVARCHAR(20)  NOT NULL,
    HireDate         DATE          NOT NULL,
    ExperienceYears  INT           NOT NULL,
    CONSTRAINT CK_Trainers_Experience CHECK (ExperienceYears >= 0)
);
GO

CREATE TABLE Halls (
    HallId       INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    Name         NVARCHAR(80) NOT NULL,
    Capacity     INT          NOT NULL,
    FloorNumber  INT          NOT NULL,
    CONSTRAINT UQ_Halls_Name UNIQUE (Name),
    CONSTRAINT CK_Halls_Capacity CHECK (Capacity > 0)
);
GO

CREATE TABLE Clients (
    ClientId      INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    FullName      NVARCHAR(120) NOT NULL,
    Phone         NVARCHAR(20)  NOT NULL,
    Email         NVARCHAR(120) NULL,
    BirthDate     DATE          NOT NULL,
    RegisteredAt  DATE          NOT NULL CONSTRAINT DF_Clients_Registered DEFAULT (CAST(GETDATE() AS DATE))
);
GO

CREATE TABLE MembershipPlans (
    PlanId        INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    Name          NVARCHAR(80)   NOT NULL,
    DurationDays  INT            NOT NULL,
    Price         DECIMAL(10,2)  NOT NULL,
    VisitLimit    INT            NULL,
    CONSTRAINT UQ_Plans_Name UNIQUE (Name),
    CONSTRAINT CK_Plans_Duration CHECK (DurationDays > 0),
    CONSTRAINT CK_Plans_Price CHECK (Price >= 0),
    CONSTRAINT CK_Plans_Visits CHECK (VisitLimit IS NULL OR VisitLimit > 0)
);
GO

CREATE TABLE Memberships (
    MembershipId  INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    ClientId      INT           NOT NULL,
    PlanId        INT           NOT NULL,
    StartDate     DATE          NOT NULL,
    EndDate       DATE          NOT NULL,
    VisitsLeft    INT           NULL,
    Status        NVARCHAR(20)  NOT NULL,
    CONSTRAINT FK_Memberships_Client FOREIGN KEY (ClientId) REFERENCES Clients(ClientId),
    CONSTRAINT FK_Memberships_Plan FOREIGN KEY (PlanId) REFERENCES MembershipPlans(PlanId),
    CONSTRAINT CK_Memberships_Status CHECK (Status IN (N'Активен', N'Истёк', N'Заморожен')),
    CONSTRAINT CK_Memberships_Visits CHECK (VisitsLeft IS NULL OR VisitsLeft >= 0)
);
GO

CREATE TABLE Classes (
    ClassId           INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    Title             NVARCHAR(120) NOT NULL,
    TrainerId         INT           NOT NULL,
    HallId            INT           NOT NULL,
    StartsAt          DATETIME2     NOT NULL,
    DurationMinutes   INT           NOT NULL,
    MaxParticipants   INT           NOT NULL,
    CONSTRAINT FK_Classes_Trainer FOREIGN KEY (TrainerId) REFERENCES Trainers(TrainerId),
    CONSTRAINT FK_Classes_Hall FOREIGN KEY (HallId) REFERENCES Halls(HallId),
    CONSTRAINT CK_Classes_Duration CHECK (DurationMinutes > 0),
    CONSTRAINT CK_Classes_Max CHECK (MaxParticipants > 0)
);
GO

CREATE TABLE Enrollments (
    EnrollmentId  INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    ClassId       INT           NOT NULL,
    ClientId      INT           NOT NULL,
    EnrolledAt    DATETIME2     NOT NULL CONSTRAINT DF_Enrollments_At DEFAULT (SYSDATETIME()),
    Status        NVARCHAR(20)  NOT NULL,
    CONSTRAINT FK_Enrollments_Class FOREIGN KEY (ClassId) REFERENCES Classes(ClassId),
    CONSTRAINT FK_Enrollments_Client FOREIGN KEY (ClientId) REFERENCES Clients(ClientId),
    CONSTRAINT UQ_Enrollments_ClassClient UNIQUE (ClassId, ClientId),
    CONSTRAINT CK_Enrollments_Status CHECK (Status IN (N'Записан', N'Отменён', N'Посетил'))
);
GO

CREATE TABLE AuditLog (
    AuditId     INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    TableName   NVARCHAR(64)  NOT NULL,
    ActionName  NVARCHAR(32)  NOT NULL,
    EntityId    INT           NOT NULL,
    Details     NVARCHAR(400) NULL,
    CreatedAt   DATETIME2     NOT NULL CONSTRAINT DF_Audit_At DEFAULT (SYSDATETIME())
);
GO

CREATE INDEX IX_Memberships_Client ON Memberships(ClientId);
CREATE INDEX IX_Classes_Trainer ON Classes(TrainerId);
CREATE INDEX IX_Classes_Starts ON Classes(StartsAt);
CREATE INDEX IX_Enrollments_Class ON Enrollments(ClassId);
CREATE INDEX IX_Clients_Name ON Clients(FullName);
GO

/* ===================== Триггеры ===================== */

CREATE TRIGGER trg_Clients_Audit
ON Clients
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO AuditLog (TableName, ActionName, EntityId, Details)
    SELECT
        N'Clients',
        CASE
            WHEN i.ClientId IS NOT NULL AND d.ClientId IS NOT NULL THEN N'UPDATE'
            WHEN i.ClientId IS NOT NULL THEN N'INSERT'
            ELSE N'DELETE'
        END,
        COALESCE(i.ClientId, d.ClientId),
        COALESCE(i.FullName, d.FullName)
    FROM inserted i
    FULL JOIN deleted d ON d.ClientId = i.ClientId;
END
GO

CREATE TRIGGER trg_Classes_HallCapacity
ON Classes
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        INNER JOIN Halls h ON h.HallId = i.HallId
        WHERE i.MaxParticipants > h.Capacity
    )
    BEGIN
        THROW 50013, N'Лимит занятия больше вместимости зала.', 1;
    END
END
GO

CREATE TRIGGER trg_Enrollments_Capacity
ON Enrollments
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        INNER JOIN Classes c ON c.ClassId = i.ClassId
        CROSS APPLY (
            SELECT COUNT(*) AS Cnt
            FROM Enrollments e
            WHERE e.ClassId = i.ClassId
              AND e.Status <> N'Отменён'
        ) x
        WHERE x.Cnt > c.MaxParticipants
    )
    BEGIN
        THROW 50010, N'Превышена вместимость занятия.', 1;
    END
END
GO

CREATE TRIGGER trg_Memberships_Validate
ON Memberships
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM inserted WHERE EndDate < StartDate)
    BEGIN
        THROW 50011, N'Дата окончания абонемента раньше даты начала.', 1;
    END

    IF EXISTS (SELECT 1 FROM inserted WHERE VisitsLeft < 0)
    BEGIN
        THROW 50012, N'Остаток посещений не может быть отрицательным.', 1;
    END
END
GO

/* ===================== Процедуры ===================== */

CREATE PROCEDURE usp_Client_Search
    @Query NVARCHAR(120) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT ClientId, FullName, Phone, Email, BirthDate, RegisteredAt
    FROM Clients
    WHERE @Query IS NULL
       OR LTRIM(RTRIM(@Query)) = N''
       OR FullName LIKE N'%' + @Query + N'%'
       OR Phone LIKE N'%' + @Query + N'%'
       OR Email LIKE N'%' + @Query + N'%'
    ORDER BY FullName;
END
GO

CREATE PROCEDURE usp_Client_Get
    @ClientId INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT ClientId, FullName, Phone, Email, BirthDate, RegisteredAt
    FROM Clients
    WHERE ClientId = @ClientId;
END
GO

CREATE PROCEDURE usp_Client_Create
    @FullName  NVARCHAR(120),
    @Phone     NVARCHAR(20),
    @Email     NVARCHAR(120) = NULL,
    @BirthDate DATE
AS
BEGIN
    SET NOCOUNT ON;

    IF LEN(LTRIM(RTRIM(@FullName))) < 3
        THROW 50040, N'Укажите ФИО клиента.', 1;
    IF LEN(LTRIM(RTRIM(@Phone))) < 6
        THROW 50041, N'Укажите телефон.', 1;
    IF @BirthDate > CAST(GETDATE() AS DATE)
        THROW 50042, N'Дата рождения не может быть в будущем.', 1;
    IF @Email = N'' SET @Email = NULL;

    DECLARE @New TABLE (ClientId INT);

    INSERT INTO Clients (FullName, Phone, Email, BirthDate)
    OUTPUT INSERTED.ClientId INTO @New
    VALUES (LTRIM(RTRIM(@FullName)), LTRIM(RTRIM(@Phone)), @Email, @BirthDate);

    SELECT ClientId FROM @New;
END
GO

CREATE PROCEDURE usp_Client_Update
    @ClientId  INT,
    @FullName  NVARCHAR(120),
    @Phone     NVARCHAR(20),
    @Email     NVARCHAR(120) = NULL,
    @BirthDate DATE
AS
BEGIN
    SET NOCOUNT ON;

    IF LEN(LTRIM(RTRIM(@FullName))) < 3
        THROW 50040, N'Укажите ФИО клиента.', 1;
    IF LEN(LTRIM(RTRIM(@Phone))) < 6
        THROW 50041, N'Укажите телефон.', 1;
    IF @BirthDate > CAST(GETDATE() AS DATE)
        THROW 50042, N'Дата рождения не может быть в будущем.', 1;
    IF @Email = N'' SET @Email = NULL;

    UPDATE Clients
    SET FullName = LTRIM(RTRIM(@FullName)),
        Phone = LTRIM(RTRIM(@Phone)),
        Email = @Email,
        BirthDate = @BirthDate
    WHERE ClientId = @ClientId;

    IF @@ROWCOUNT = 0
        THROW 50043, N'Клиент не найден.', 1;
END
GO

CREATE PROCEDURE usp_Trainer_List
AS
BEGIN
    SET NOCOUNT ON;
    SELECT TrainerId, FullName, Specialization, Phone, ExperienceYears
    FROM Trainers
    ORDER BY FullName;
END
GO

CREATE PROCEDURE usp_Hall_List
AS
BEGIN
    SET NOCOUNT ON;
    SELECT HallId, Name, Capacity, FloorNumber
    FROM Halls
    ORDER BY Name;
END
GO

CREATE PROCEDURE usp_Plan_List
AS
BEGIN
    SET NOCOUNT ON;
    SELECT PlanId, Name, DurationDays, Price, VisitLimit
    FROM MembershipPlans
    ORDER BY Price;
END
GO

CREATE PROCEDURE usp_Membership_List
    @ClientId INT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE Memberships
    SET Status = N'Истёк'
    WHERE ClientId = @ClientId
      AND Status = N'Активен'
      AND EndDate < CAST(GETDATE() AS DATE);

    SELECT m.MembershipId, m.ClientId, p.Name AS PlanName, p.Price,
           m.StartDate, m.EndDate, m.VisitsLeft, m.Status
    FROM Memberships m
    INNER JOIN MembershipPlans p ON p.PlanId = m.PlanId
    WHERE m.ClientId = @ClientId
    ORDER BY m.StartDate DESC;
END
GO

CREATE PROCEDURE usp_Membership_Issue
    @ClientId  INT,
    @PlanId    INT,
    @StartDate DATE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        IF NOT EXISTS (SELECT 1 FROM Clients WHERE ClientId = @ClientId)
            THROW 50031, N'Клиент не найден.', 1;

        DECLARE @Days INT, @Limit INT;
        SELECT @Days = DurationDays, @Limit = VisitLimit
        FROM MembershipPlans
        WHERE PlanId = @PlanId;

        IF @Days IS NULL
            THROW 50030, N'Тариф не найден.', 1;

        UPDATE Memberships
        SET Status = N'Истёк'
        WHERE ClientId = @ClientId
          AND Status = N'Активен';

        DECLARE @New TABLE (MembershipId INT);

        INSERT INTO Memberships (ClientId, PlanId, StartDate, EndDate, VisitsLeft, Status)
        OUTPUT INSERTED.MembershipId INTO @New
        VALUES (
            @ClientId,
            @PlanId,
            @StartDate,
            DATEADD(DAY, @Days, @StartDate),
            @Limit,
            N'Активен'
        );

        COMMIT TRANSACTION;

        SELECT MembershipId FROM @New;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END
GO

CREATE PROCEDURE usp_Class_Search
    @Query NVARCHAR(120) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        c.ClassId,
        c.Title,
        c.TrainerId,
        t.FullName AS TrainerName,
        c.HallId,
        h.Name AS HallName,
        c.StartsAt,
        c.DurationMinutes,
        c.MaxParticipants,
        (
            SELECT COUNT(*)
            FROM Enrollments e
            WHERE e.ClassId = c.ClassId
              AND e.Status <> N'Отменён'
        ) AS Taken
    FROM Classes c
    INNER JOIN Trainers t ON t.TrainerId = c.TrainerId
    INNER JOIN Halls h ON h.HallId = c.HallId
    WHERE @Query IS NULL
       OR LTRIM(RTRIM(@Query)) = N''
       OR c.Title LIKE N'%' + @Query + N'%'
       OR t.FullName LIKE N'%' + @Query + N'%'
       OR h.Name LIKE N'%' + @Query + N'%'
    ORDER BY c.StartsAt;
END
GO

CREATE PROCEDURE usp_Class_Get
    @ClassId INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        c.ClassId,
        c.Title,
        c.TrainerId,
        t.FullName AS TrainerName,
        c.HallId,
        h.Name AS HallName,
        c.StartsAt,
        c.DurationMinutes,
        c.MaxParticipants,
        (
            SELECT COUNT(*)
            FROM Enrollments e
            WHERE e.ClassId = c.ClassId
              AND e.Status <> N'Отменён'
        ) AS Taken
    FROM Classes c
    INNER JOIN Trainers t ON t.TrainerId = c.TrainerId
    INNER JOIN Halls h ON h.HallId = c.HallId
    WHERE c.ClassId = @ClassId;
END
GO

CREATE PROCEDURE usp_Class_Create
    @Title            NVARCHAR(120),
    @TrainerId        INT,
    @HallId           INT,
    @StartsAt         DATETIME2,
    @DurationMinutes  INT,
    @MaxParticipants  INT
AS
BEGIN
    SET NOCOUNT ON;

    IF LEN(LTRIM(RTRIM(@Title))) < 3
        THROW 50060, N'Укажите название занятия.', 1;
    IF NOT EXISTS (SELECT 1 FROM Trainers WHERE TrainerId = @TrainerId)
        THROW 50061, N'Тренер не найден.', 1;
    IF NOT EXISTS (SELECT 1 FROM Halls WHERE HallId = @HallId)
        THROW 50062, N'Зал не найден.', 1;
    IF @DurationMinutes <= 0 OR @MaxParticipants <= 0
        THROW 50063, N'Длительность и лимит мест должны быть больше нуля.', 1;

    DECLARE @New TABLE (ClassId INT);

    INSERT INTO Classes (Title, TrainerId, HallId, StartsAt, DurationMinutes, MaxParticipants)
    OUTPUT INSERTED.ClassId INTO @New
    VALUES (LTRIM(RTRIM(@Title)), @TrainerId, @HallId, @StartsAt, @DurationMinutes, @MaxParticipants);

    SELECT ClassId FROM @New;
END
GO

CREATE PROCEDURE usp_Class_Update
    @ClassId          INT,
    @Title            NVARCHAR(120),
    @TrainerId        INT,
    @HallId           INT,
    @StartsAt         DATETIME2,
    @DurationMinutes  INT,
    @MaxParticipants  INT
AS
BEGIN
    SET NOCOUNT ON;

    IF LEN(LTRIM(RTRIM(@Title))) < 3
        THROW 50060, N'Укажите название занятия.', 1;
    IF NOT EXISTS (SELECT 1 FROM Classes WHERE ClassId = @ClassId)
        THROW 50064, N'Занятие не найдено.', 1;

    DECLARE @Taken INT;
    SELECT @Taken = COUNT(*)
    FROM Enrollments
    WHERE ClassId = @ClassId
      AND Status <> N'Отменён';

    IF @Taken > @MaxParticipants
        THROW 50065, N'На занятие уже записано больше людей, чем новый лимит.', 1;

    UPDATE Classes
    SET Title = LTRIM(RTRIM(@Title)),
        TrainerId = @TrainerId,
        HallId = @HallId,
        StartsAt = @StartsAt,
        DurationMinutes = @DurationMinutes,
        MaxParticipants = @MaxParticipants
    WHERE ClassId = @ClassId;
END
GO

CREATE PROCEDURE usp_Enrollment_List
    @ClassId INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT e.EnrollmentId, e.ClassId, e.ClientId, c.FullName AS ClientName, e.Status, e.EnrolledAt
    FROM Enrollments e
    INNER JOIN Clients c ON c.ClientId = e.ClientId
    WHERE e.ClassId = @ClassId
      AND e.Status <> N'Отменён'
    ORDER BY e.EnrolledAt;
END
GO

CREATE PROCEDURE usp_Enrollment_Create
    @ClassId  INT,
    @ClientId INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        IF NOT EXISTS (SELECT 1 FROM Clients WHERE ClientId = @ClientId)
            THROW 50020, N'Клиент не найден.', 1;
        IF NOT EXISTS (SELECT 1 FROM Classes WHERE ClassId = @ClassId)
            THROW 50021, N'Занятие не найдено.', 1;

        DECLARE @MembershipId INT, @VisitsLeft INT;

        SELECT TOP (1)
            @MembershipId = MembershipId,
            @VisitsLeft = VisitsLeft
        FROM Memberships
        WHERE ClientId = @ClientId
          AND Status = N'Активен'
          AND StartDate <= CAST(GETDATE() AS DATE)
          AND EndDate >= CAST(GETDATE() AS DATE)
        ORDER BY EndDate DESC;

        IF @MembershipId IS NULL
            THROW 50022, N'У клиента нет активного абонемента.', 1;

        IF @VisitsLeft IS NOT NULL AND @VisitsLeft <= 0
            THROW 50023, N'Посещения по абонементу закончились.', 1;

        DECLARE @ExistingId INT, @ExistingStatus NVARCHAR(20);
        SELECT @ExistingId = EnrollmentId, @ExistingStatus = Status
        FROM Enrollments
        WHERE ClassId = @ClassId
          AND ClientId = @ClientId;

        IF @ExistingStatus IS NOT NULL AND @ExistingStatus <> N'Отменён'
            THROW 50024, N'Клиент уже записан на это занятие.', 1;

        DECLARE @Max INT, @Taken INT;
        SELECT @Max = MaxParticipants FROM Classes WHERE ClassId = @ClassId;
        SELECT @Taken = COUNT(*)
        FROM Enrollments
        WHERE ClassId = @ClassId
          AND Status <> N'Отменён';

        IF @Taken >= @Max
            THROW 50025, N'Свободных мест нет.', 1;

        DECLARE @EnrollmentId INT;

        IF @ExistingId IS NULL
        BEGIN
            DECLARE @New TABLE (EnrollmentId INT);
            INSERT INTO Enrollments (ClassId, ClientId, Status)
            OUTPUT INSERTED.EnrollmentId INTO @New
            VALUES (@ClassId, @ClientId, N'Записан');
            SELECT @EnrollmentId = EnrollmentId FROM @New;
        END
        ELSE
        BEGIN
            UPDATE Enrollments
            SET Status = N'Записан',
                EnrolledAt = SYSDATETIME()
            WHERE EnrollmentId = @ExistingId;
            SET @EnrollmentId = @ExistingId;
        END

        IF @VisitsLeft IS NOT NULL
        BEGIN
            UPDATE Memberships
            SET VisitsLeft = VisitsLeft - 1
            WHERE MembershipId = @MembershipId;
        END

        COMMIT TRANSACTION;

        SELECT @EnrollmentId AS EnrollmentId;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END
GO

CREATE PROCEDURE usp_Enrollment_Cancel
    @EnrollmentId INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @Status NVARCHAR(20), @ClientId INT;

        SELECT @Status = Status, @ClientId = ClientId
        FROM Enrollments
        WHERE EnrollmentId = @EnrollmentId;

        IF @Status IS NULL
            THROW 50050, N'Запись не найдена.', 1;
        IF @Status = N'Отменён'
            THROW 50051, N'Запись уже отменена.', 1;

        UPDATE Enrollments
        SET Status = N'Отменён'
        WHERE EnrollmentId = @EnrollmentId;

        ;WITH ActivePass AS (
            SELECT TOP (1) MembershipId
            FROM Memberships
            WHERE ClientId = @ClientId
              AND Status = N'Активен'
              AND VisitsLeft IS NOT NULL
            ORDER BY EndDate DESC
        )
        UPDATE Memberships
        SET VisitsLeft = VisitsLeft + 1
        WHERE MembershipId IN (SELECT MembershipId FROM ActivePass);

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END
GO

/* ===================== Начальные данные ===================== */

INSERT INTO Trainers (FullName, Specialization, Phone, HireDate, ExperienceYears) VALUES
(N'Анна Соколова', N'Йога', N'+7 903 110-20-01', '2021-03-01', 7),
(N'Игорь Лебедев', N'Силовой тренинг', N'+7 903 110-20-02', '2019-09-15', 10),
(N'Марина Ким', N'Пилатес', N'+7 903 110-20-03', '2022-01-20', 5),
(N'Павел Орлов', N'Бокс', N'+7 903 110-20-04', '2018-05-11', 12),
(N'Елена Ветрова', N'Растяжка', N'+7 903 110-20-05', '2023-04-02', 4),
(N'Дмитрий Сафин', N'Функциональный тренинг', N'+7 903 110-20-06', '2020-11-08', 8);
GO

INSERT INTO Halls (Name, Capacity, FloorNumber) VALUES
(N'Большой зал', 20, 1),
(N'Зал единоборств', 12, 1),
(N'Кардио-зона', 15, 2),
(N'Зал йоги', 16, 2);
GO

INSERT INTO MembershipPlans (Name, DurationDays, Price, VisitLimit) VALUES
(N'Разовое занятие', 1, 700, 1),
(N'Утренний', 30, 2500, 12),
(N'8 занятий', 30, 4500, 8),
(N'Месяц безлимит', 30, 5500, NULL),
(N'3 месяца безлимит', 90, 14000, NULL);
GO

INSERT INTO Clients (FullName, Phone, Email, BirthDate) VALUES
(N'Смирнов Артём', N'+7 916 200-00-01', N'artem.smirnov@mail.ru', '1998-04-12'),
(N'Кузнецова Ольга', N'+7 916 200-00-02', N'olga.k@mail.ru', '1995-11-03'),
(N'Попов Никита', N'+7 916 200-00-03', N'nikita.popov@gmail.com', '2001-02-19'),
(N'Васильева Ирина', N'+7 916 200-00-04', N'irina.v@yandex.ru', '1992-07-28'),
(N'Морозов Кирилл', N'+7 916 200-00-05', NULL, '1989-09-14'),
(N'Новикова Алина', N'+7 916 200-00-06', N'alina.n@mail.ru', '2000-12-01'),
(N'Фёдоров Павел', N'+7 916 200-00-07', N'pavel.f@mail.ru', '1997-06-23'),
(N'Соловьёва Мария', N'+7 916 200-00-08', N'maria.s@gmail.com', '1994-01-30'),
(N'Волков Денис', N'+7 916 200-00-09', NULL, '1987-08-08'),
(N'Павлова Екатерина', N'+7 916 200-00-10', N'ekaterina.p@mail.ru', '1999-05-17'),
(N'Белов Андрей', N'+7 916 200-00-11', N'andrey.belov@mail.ru', '1993-03-09'),
(N'Орлова Дарья', N'+7 916 200-00-12', N'darya.orlova@mail.ru', '2002-10-21');
GO

DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @Tomorrow DATETIME2 = DATEADD(DAY, 1, CAST(@Today AS DATETIME2));

INSERT INTO Classes (Title, TrainerId, HallId, StartsAt, DurationMinutes, MaxParticipants) VALUES
(N'Утренняя йога', 1, 4, DATEADD(HOUR, 9, CAST(@Today AS DATETIME2)), 60, 12),
(N'Силовая база', 2, 1, DATEADD(HOUR, 18, CAST(@Today AS DATETIME2)), 55, 16),
(N'Пилатес', 3, 4, DATEADD(HOUR, 11, @Tomorrow), 50, 12),
(N'Бокс для начинающих', 4, 2, DATEADD(HOUR, 19, @Tomorrow), 60, 10),
(N'Растяжка', 5, 4, DATEADD(HOUR, 20, CAST(@Today AS DATETIME2)), 45, 14),
(N'Функциональный круг', 6, 1, DATEADD(HOUR, 7, @Tomorrow), 45, 15),
(N'Кардио-интервал', 6, 3, DATEADD(DAY, 2, DATEADD(HOUR, 18, CAST(@Today AS DATETIME2))), 40, 12),
(N'Вечерняя йога', 1, 4, DATEADD(DAY, 2, DATEADD(HOUR, 19, CAST(@Today AS DATETIME2))), 60, 12);
GO

/* Абонементы и записи идут через процедуры: так отрабатывают транзакции и триггеры. */
DECLARE @Start DATE = CAST(GETDATE() AS DATE);

EXEC usp_Membership_Issue @ClientId = 1,  @PlanId = 3, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 2,  @PlanId = 3, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 3,  @PlanId = 3, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 4,  @PlanId = 3, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 5,  @PlanId = 3, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 6,  @PlanId = 3, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 7,  @PlanId = 3, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 8,  @PlanId = 3, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 9,  @PlanId = 4, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 10, @PlanId = 4, @StartDate = @Start;
EXEC usp_Membership_Issue @ClientId = 11, @PlanId = 2, @StartDate = @Start;

EXEC usp_Enrollment_Create @ClassId = 1, @ClientId = 1;
EXEC usp_Enrollment_Create @ClassId = 1, @ClientId = 2;
EXEC usp_Enrollment_Create @ClassId = 1, @ClientId = 3;
EXEC usp_Enrollment_Create @ClassId = 1, @ClientId = 4;
EXEC usp_Enrollment_Create @ClassId = 1, @ClientId = 5;
EXEC usp_Enrollment_Create @ClassId = 1, @ClientId = 6;
EXEC usp_Enrollment_Create @ClassId = 1, @ClientId = 7;
EXEC usp_Enrollment_Create @ClassId = 1, @ClientId = 8;
EXEC usp_Enrollment_Create @ClassId = 2, @ClientId = 1;
EXEC usp_Enrollment_Create @ClassId = 2, @ClientId = 2;
EXEC usp_Enrollment_Create @ClassId = 2, @ClientId = 9;
EXEC usp_Enrollment_Create @ClassId = 2, @ClientId = 10;
GO
