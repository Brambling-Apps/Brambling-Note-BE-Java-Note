package moe.echo.bramblingnote.note;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NoteMapper {
    @Mapping(target = "user.id", source = "userId")
    NoteDto toNoteDto(Note note);

    @Mapping(target = "userId", source = "user.id")
    Note toNote(NoteDto noteDto);
}
