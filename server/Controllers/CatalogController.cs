using Microsoft.AspNetCore.Mvc;

namespace FitStudio.Api.Controllers;

[ApiController]
[Route("api")]
public class CatalogController(FitStudioRepository db) : ControllerBase
{
    [HttpGet("trainers")]
    public Task<List<TrainerDto>> Trainers() => db.Trainers();

    [HttpGet("halls")]
    public Task<List<HallDto>> Halls() => db.Halls();

    [HttpGet("plans")]
    public Task<List<PlanDto>> Plans() => db.Plans();

    [HttpPost("memberships")]
    public async Task<ActionResult<IdResult>> Issue(MembershipIssue body)
    {
        var id = await db.IssueMembership(body);
        return Ok(new IdResult(id));
    }

    [HttpPost("enrollments")]
    public async Task<ActionResult<IdResult>> Enroll(EnrollRequest body)
    {
        var id = await db.Enroll(body);
        return Ok(new IdResult(id));
    }

    [HttpPost("enrollments/{id:int}/cancel")]
    public async Task<IActionResult> Cancel(int id)
    {
        await db.CancelEnrollment(id);
        return NoContent();
    }
}
