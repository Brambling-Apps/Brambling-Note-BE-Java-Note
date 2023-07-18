package moe.echo.bramblingnote.note;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import moe.echo.bramblingnote.user.UserDto;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class NoteDto {
    @JsonView(View.ViewOnly.class)
    private UUID id;

    @JsonView(View.Editable.class)
    @NotBlank
    private String content;

    @JsonView(View.ViewOnly.class)
    @NotBlank
    private Date date;

    @JsonView(View.Editable.class)
    @NotBlank
    private boolean importance;

    @JsonView(View.Internal.class)
    private Date expireAt;

    @JsonView(View.ViewOnly.class)
    private UserDto user;
}
