package fr.avote.api;

import fr.avote.domain.Chamber;
import fr.avote.service.ParliamentarianQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ParliamentarianController {

    private final ParliamentarianQueryService service;

    public ParliamentarianController(ParliamentarianQueryService service) {
        this.service = service;
    }

    @GetMapping("/parliamentarians")
    public PageResponse<ParliamentarianSummary> list(
            @RequestParam(required = false) Chamber chamber,
            @RequestParam(required = false) Long group,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        String query = q == null || q.isBlank() ? null : q.strip();
        return service.search(chamber, group, query, page, size);
    }

    @GetMapping("/parliamentarians/{id}")
    public ParliamentarianDetail get(@PathVariable long id) {
        return service.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/groups")
    public List<GroupDto> groups(@RequestParam(required = false) Chamber chamber) {
        return service.groups(chamber);
    }

    @GetMapping("/chambers")
    public List<ChamberStats> chambers() {
        return service.chambers();
    }
}
