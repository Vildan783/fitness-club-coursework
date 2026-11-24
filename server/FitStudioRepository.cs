using Microsoft.Data.SqlClient;

namespace FitStudio.Api;

public class FitStudioRepository(IConfiguration configuration)
{
    private readonly string _connectionString = configuration.GetConnectionString("FitStudio")
        ?? throw new InvalidOperationException("Не задана строка подключения FitStudio.");

    public Task<List<ClientDto>> SearchClients(string? query) =>
        Query("usp_Client_Search", r => new ClientDto(
            r.GetInt32(0), r.GetString(1), r.GetString(2), GetStringOrNull(r, 3), r.GetDateTime(4), r.GetDateTime(5)),
            new SqlParameter("@Query", (object?)query ?? DBNull.Value));

    public async Task<ClientDto?> GetClient(int id)
    {
        var rows = await Query("usp_Client_Get", r => new ClientDto(
            r.GetInt32(0), r.GetString(1), r.GetString(2), GetStringOrNull(r, 3), r.GetDateTime(4), r.GetDateTime(5)),
            new SqlParameter("@ClientId", id));
        return rows.FirstOrDefault();
    }

    public Task<int> CreateClient(ClientWrite body) =>
        Scalar("usp_Client_Create",
            new SqlParameter("@FullName", body.FullName),
            new SqlParameter("@Phone", body.Phone),
            new SqlParameter("@Email", (object?)body.Email ?? DBNull.Value),
            new SqlParameter("@BirthDate", body.BirthDate.Date));

    public Task UpdateClient(int id, ClientWrite body) =>
        Execute("usp_Client_Update",
            new SqlParameter("@ClientId", id),
            new SqlParameter("@FullName", body.FullName),
            new SqlParameter("@Phone", body.Phone),
            new SqlParameter("@Email", (object?)body.Email ?? DBNull.Value),
            new SqlParameter("@BirthDate", body.BirthDate.Date));

    public Task<List<TrainerDto>> Trainers() =>
        Query("usp_Trainer_List", r => new TrainerDto(
            r.GetInt32(0), r.GetString(1), r.GetString(2), r.GetString(3), r.GetInt32(4)));

    public Task<List<HallDto>> Halls() =>
        Query("usp_Hall_List", r => new HallDto(r.GetInt32(0), r.GetString(1), r.GetInt32(2), r.GetInt32(3)));

    public Task<List<PlanDto>> Plans() =>
        Query("usp_Plan_List", r => new PlanDto(
            r.GetInt32(0), r.GetString(1), r.GetInt32(2), r.GetDecimal(3), GetIntOrNull(r, 4)));

    public Task<List<MembershipDto>> Memberships(int clientId) =>
        Query("usp_Membership_List", r => new MembershipDto(
            r.GetInt32(0), r.GetInt32(1), r.GetString(2), r.GetDecimal(3),
            r.GetDateTime(4), r.GetDateTime(5), GetIntOrNull(r, 6), r.GetString(7)),
            new SqlParameter("@ClientId", clientId));

    public Task<int> IssueMembership(MembershipIssue body) =>
        Scalar("usp_Membership_Issue",
            new SqlParameter("@ClientId", body.ClientId),
            new SqlParameter("@PlanId", body.PlanId),
            new SqlParameter("@StartDate", body.StartDate.Date));

    public Task<List<ClassDto>> SearchClasses(string? query) =>
        Query("usp_Class_Search", ReadClass, new SqlParameter("@Query", (object?)query ?? DBNull.Value));

    public async Task<ClassDto?> GetClass(int id)
    {
        var rows = await Query("usp_Class_Get", ReadClass, new SqlParameter("@ClassId", id));
        return rows.FirstOrDefault();
    }

    public Task<int> CreateClass(ClassWrite body) =>
        Scalar("usp_Class_Create", ClassParameters(body));

    public Task UpdateClass(int id, ClassWrite body) =>
        Execute("usp_Class_Update", ClassParameters(body, id));

    public Task<List<EnrollmentDto>> Enrollments(int classId) =>
        Query("usp_Enrollment_List", r => new EnrollmentDto(
            r.GetInt32(0), r.GetInt32(1), r.GetInt32(2), r.GetString(3), r.GetString(4), r.GetDateTime(5)),
            new SqlParameter("@ClassId", classId));

    public Task<int> Enroll(EnrollRequest body) =>
        Scalar("usp_Enrollment_Create",
            new SqlParameter("@ClassId", body.ClassId),
            new SqlParameter("@ClientId", body.ClientId));

    public Task CancelEnrollment(int id) =>
        Execute("usp_Enrollment_Cancel", new SqlParameter("@EnrollmentId", id));

    private static ClassDto ReadClass(SqlDataReader r) => new(
        r.GetInt32(0), r.GetString(1), r.GetInt32(2), r.GetString(3),
        r.GetInt32(4), r.GetString(5), r.GetDateTime(6), r.GetInt32(7), r.GetInt32(8), r.GetInt32(9));

    private static SqlParameter[] ClassParameters(ClassWrite body, int? classId = null)
    {
        var list = new List<SqlParameter>
        {
            new("@Title", body.Title),
            new("@TrainerId", body.TrainerId),
            new("@HallId", body.HallId),
            new("@StartsAt", body.StartsAt),
            new("@DurationMinutes", body.DurationMinutes),
            new("@MaxParticipants", body.MaxParticipants)
        };
        if (classId is not null)
            list.Insert(0, new SqlParameter("@ClassId", classId.Value));
        return list.ToArray();
    }

    private async Task<List<T>> Query<T>(string procedure, Func<SqlDataReader, T> map, params SqlParameter[] parameters)
    {
        await using var connection = new SqlConnection(_connectionString);
        await using var command = Command(connection, procedure, parameters);
        await connection.OpenAsync();
        await using var reader = await command.ExecuteReaderAsync();
        var rows = new List<T>();
        while (await reader.ReadAsync())
            rows.Add(map(reader));
        return rows;
    }

    private async Task<int> Scalar(string procedure, params SqlParameter[] parameters)
    {
        await using var connection = new SqlConnection(_connectionString);
        await using var command = Command(connection, procedure, parameters);
        await connection.OpenAsync();
        var value = await command.ExecuteScalarAsync();
        return Convert.ToInt32(value);
    }

    private async Task Execute(string procedure, params SqlParameter[] parameters)
    {
        await using var connection = new SqlConnection(_connectionString);
        await using var command = Command(connection, procedure, parameters);
        await connection.OpenAsync();
        await command.ExecuteNonQueryAsync();
    }

    private static SqlCommand Command(SqlConnection connection, string procedure, params SqlParameter[] parameters)
    {
        var command = new SqlCommand(procedure, connection) { CommandType = System.Data.CommandType.StoredProcedure };
        command.Parameters.AddRange(parameters);
        return command;
    }

    private static string? GetStringOrNull(SqlDataReader reader, int index) =>
        reader.IsDBNull(index) ? null : reader.GetString(index);

    private static int? GetIntOrNull(SqlDataReader reader, int index) =>
        reader.IsDBNull(index) ? null : reader.GetInt32(index);
}
