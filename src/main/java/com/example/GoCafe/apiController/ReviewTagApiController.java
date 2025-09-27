package com.example.GoCafe.apiController;

import com.example.GoCafe.entity.ReviewTag;
import com.example.GoCafe.service.ReviewTagService;
import com.example.GoCafe.support.EntityIdUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/review-tags")
@RequiredArgsConstructor
public class ReviewTagApiController {

    private final ReviewTagService service;

    @GetMapping
    public List<ReviewTag> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ReviewTag getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ReviewTag> create(@RequestBody @Valid ReviewTag body, UriComponentsBuilder uriBuilder) {
        ReviewTag saved = service.create(body);
        Object id = EntityIdUtil.getId(saved);
        URI location = uriBuilder.path("/api/review-tags/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public ReviewTag update(@PathVariable Long id, @RequestBody @Valid ReviewTag body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
