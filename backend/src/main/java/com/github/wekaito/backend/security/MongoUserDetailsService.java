package com.github.wekaito.backend.security;

import com.github.wekaito.backend.IdService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class MongoUserDetailsService implements UserDetailsService {

    private final MongoUserRepository mongoUserRepository;

    private final IdService idService = new IdService();

    String exceptionMessage = " not found";

    public MongoUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return mongoUserRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("User " + username + exceptionMessage));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MongoUser mongoUser = mongoUserRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("User " + username + exceptionMessage));

        return new User(mongoUser.username(), mongoUser.password(), Collections.emptyList());
    }

    public String registerNewUser(RegistrationUser registrationUser){

        PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 8, 1 << 16, 4);
        String encodedPassword = encoder.encode(registrationUser.password());

        if (mongoUserRepository.findByUsername(registrationUser.username()).isPresent()) {
            return "Username already exists!";
        }

        MongoUser newUser = new MongoUser(idService.createId() ,registrationUser.username(), encodedPassword, "", "ava1", "Default");
        mongoUserRepository.save(newUser);

        return "Successfully registered!";
    }

    public String getUserIdByUsername(String username){
        MongoUser mongoUser = mongoUserRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("User " + username + exceptionMessage));
        return mongoUser.id();
    }

    public void setActiveDeck(String deckId) {
        MongoUser mongoUser = getCurrentUser();
        MongoUser updatedUser = new MongoUser(mongoUser.id(), mongoUser.username(), mongoUser.password(), deckId, mongoUser.avatarName(), mongoUser.sleeveName());
        mongoUserRepository.save(updatedUser);
    }

    public String getActiveDeck() {
        MongoUser mongoUser = getCurrentUser();
        return mongoUser.activeDeckId();
    }

    public String getActiveDeck(String username) {
        MongoUser mongoUser = mongoUserRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("User " + username + exceptionMessage));
        return mongoUser.activeDeckId();
    }

    public void setAvatar(String avatarName) {
        MongoUser mongoUser = getCurrentUser();
        MongoUser updatedUser = new MongoUser(mongoUser.id(), mongoUser.username(), mongoUser.password(), mongoUser.activeDeckId(), avatarName, mongoUser.sleeveName());
        mongoUserRepository.save(updatedUser);
    }

    public String getAvatar() {
        MongoUser mongoUser = getCurrentUser();
        return mongoUser.avatarName();
    }

    public String getAvatar(String username) {
        MongoUser mongoUser = mongoUserRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("User " + username + exceptionMessage));
        return mongoUser.avatarName();
    }

    public void setSleeve(String sleeveName) {
        MongoUser mongoUser = getCurrentUser();
        MongoUser updatedUser = new MongoUser(mongoUser.id(), mongoUser.username(), mongoUser.password(), mongoUser.activeDeckId(), mongoUser.avatarName(), sleeveName);
        mongoUserRepository.save(updatedUser);
    }

    public String getSleeve() {
        MongoUser mongoUser = getCurrentUser();
        return mongoUser.sleeveName();
    }

    public String getSleeve(String username) {
        MongoUser mongoUser = mongoUserRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("User " + username + exceptionMessage));
        return mongoUser.sleeveName();
    }
}
