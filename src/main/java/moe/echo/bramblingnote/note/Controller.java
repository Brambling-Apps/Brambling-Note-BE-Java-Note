package moe.echo.bramblingnote.note;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import moe.echo.bramblingnote.user.UserDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
    private final NoteMapper noteMapper;

    public Controller(
            ServiceImpl service,
            HttpSession session,
            KafkaTemplate<String, String> template,
            NoteMapper noteMapper
    ) {
        this.service = service;
        this.session = session;
        this.template = template;
        this.noteMapper = noteMapper;
    }

    @JsonView(moe.echo.bramblingnote.user.View.ViewOnly.class)
    private UserDto getUserFromSession() {
        Object rawUser = session.getAttribute("user");

        if (!(rawUser instanceof String userJson)) {
            session.invalidate();
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(401), "You are not login yet"
            );
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(userJson, UserDto.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(500), e.getMessage()
            );
        }
    }

    private Note getNote(UUID id, UUID userId) throws ResponseStatusException {
        Note note = service.findById(id, userId);

        if (note == null) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "Note `" + id + "` not found"
            );
        }

        if (note.getExpireAt() != null) {
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void health() {}

    @PostMapping("/")
    @JsonView(View.ViewOnly.class)
    public NoteDto create(@RequestBody @JsonView(View.Editable.class) NoteDto newNote) {
        UserDto user = getUserFromSession();

        NoteDto note = new NoteDto();
        note.setContent(newNote.getContent());
        note.setImportance(newNote.isImportance());
        note.setDate(new Date());
        note.setExpireAt(null);
        note.setUser(user);
        return noteMapper.toNoteDto(service.save(noteMapper.toNote(note)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        UserDto user = getUserFromSession();
        getNote(id, user.getId());

        template.send("delete-note", id.toString(), user.getId().toString());
    }

    @PatchMapping("/deleted/{id}")
    @JsonView(View.ViewOnly.class)
    public NoteDto undoDelete(@PathVariable UUID id) {
        UserDto user = getUserFromSession();
        Note note = service.undoDeleteById(id, user.getId());

        if (note == null) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(404),
                    "Note `" + id + "` is already gone."
            );
        }

        NoteDto noteDto = noteMapper.toNoteDto(note);
        noteDto.setUser(user);

        return noteDto;
    }

    @GetMapping("/")
    @JsonView(View.ViewOnly.class)
    public List<NoteDto> getAll() {
        UserDto user = getUserFromSession();

        return service.findAllByUserId(user.getId()).stream()
                .filter(note -> note.getExpireAt() == null)
                .map(note -> {
                    NoteDto noteDto = noteMapper.toNoteDto(note);
                    noteDto.setUser(user);
                    return noteDto;
                })
                .toList();
    }

    @GetMapping("/{id}")
    @JsonView(View.ViewOnly.class)
    public NoteDto get(@PathVariable UUID id) {
        UserDto user = getUserFromSession();
        Note note = getNote(id, user.getId());

        NoteDto noteDto = noteMapper.toNoteDto(note);
        noteDto.setUser(user);
        return noteDto;
    }

    @PutMapping("/{id}")
    @JsonView(View.ViewOnly.class)
    public NoteDto update(@PathVariable UUID id, @RequestBody @JsonView(View.Editable.class) NoteDto newNote) {
        UserDto user = getUserFromSession();

        Note note = getNote(id, user.getId());
        note.setContent(newNote.getContent());
        note.setImportance(newNote.isImportance());

        NoteDto noteDto = noteMapper.toNoteDto(service.save(note));
        noteDto.setUser(user);
        return noteDto;
    }
}
