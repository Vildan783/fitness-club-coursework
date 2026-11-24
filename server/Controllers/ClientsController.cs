using Microsoft.AspNetCore.Mvc;

namespace FitStudio.Api.Controllers;

[ApiController]
[Route("api/clients")]
public class ClientsController(FitStudioRepository db) : ControllerBase
{
    [HttpGet]
    public Task<List<ClientDto>> Search([FromQuery] string? q) => db.SearchClients(q);

    [HttpGet("{id:int}")]
    public async Task<ActionResult<ClientDto>> Get(int id)
    {
        var client = await db.GetClient(id);
        return client is null ? NotFound(new { message = "Клиент не найден." }) : client;
    }

    [HttpPost]
    public async Task<ActionResult<ClientDto>> Create(ClientWrite body)
    {
        var id = await db.CreateClient(body);
        var client = await db.GetClient(id);
        return CreatedAtAction(nameof(Get), new { id }, client);
    }

    [HttpPut("{id:int}")]
    public async Task<ActionResult<ClientDto>> Update(int id, ClientWrite body)
    {
        await db.UpdateClient(id, body);
        var client = await db.GetClient(id);
        return client is null ? NotFound(new { message = "Клиент не найден." }) : client;
    }

    [HttpGet("{id:int}/memberships")]
    public Task<List<MembershipDto>> Memberships(int id) => db.Memberships(id);
}
