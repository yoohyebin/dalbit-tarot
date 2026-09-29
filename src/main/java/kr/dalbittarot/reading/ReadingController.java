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
    private final InterpretationService interpretationService;

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

    /**
     * LLM 해석 요청.
     *
     * 작업순서 문서에는 {id} 로 적혀 있지만 shareToken 을 쓴다. 로그인이 없어서 순번 id 를
     * 노출하면 남의 결과를 훑을 수 있다.
     */
    @PostMapping("/{shareToken}/interpretation")
    public ReadingResponse interpret(@PathVariable String shareToken) {
        return interpretationService.interpret(shareToken);
    }
}
