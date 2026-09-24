package kr.dalbittarot.reading;

import jakarta.validation.Valid;
import kr.dalbittarot.reading.dto.ReadingCreateRequest;
import kr.dalbittarot.reading.dto.ReadingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/readings")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;

    @PostMapping
    public ResponseEntity<ReadingResponse> create(@Valid @RequestBody ReadingCreateRequest request) {
        ReadingResponse response = readingService.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/readings/{shareToken}")
                .buildAndExpand(response.shareToken())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{shareToken}")
    public ReadingResponse findByShareToken(@PathVariable String shareToken) {
        return readingService.findByShareToken(shareToken);
    }
}
