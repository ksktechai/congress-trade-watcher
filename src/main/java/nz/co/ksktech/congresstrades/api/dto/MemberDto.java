package nz.co.ksktech.congresstrades.api.dto;

import nz.co.ksktech.congresstrades.domain.Member;

/**
 * Read model for a {@link Member}.
 */
public record MemberDto(
        Long id,
        String fullName,
        String chamber,
        String party,
        String state
) {
    public static MemberDto from(Member m) {
        return new MemberDto(
                m.id,
                m.fullName,
                m.chamber != null ? m.chamber.name() : null,
                m.party,
                m.state);
    }
}
