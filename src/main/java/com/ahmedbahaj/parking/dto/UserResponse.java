package com.ahmedbahaj.parking.dto;

import com.ahmedbahaj.parking.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Outbound representation of a {@link User}.
 *
 * Why a dedicated DTO: returning the JPA entity directly was leaking the
 * full row to the network — including {@code password} hashes (covered by
 * @JsonIgnore today, but one accidental annotation removal away from a
 * disaster) and other internal columns. A frozen DTO surface gives us:
 *  - A guarantee that no field added to {@link User} ever leaks by default.
 *  - A smaller, cacheable response payload.
 *  - A clear contract for B2B customers reviewing the API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Integer id;
    private String username;
    private String email;
    private String mobile;
    private String address;
    private String carPlateNo;
    private BigDecimal credit;
    private String role;

    public static UserResponse from(User u) {
        if (u == null) return null;
        return new UserResponse(
            u.getId(),
            u.getUsername(),
            u.getEmail(),
            u.getMobile(),
            u.getAddress(),
            u.getCarPlateNo(),
            u.getCredit(),
            u.getRole()
        );
    }
}
