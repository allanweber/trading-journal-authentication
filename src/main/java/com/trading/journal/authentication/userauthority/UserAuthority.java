package com.trading.journal.authentication.userauthority;

import com.trading.journal.authentication.authority.Authority;
import com.trading.journal.authentication.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Entity
@Table(name = "UserAuthorities")
public class UserAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @OneToOne
    @JoinColumn(name = "authorityId")
    private Authority authority;

    public UserAuthority(User user, Authority authority) {
        this.user = user;
        this.authority = authority;
    }
}
