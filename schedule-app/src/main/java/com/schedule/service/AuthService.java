package com.schedule.service;

import com.schedule.entity.User;
import com.schedule.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @PostConstruct
    public void initUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User("admin", encoder.encode("admin123"), "管理员", User.Role.ADMIN));
        }
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十",
                          "郑一", "冯二", "陈三", "褚四", "卫五", "蒋六", "沈七"};
        for (int i = 0; i < names.length; i++) {
            String username = "emp" + (i + 1);
            if (!userRepository.existsByUsername(username)) {
                userRepository.save(new User(username, encoder.encode("123456"), names[i], User.Role.EMPLOYEE));
            }
        }
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && encoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
