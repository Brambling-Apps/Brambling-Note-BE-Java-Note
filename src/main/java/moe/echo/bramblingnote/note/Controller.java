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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
public class Controller {
    private final Repository repository;
    private final HttpSession session;
    private final KafkaTemplate<String, UUID> template;

    Controller(Repository repository, HttpSession session, KafkaTemplate<String, UUID> template) {
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

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name("delete-note")
                .partitions(10)
                .replicas(1)
                .build();
    }

    @KafkaListener(id = "eraser", topics = "delete-note")
    public void listen(UUID id) {
        repository.deleteById(id);
    }

    @GetMapping("/health")
    public MessageJson health() {
        MessageJson message = new MessageJson();
        message.setMessage("ok");
        return message;
    }

    @PostMapping("/")
    public NoteForReturn create(@RequestBody NewNote note) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForReturn user) {
            Note n = new Note();
            n.setContent(note.getContent());
            n.setImportance(note.isImportance());
            n.setDate(new Date());
            n.setExpireAt(null);
            n.setUserId(user.getId());
            return toNoteForReturn(repository.save(n), user);
        }
        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }

    // TODO: expireAt
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageJson> delete(@PathVariable UUID id) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForReturn user) {
            return repository.findById(id).map(note -> {
                if (user.getId().equals(note.getUserId())) {
                    template.send("delete-note", id);

                    MessageJson message = new MessageJson();
                    message.setMessage("ok");
                    return ResponseEntity.status(202).body(message);
                }

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(401),
                        "You have no permission to access this note"
                );
            }).orElseThrow(() -> new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "Note `" + id + "` not found")
            );
        }
        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }

    @GetMapping("/")
    public List<NoteForReturn> get() {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForReturn user) {
            return repository.findAllByUserId(user.getId()).stream()
                    .map(note -> toNoteForReturn(note, user))
                    .toList();
        }
        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }

    @GetMapping("/{id}")
    public NoteForReturn getById(@PathVariable UUID id) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForReturn user) {
            return repository.findById(id).map(n -> {
                if (n.getUserId().equals(user.getId())) {
                    return toNoteForReturn(n, user);
                }
                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(401),
                        "You have no permission to note `" + id + "`"
                );
            }).orElseThrow(() -> new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "note `" + id + "` not found"
            ));
        }

        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }

    @PutMapping("/{id}")
    public NoteForReturn update(@PathVariable UUID id, @RequestBody NewNote note) {
        Object rawUser = session.getAttribute("user");
        if (rawUser instanceof UserForReturn user) {
            return repository.findById(id).map(n -> {
                if (n.getUserId().equals(user.getId())) {
                    String content = note.getContent();
                    if (content != null) {
                        n.setContent(note.getContent());
                    }

                    n.setImportance(note.isImportance());
                    Note savedNote = repository.save(n);
                    return toNoteForReturn(savedNote, user);
                }
                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(401),
                        "You have no permission to note `" + id + "`"
                );
            }).orElseThrow(() -> new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "note `" + id + "` not found"
            ));
        }

        throw new ResponseStatusException(HttpStatusCode.valueOf(401), "You are not login yet");
    }
}
