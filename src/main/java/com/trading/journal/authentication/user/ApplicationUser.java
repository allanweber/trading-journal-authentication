package com.trading.journal.authentication.user;

import com.trading.journal.authentication.userauthority.UserAuthority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Entity(name = "Users")
public class ApplicationUser {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String userName;

    private String password;

    private String firstName;

    private String lastName;

    private String email;

    private Boolean enabled;

    private Boolean verified;

    @Transient
    private List<UserAuthority> authorities;

    private LocalDateTime createdAt;

    public void loadAuthorities(List<UserAuthority> authorities) {
        this.authorities = authorities;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void verify() {
        this.verified = true;
    }

    public void changePassword(@NotBlank String newPassword) {
        this.password = newPassword;
    }
}