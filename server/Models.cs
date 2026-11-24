namespace FitStudio.Api;

public record ClientDto(int ClientId, string FullName, string Phone, string? Email, DateTime BirthDate, DateTime RegisteredAt);

public record ClientWrite(string FullName, string Phone, string? Email, DateTime BirthDate);

public record TrainerDto(int TrainerId, string FullName, string Specialization, string Phone, int ExperienceYears);

public record HallDto(int HallId, string Name, int Capacity, int FloorNumber);

public record PlanDto(int PlanId, string Name, int DurationDays, decimal Price, int? VisitLimit);

public record MembershipDto(
    int MembershipId,
    int ClientId,
    string PlanName,
    decimal Price,
    DateTime StartDate,
    DateTime EndDate,
    int? VisitsLeft,
    string Status);

public record MembershipIssue(int ClientId, int PlanId, DateTime StartDate);

public record ClassDto(
    int ClassId,
    string Title,
    int TrainerId,
    string TrainerName,
    int HallId,
    string HallName,
    DateTime StartsAt,
    int DurationMinutes,
    int MaxParticipants,
    int Taken);

public record ClassWrite(
    string Title,
    int TrainerId,
    int HallId,
    DateTime StartsAt,
    int DurationMinutes,
    int MaxParticipants);

public record EnrollmentDto(
    int EnrollmentId,
    int ClassId,
    int ClientId,
    string ClientName,
    string Status,
    DateTime EnrolledAt);

public record EnrollRequest(int ClassId, int ClientId);

public record IdResult(int Id);

public class ApiException : Exception
{
    public ApiException(string message) : base(message) { }
}
