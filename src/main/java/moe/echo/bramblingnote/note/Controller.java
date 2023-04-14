package moe.echo.bramblingnote.note;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        return ResponseEntity.ok().build();
    }
}
