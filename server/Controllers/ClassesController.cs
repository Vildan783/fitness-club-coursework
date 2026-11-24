using Microsoft.AspNetCore.Mvc;

namespace FitStudio.Api.Controllers;

[ApiController]
[Route("api/classes")]
public class ClassesController(FitStudioRepository db) : ControllerBase
{
    [HttpGet]
    public Task<List<ClassDto>> Search([FromQuery] string? q) => db.SearchClasses(q);

    [HttpGet("{id:int}")]
    public async Task<ActionResult<ClassDto>> Get(int id)
    {
        var item = await db.GetClass(id);
        return item is null ? NotFound(new { message = "Занятие не найдено." }) : item;
    }

    [HttpPost]
    public async Task<ActionResult<ClassDto>> Create(ClassWrite body)
    {
        var id = await db.CreateClass(body);
        var item = await db.GetClass(id);
        return CreatedAtAction(nameof(Get), new { id }, item);
    }

    [HttpPut("{id:int}")]
    public async Task<ActionResult<ClassDto>> Update(int id, ClassWrite body)
    {
        await db.UpdateClass(id, body);
        var item = await db.GetClass(id);
        return item is null ? NotFound(new { message = "Занятие не найдено." }) : item;
    }

    [HttpGet("{id:int}/enrollments")]
    public Task<List<EnrollmentDto>> Enrollments(int id) => db.Enrollments(id);
}
