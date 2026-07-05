package com.ahmedbahaj.parking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Partial-update payload for {@code PUT /api/users/{id}} and
 * {@code PUT /api/admin/users/{id}}.
 *
 * <p>Every field is optional — {@code UserService.updateUser} only writes
 * fields that are non-null, so omitting a field leaves it unchanged. This
 * DTO deliberately has no default values (unlike {@link com.ahmedbahaj.parking.model.User},
 * whose {@code role} and {@code credit} fields default to {@code "Customer"}
 * and {@code 0.00} for brand-new accounts). Binding the entity directly as
 * the request body meant Jackson left an omitted {@code "role"} or
 * {@code "credit"} JSON field at that entity default (non-null) rather than
 * null, so a partial update that never mentioned them would silently
 * overwrite them — e.g. the admin panel's "edit customer" form, which never
 * sends {@code role}, was silently resetting every edited user's role back
 * to Customer. With no defaults here, "absent from the JSON" and "null"
 * are the same thing, matching what the service layer already assumed.
 */
@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String username;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 20, message = "Mobile number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9+\\-() ]*$", message = "Invalid mobile number format")
    private String mobile;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;

    @Size(max = 20, message = "Car plate number must not exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\- ]*$", message = "Invalid car plate format")
    private String carPlateNo;

    @DecimalMin(value = "0.0", message = "Credit cannot be negative")
    private BigDecimal credit;

    /** Admin-only field; the controller nulls this out before calling the service for non-admin callers. */
    @Pattern(regexp = "^(Admin|Customer)$", message = "Role must be Admin or Customer")
    private String role;
}
