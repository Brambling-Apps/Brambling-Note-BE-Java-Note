package moe.echo.bramblingnote.note;

import jakarta.servlet.http.HttpSession;
import moe.echo.bramblingnote.user.UserForReturn;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
public class Controller {
    private final ServiceImpl service;
    private final HttpSession session;
    private final KafkaTemplate<String, String> template;

    Controller(ServiceImpl service, HttpSession session, KafkaTemplate<String, String> template) {
        this.service = service;
        this.session = session;
        this.template = template;
    }

    private NoteForReturn toNoteForReturn(Note note, UserForReturn user) {
        NoteForReturn n = new NoteForReturn();
        n.setId(note.getId());
        n.setContent(note.getContent());
        n.setDate(note.getDate());
        n.setImportance(note.isImportance());
        n.setExpireAt(note.getExpireAt());
        n.setUser(user);
        return n;
    }

    private UserForReturn getUser(HttpSession session) throws ResponseStatusException {
        Object rawUser = session.getAttribute("user");
        if (!(rawUser instanceof UserForReturn user)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
        }

        return user;
    }

    private Note getNote(UUID id, UUID userId) throws ResponseStatusException {
        Note note = service.findById(id, userId);

        if (note == null) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "Note `" + id + "` not found"
            );
        }

        return note;
    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name("delete-note")
                .partitions(10)
                .replicas(1)
                .build();
    }

    @KafkaListener(id = "eraser", topics = "delete-note")
    public void listen(@Header(KafkaHeaders.RECEIVED_KEY) String id, String userId) {
        service.undoableDeleteById(UUID.fromString(id), UUID.fromString(userId));
    }

    @GetMapping("/health")
    public MessageJson health() {
        MessageJson message = new MessageJson();
        message.setMessage("ok");
        return message;
    }

    @PostMapping("/")
    public NoteForReturn create(@RequestBody NewNote note) {
        UserForReturn user = getUser(session);

        Note n = new Note();
        n.setContent(note.getContent());
        n.setImportance(note.isImportance());
        n.setDate(new Date());
        n.setExpireAt(null);
        n.setUserId(user.getId());
        return toNoteForReturn(service.save(n), user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageJson> delete(@PathVariable UUID id) {
        UserForReturn user = getUser(session);
        getNote(id, user.getId());

        template.send("delete-note", id.toString(), user.getId().toString());

        MessageJson message = new MessageJson();
        message.setMessage("ok");
        return ResponseEntity.status(202).body(message);
    }

    @PatchMapping("/deleted/{id}")
    public NoteForReturn undoDelete(@PathVariable UUID id) {
        UserForReturn user = getUser(session);
        Note note = service.undoDeleteById(id, user.getId());

        if (note == null) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "Note `" + id + "` is already gone."
            );
        }

        return toNoteForReturn(note, user);
    }

    @GetMapping("/")
    public List<NoteForReturn> getAll() {
        UserForReturn user = getUser(session);

        return service.findAllByUserId(user.getId()).stream()
                .map(note -> toNoteForReturn(note, user))
                .toList();
    }

    @GetMapping("/{id}")
    public NoteForReturn get(@PathVariable UUID id) {
        UserForReturn user = getUser(session);
        Note note = getNote(id, user.getId());

        return toNoteForReturn(note, user);
    }

    @PutMapping("/{id}")
    public NoteForReturn update(@PathVariable UUID id, @RequestBody NewNote newNote) {
        UserForReturn user = getUser(session);

        Note note = getNote(id, user.getId());
        note.setContent(newNote.getContent());
        note.setImportance(newNote.isImportance());

        return toNoteForReturn(service.save(note), user);
    }
}
