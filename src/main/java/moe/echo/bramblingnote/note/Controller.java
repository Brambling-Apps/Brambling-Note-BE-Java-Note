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
    private final Repository repository;
    private final HttpSession session;
    private final KafkaTemplate<String, String> template;

    Controller(Repository repository, HttpSession session, KafkaTemplate<String, String> template) {
        this.repository = repository;
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

    private Note getNote(UUID id) throws ResponseStatusException {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatusCode.valueOf(404),
                "Note `" + id + "` not found"
        ));
    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name("delete-note")
                .partitions(10)
                .replicas(1)
                .build();
    }

    @KafkaListener(id = "eraser", topics = "delete-note")
    public void listen(@Header(KafkaHeaders.RECEIVED_KEY) String id) {
        repository.deleteById(UUID.fromString(id));
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
        return toNoteForReturn(repository.save(n), user);
    }

    // TODO: expireAt
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageJson> delete(@PathVariable UUID id) {
        UserForReturn user = getUser(session);
        getNote(id);

        template.send("delete-note", id.toString());

        MessageJson message = new MessageJson();
        message.setMessage("ok");
        return ResponseEntity.status(202).body(message);
    }

    @GetMapping("/")
    public List<NoteForReturn> get() {
        UserForReturn user = getUser(session);

        return repository.findAllByUserId(user.getId()).stream()
                .map(note -> toNoteForReturn(note, user))
                .toList();
    }

    @GetMapping("/{id}")
    public NoteForReturn getById(@PathVariable UUID id) {
        UserForReturn user = getUser(session);
        Note note = getNote(id);

        return toNoteForReturn(note, user);
    }

    @PutMapping("/{id}")
    public NoteForReturn update(@PathVariable UUID id, @RequestBody NewNote newNote) {
        UserForReturn user = getUser(session);

        Note note = getNote(id);
        String content = newNote.getContent();
        if (content != null) {
            note.setContent(content);
        }
        note.setImportance(newNote.isImportance());

        return toNoteForReturn(repository.save(note), user);
    }
}
